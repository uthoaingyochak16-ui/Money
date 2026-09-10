package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiParsedLog(
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val account: String,
    val note: String,
    val dateString: String? = null,
    val explanation: String = ""
)

object GeminiAiService {

    private const val TAG = "GeminiAiService"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")
    }

    /**
     * Parse freeform text, receipt OCR, or SMS into structured financial log parameters
     */
    suspend fun parseLogFromText(input: String): AiParsedLog? = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext fallbackLocalParse(input)
        }

        val prompt = """
            You are an intelligent financial assistant for a money management app.
            Analyze the following text (it could be a transaction description, SMS, note, or receipt text):
            "$input"
            
            Extract the financial transaction details and return ONLY a valid JSON object with:
            {
              "type": "EXPENSE" | "INCOME" | "TRANSFER",
              "amount": number (positive decimal or integer),
              "category": one of ["Food", "Groceries", "Transport", "Bills", "Shopping", "Entertainment", "Health", "Education", "Rent", "Salary", "Business", "Deposit", "Transfer", "Other"],
              "account": one of ["Cash", "Bank", "bKash", "Nagad", "Rocket", "Card"],
              "note": concise description of what was purchased or received,
              "explanation": brief 1-line explanation of how you parsed it
            }
            Do NOT include markdown formatting or backticks. Return raw JSON only.
        """.trimIndent()

        try {
            val responseText = callGeminiRestApi(prompt) ?: return@withContext fallbackLocalParse(input)
            val cleanJson = extractJson(responseText)
            val json = JSONObject(cleanJson)

            val typeStr = json.optString("type", "EXPENSE").uppercase()
            val type = when (typeStr) {
                "INCOME" -> TransactionType.INCOME
                "TRANSFER" -> TransactionType.TRANSFER
                else -> TransactionType.EXPENSE
            }
            val amount = json.optDouble("amount", 0.0)
            val category = json.optString("category", "Other")
            val account = json.optString("account", "Cash")
            val note = json.optString("note", input.take(40))
            val explanation = json.optString("explanation", "Parsed by Gemini AI")

            if (amount > 0.0) {
                return@withContext AiParsedLog(
                    type = type,
                    amount = amount,
                    category = category,
                    account = account,
                    note = note,
                    explanation = explanation
                )
            } else {
                return@withContext fallbackLocalParse(input)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini parsing failed, using fallback", e)
            return@withContext fallbackLocalParse(input)
        }
    }

    /**
     * Ask Gemini financial advisor about budget, savings, spending patterns
     */
    suspend fun askFinancialAdvisor(
        financialContext: String,
        userQuestion: String
    ): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext "⚠️ Gemini API key is not configured yet. You can configure it in AI Studio Secrets or Settings.\n\nHere is a quick rule-of-thumb tip: Aim for the 50/30/20 rule: 50% for Needs, 30% for Wants, and 20% for Savings and debt repayment."
        }

        val prompt = """
            You are an expert personal finance advisor built into Money Manager.
            The user's current financial profile and recent spending snapshot:
            $financialContext
            
            User's question or request:
            "$userQuestion"
            
            Please provide a friendly, insightful, and actionable response.
            Focus on practical money-saving advice, budget warnings if any categories are too high, and clear suggestions.
            Keep formatting clean with bullet points and bold highlights.
        """.trimIndent()

        try {
            val responseText = callGeminiRestApi(prompt)
            return@withContext responseText ?: "Unable to get advice at this moment. Please check your internet connection."
        } catch (e: Exception) {
            Log.e(TAG, "Gemini advisor call failed", e)
            return@withContext "Error connecting to AI service: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    private fun callGeminiRestApi(promptText: String): String? {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return null

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string()
            Log.e(TAG, "Gemini API HTTP ${response.code}: $errBody")
            return null
        }

        val resBodyString = response.body?.string() ?: return null
        val root = JSONObject(resBodyString)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        val stringBuilder = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            stringBuilder.append(part.optString("text", ""))
        }

        return stringBuilder.toString().trim()
    }

    private fun extractJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        val startIdx = clean.indexOf("{")
        val endIdx = clean.lastIndexOf("}")
        return if (startIdx != -1 && endIdx != -1 && endIdx >= startIdx) {
            clean.substring(startIdx, endIdx + 1).trim()
        } else {
            clean.trim()
        }
    }

    private fun fallbackLocalParse(input: String): AiParsedLog? {
        val lower = input.lowercase()
        val numRegex = Regex("""(?:tk|bdt|\$|rs)?\s*([0-9]+(?:\.[0-9]{1,2})?)\s*(?:tk|bdt)?""", RegexOption.IGNORE_CASE)
        val match = numRegex.find(input)
        val amount = match?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: return null

        val isIncome = lower.contains("salary") || lower.contains("received") || lower.contains("deposit") || lower.contains("cashback")
        val isTransfer = lower.contains("transfer") || lower.contains("send money")

        val type = when {
            isIncome -> TransactionType.INCOME
            isTransfer -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }

        val category = when {
            lower.contains("food") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("snack") || lower.contains("tea") -> "Food"
            lower.contains("grocery") || lower.contains("market") || lower.contains("shwapno") -> "Groceries"
            lower.contains("uber") || lower.contains("rickshaw") || lower.contains("bus") || lower.contains("cng") || lower.contains("fuel") -> "Transport"
            lower.contains("bill") || lower.contains("wifi") || lower.contains("recharge") || lower.contains("electricity") -> "Bills"
            lower.contains("salary") -> "Salary"
            lower.contains("rent") -> "Rent"
            else -> "Other"
        }

        val account = when {
            lower.contains("bkash") -> "bKash"
            lower.contains("nagad") -> "Nagad"
            lower.contains("bank") || lower.contains("card") -> "Bank"
            else -> "Cash"
        }

        return AiParsedLog(
            type = type,
            amount = amount,
            category = category,
            account = account,
            note = input.trim(),
            explanation = "Parsed with smart local parser"
        )
    }
}
