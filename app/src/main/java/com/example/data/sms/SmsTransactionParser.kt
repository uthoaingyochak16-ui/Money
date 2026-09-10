package com.example.data.sms

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsResult(
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val account: String,
    val note: String,
    val dateString: String,
    val timestamp: Long,
    val rawBody: String,
    val sender: String,
    val confidence: Float = 1.0f
) {
    fun toTransactionEntity(): TransactionEntity {
        return TransactionEntity(
            type = type,
            amount = amount,
            category = category,
            account = account,
            timestamp = timestamp,
            note = note,
            dateString = dateString,
            isAutoLoggedFromSms = true,
            rawSmsBody = rawBody
        )
    }
}

object SmsTransactionParser {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Parses an incoming cellular SMS message.
     * Returns ParsedSmsResult if it's a financial transaction, or null if unrelated.
     */
    fun parse(sender: String, body: String, timestamp: Long = System.currentTimeMillis()): ParsedSmsResult? {
        val cleanBody = body.trim()
        val lowerBody = cleanBody.lowercase()
        val lowerSender = sender.lowercase()

        val dateStr = dateFormat.format(Date(timestamp))

        // 1. bKash SMS Detection
        if (lowerSender.contains("bkash") || lowerBody.contains("bkash") || lowerBody.contains("trxid")) {
            val bkashResult = parseBkash(sender, cleanBody, timestamp, dateStr)
            if (bkashResult != null) return bkashResult
        }

        // 2. Nagad SMS Detection
        if (lowerSender.contains("nagad") || lowerBody.contains("nagad")) {
            val nagadResult = parseNagad(sender, cleanBody, timestamp, dateStr)
            if (nagadResult != null) return nagadResult
        }

        // 3. Bank SMS (City Bank, Brac, EBL, Dutch-Bangla, SCB, etc.)
        val bankResult = parseBankSms(sender, cleanBody, timestamp, dateStr)
        if (bankResult != null) return bankResult

        // 4. Generic Financial Cellular SMS Parser
        return parseGenericFinancialSms(sender, cleanBody, timestamp, dateStr)
    }

