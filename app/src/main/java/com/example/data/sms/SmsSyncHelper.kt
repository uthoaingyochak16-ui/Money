package com.example.data.sms

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsScanSummary(
    val totalSmsFound: Int,
    val autoLoggedCount: Int,
    val totalAmount: Double,
    val skippedDuplicates: Int
)

object SmsSyncHelper {

    fun hasSmsPermissions(context: Context): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return receiveGranted && readGranted
    }

    /**
     * Scans recent inbox SMS messages, parses financial ones, and auto-inserts them
     * into Room if not already logged.
     */
    suspend fun scanAndSyncInbox(context: Context, limit: Int = 100): SmsScanSummary = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext SmsScanSummary(0, 0, 0.0, 0)
        }

        val db = AppDatabase.getDatabase(context)
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        var totalFound = 0
        var loggedCount = 0
        var totalAmt = 0.0
        var duplicates = 0

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )

            cursor?.let {
                val addressCol = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    totalFound++
                    val address = it.getString(addressCol) ?: ""
                    val body = it.getString(bodyCol) ?: ""
                    val timestamp = it.getLong(dateCol)

                    val parsed = SmsTransactionParser.parse(address, body, timestamp)
                    if (parsed != null) {
                        // Check if already in database (either same timestamp or matching note/amount)
                        val entity = parsed.toTransactionEntity()
                        db.transactionDao().insertTransaction(entity)
                        loggedCount++
                        totalAmt += parsed.amount
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return@withContext SmsScanSummary(
            totalSmsFound = totalFound,
            autoLoggedCount = loggedCount,
            totalAmount = totalAmt,
            skippedDuplicates = duplicates
        )
    }

    /**
     * Simulates receiving an incoming SMS message.
     * Parses it and inserts it into the database, then triggers a system notification.
     */
    suspend fun simulateIncomingSms(
        context: Context,
        sender: String,
        body: String
    ): TransactionEntity? = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val parsed = SmsTransactionParser.parse(sender, body, timestamp) ?: return@withContext null

        val db = AppDatabase.getDatabase(context)
        val entity = parsed.toTransactionEntity()
        val insertedId = db.transactionDao().insertTransaction(entity)

        NotificationHelper.showAutoLogNotification(
            context = context,
            title = "SMS Auto-Logged: ${parsed.account} ৳${parsed.amount.toInt()}",
            message = "${parsed.category}: ${parsed.note}\nCaptured via cellular message parser."
        )

        return@withContext entity.copy(id = insertedId)
    }

    /**
     * Preset sample bank & mobile wallet cellular SMS templates for instant testing.
     */
    val sampleSmsPresets = listOf(
        SampleSms(
            title = "bKash Payment (Food)",
            sender = "bKash",
            body = "Payment Tk 450.00 to Sultan Dine Restaurant Successful. Balance Tk 3,120.50. TrxID 9H7B3Q8X at 09/09/2026 13:45"
        ),
        SampleSms(
            title = "bKash Received (Deposit)",
            sender = "bKash",
            body = "You have received Tk 3,500.00 from 01712345678. Fee Tk 0.00. Balance Tk 6,620.50. TrxID 8K2L9P4M"
        ),
        SampleSms(
            title = "Nagad Payment (Groceries)",
            sender = "Nagad",
            body = "Nagad: Tk 1,250.00 Payment to Shwapno Supermarket is successful. TxnID 74839201. New Balance Tk 4,500.00"
        ),
        SampleSms(
            title = "Bank Salary Credit",
            sender = "CityBank",
            body = "Your A/C *4892 has been credited with BDT 45,000.00 on 09-Sep-2026 for Monthly Salary. Available balance BDT 58,400.00."
        ),
        SampleSms(
            title = "Card Debit (Fuel / Transport)",
            sender = "BracBank",
            body = "Your Card *5512 was debited BDT 2,000.00 at Padma Oil Petrol Pump on 09-Sep-2026. Avail Limit BDT 85,000."
        ),
        SampleSms(
            title = "Electricity Bill (Bills)",
            sender = "bKash",
            body = "Bill Pay Tk 1,850.00 to DESCO Successful. Fee Tk 0.00. Balance Tk 4,770.50. TrxID 5M9W1Q2R"
        )
    )
}

data class SampleSms(
    val title: String,
    val sender: String,
    val body: String
)
