package com.example.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "SmsReceiver"
        const val ACTION_SMS_LOGGED = "com.example.ACTION_SMS_TRANSACTION_LOGGED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SMS messages from intent", e)
            return
        }

        if (messages.isNullOrEmpty()) return

        // Combine multipart messages from same sender
        val sender = messages[0].originatingAddress ?: "Cellular SMS"
        val bodyBuilder = StringBuilder()
        var timestamp = System.currentTimeMillis()

        for (msg in messages) {
            bodyBuilder.append(msg.messageBody)
            if (msg.timestampMillis > 0) {
                timestamp = msg.timestampMillis
            }
        }

        val fullBody = bodyBuilder.toString()
        Log.d(TAG, "Received SMS from $sender: $fullBody")

        // Parse via SmsTransactionParser
        val parsed = SmsTransactionParser.parse(sender, fullBody, timestamp) ?: return

        Log.i(TAG, "Parsed financial transaction: ${parsed.type} ${parsed.amount} from ${parsed.account}")

        // Save into Room database asynchronously
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val entity = parsed.toTransactionEntity()
                db.transactionDao().insertTransaction(entity)

                // Show user notification
                val sign = if (parsed.type == TransactionType.INCOME) "+" else "-"
                val title = "Auto-Logged: $sign${parsed.amount.toInt()} (${parsed.account})"
                val notifBody = "${parsed.category}: ${parsed.note}\nCaptured automatically from cellular message."
                NotificationHelper.showAutoLogNotification(context, title, notifBody)

                // Send local broadcast to notify in-app screens
                val updateIntent = Intent(ACTION_SMS_LOGGED).apply {
                    putExtra("amount", parsed.amount)
                    putExtra("category", parsed.category)
                    putExtra("account", parsed.account)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(updateIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-saving SMS transaction", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