    private fun parseBkash(sender: String, body: String, timestamp: Long, dateStr: String): ParsedSmsResult? {
        val lower = body.lowercase()

        // Amount extraction
        val amount = extractAmount(body) ?: return null

        return when {
            // Received money / Cash In
            lower.contains("received") || lower.contains("cash in") -> {
                val fromWho = extractCounterparty(body, listOf("from", "by")) ?: "bKash Transfer"
                ParsedSmsResult(
                    type = TransactionType.INCOME,
                    amount = amount,
                    category = "Deposit",
                    account = "bKash",
                    note = "bKash Received: $fromWho",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            // Payment to merchant
            lower.contains("payment") -> {
                val merchant = extractCounterparty(body, listOf("to", "at")) ?: "Merchant"
                ParsedSmsResult(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = guessCategory(merchant),
                    account = "bKash",
                    note = "bKash Payment: $merchant",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            // Cash Out (ATM / Agent)
            lower.contains("cash out") -> {
                ParsedSmsResult(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = "Other",
                    account = "bKash",
                    note = "bKash Cash Out",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            // Send Money
            lower.contains("send money") || lower.contains("sent") -> {
                val toWho = extractCounterparty(body, listOf("to")) ?: "Recipient"
                ParsedSmsResult(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = "Transfer",
                    account = "bKash",
                    note = "bKash Send Money to $toWho",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            // Mobile Recharge
            lower.contains("recharge") -> {
                ParsedSmsResult(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = "Bills",
                    account = "bKash",
                    note = "bKash Mobile Recharge",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            else -> null
        }
    }

    private fun parseNagad(sender: String, body: String, timestamp: Long, dateStr: String): ParsedSmsResult? {
        val lower = body.lowercase()
        val amount = extractAmount(body) ?: return null

        return when {
            lower.contains("received") || lower.contains("cash in") -> {
                val fromWho = extractCounterparty(body, listOf("from")) ?: "Nagad User"
                ParsedSmsResult(
                    type = TransactionType.INCOME,
                    amount = amount,
                    category = "Deposit",
                    account = "Nagad",
                    note = "Nagad Received from $fromWho",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            lower.contains("payment") -> {
                val merchant = extractCounterparty(body, listOf("to", "at")) ?: "Merchant"
                ParsedSmsResult(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = guessCategory(merchant),
                    account = "Nagad",
                    note = "Nagad Payment: $merchant",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            lower.contains("cash out") -> {
                ParsedSmsResult(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = "Other",
                    account = "Nagad",
                    note = "Nagad Cash Out",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            lower.contains("send money") -> {
                val toWho = extractCounterparty(body, listOf("to")) ?: "Recipient"
                ParsedSmsResult(
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    category = "Transfer",
                    account = "Nagad",
                    note = "Nagad Send Money to $toWho",
                    dateString = dateStr,
                    timestamp = timestamp,
                    rawBody = body,
                    sender = sender
                )
            }
            else -> null
        }
    }

    private fun parseBankSms(sender: String, body: String, timestamp: Long, dateStr: String): ParsedSmsResult? {
        val lower = body.lowercase()

        // Check for debit / credit patterns
        val isDebit = lower.contains("debited") || lower.contains("spent") || lower.contains("withdrawn") ||
            lower.contains("purchase") || lower.contains("charged")
        val isCredit = lower.contains("credited") || lower.contains("deposited") || lower.contains("salary")

        if (!isDebit && !isCredit) return null

        val amount = extractAmount(body) ?: return null
        val merchant = extractCounterparty(body, listOf("at", "to", "info", "pos")) ?: sender.ifBlank { "Bank Transaction" }

        val type = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE
        val category = if (isCredit) {
            if (lower.contains("salary")) "Salary" else "Deposit"
        } else {
            guessCategory(merchant)
        }

        return ParsedSmsResult(
            type = type,
            amount = amount,
            category = category,
            account = "Bank",
            note = if (isCredit) "Bank Deposit: $merchant" else "Bank Debit at $merchant",
            dateString = dateStr,
            timestamp = timestamp,
            rawBody = body,
            sender = sender
        )
    }

    private fun parseGenericFinancialSms(sender: String, body: String, timestamp: Long, dateStr: String): ParsedSmsResult? {
        val lower = body.lowercase()

        val isExpense = lower.contains("spent") || lower.contains("paid") || lower.contains("debited") ||
            lower.contains("withdrawn") || lower.contains("payment of")
        val isIncome = lower.contains("received") || lower.contains("credited") || lower.contains("deposited") ||
            lower.contains("cashback")

        if (!isExpense && !isIncome) return null

        val amount = extractAmount(body) ?: return null
        val place = extractCounterparty(body, listOf("at", "to", "from", "for")) ?: sender

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        val category = if (isIncome) "Deposit" else guessCategory(place)

        return ParsedSmsResult(
            type = type,
            amount = amount,
            category = category,
            account = if (lower.contains("cash")) "Cash" else "Bank",
            note = "$sender: $place",
            dateString = dateStr,
            timestamp = timestamp,
            rawBody = body,
            sender = sender
        )
    }

    /**
     * Extracts monetary amount from text like:
     * Tk 1,500.00, Tk. 250, BDT 5,000, USD 45.50, $20, 500.00 Tk
     */
    fun extractAmount(text: String): Double? {
        val patterns = listOf(
            // Tk / BDT / Rs / USD / $ before amount
            Pattern.compile("(?:Tk\\.?|BDT|Rs\\.?|USD|EUR|\\$)\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            // Amount before Tk / BDT
            Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:Tk\\.?|BDT)", Pattern.CASE_INSENSITIVE),
            // "debited by 500.00" / "credited with 1000"
            Pattern.compile("(?:debited|credited|paid|amount|spent|fee)\\s+(?:by|with|of)?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawNum = matcher.group(1)?.replace(",", "") ?: continue
                val parsed = rawNum.toDoubleOrNull()
                if (parsed != null && parsed > 0.0 && parsed < 10_000_000.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractCounterparty(text: String, keywords: List<String>): String? {
        for (kw in keywords) {
            val regex = Pattern.compile("\\b$kw\\s+([A-Za-z0-9&\\.\\-_' ]{2,30})", Pattern.CASE_INSENSITIVE)
            val matcher = regex.matcher(text)
            if (matcher.find()) {
                var candidate = matcher.group(1)?.trim() ?: continue
                // Cut off at common boundary markers
                val stopWords = listOf("on", "at", "fee", "balance", "avail", "bal", "trxid", "txnid", "ref", "dated")
                for (stop in stopWords) {
                    val idx = candidate.lowercase().indexOf(" $stop")
                    if (idx != -1) {
                        candidate = candidate.substring(0, idx).trim()
                    }
                }
                if (candidate.isNotBlank() && candidate.length > 2) {
                    return candidate
                }
            }
        }
        return null
    }

    fun guessCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("food") || lower.contains("restaurant") || lower.contains("dine") ||
                lower.contains("cafe") || lower.contains("burger") || lower.contains("pizza") ||
                lower.contains("kitchen") || lower.contains("coffee") || lower.contains("swiggy") ||
                lower.contains("zomato") || lower.contains("kfc") -> "Food"

            lower.contains("grocery") || lower.contains("supermarket") || lower.contains("shwapno") ||
                lower.contains("meenabazar") || lower.contains("unimart") || lower.contains("mart") ||
                lower.contains("bazaar") -> "Groceries"

            lower.contains("uber") || lower.contains("pathao") || lower.contains("fuel") ||
                lower.contains("petrol") || lower.contains("cng") || lower.contains("bus") ||
                lower.contains("train") || lower.contains("metro") || lower.contains("transport") -> "Transport"

            lower.contains("bill") || lower.contains("desco") || lower.contains("wasa") ||
                lower.contains("titas") || lower.contains("electricity") || lower.contains("wifi") ||
                lower.contains("internet") || lower.contains("recharge") -> "Bills"

            lower.contains("shop") || lower.contains("daraz") || lower.contains("store") ||
                lower.contains("cloth") || lower.contains("brand") || lower.contains("amazon") ||
                lower.contains("aarong") || lower.contains("fashion") -> "Shopping"

            lower.contains("hospital") || lower.contains("pharma") || lower.contains("med") ||
                lower.contains("doctor") || lower.contains("diagnostic") -> "Health"

            lower.contains("cinema") || lower.contains("movie") || lower.contains("cineplex") ||
                lower.contains("game") || lower.contains("netflix") || lower.contains("spotify") -> "Entertainment"

            lower.contains("rent") || lower.contains("landlord") -> "Rent"

            lower.contains("salary") || lower.contains("bonus") || lower.contains("payroll") -> "Salary"

            lower.contains("send money") || lower.contains("transfer") -> "Transfer"

            else -> "Other"
        }
    }
}
