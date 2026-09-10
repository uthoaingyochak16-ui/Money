package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.sms.SampleSms
import com.example.data.sms.SmsScanSummary
import com.example.data.sms.SmsSyncHelper
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun SmsSimulatorDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var customSender by remember { mutableStateOf("bKash") }
    var customBody by remember {
        mutableStateOf("Payment Tk 350.00 to Pizza Hut Successful. Balance Tk 4,120.00. TrxID 9K2L4P8Q")
    }

    var lastLoggedInfo by remember { mutableStateOf<String?>(null) }
    var isSimulating by remember { mutableStateOf(false) }
    var isScanningInbox by remember { mutableStateOf(false) }
    var scanResultSummary by remember { mutableStateOf<SmsScanSummary?>(null) }

    var hasPermissions by remember {
        mutableStateOf(SmsSyncHelper.hasSmsPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions[Manifest.permission.RECEIVE_SMS] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cellular SMS Auto-Entry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Permission status badge & prompt
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasPermissions) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasPermissions) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasPermissions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasPermissions) "Cellular SMS Listener Active" else "SMS Permissions Needed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasPermissions) {
                                    "Ready to catch live incoming cellular SMS from bKash, Nagad, and banks."
                                } else {
                                    "Grant RECEIVE_SMS to catch live incoming bank messages."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!hasPermissions) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    val reqs = mutableListOf(
                                        Manifest.permission.RECEIVE_SMS,
                                        Manifest.permission.READ_SMS
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        reqs.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    permissionLauncher.launch(reqs.toTypedArray())
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Grant", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // 2. Scan Device Inbox Button
                OutlinedButton(
                    onClick = {
                        isScanningInbox = true
                        scanResultSummary = null
                        viewModel.syncInboxSms { summary ->
                            isScanningInbox = false
                            scanResultSummary = summary
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isScanningInbox) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning Device Inbox...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan & Import Existing SMS from Inbox", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (scanResultSummary != null) {
                    Text(
                        text = "Scanned ${scanResultSummary!!.totalSmsFound} SMS messages. Auto-logged ${scanResultSummary!!.autoLoggedCount} transactions (Total: ৳${scanResultSummary!!.totalAmount.toInt()}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // 3. One-Click Test Presets (for Instant Demonstration)
                Text(
                    text = "Test Incoming Cellular Messages (One-Click Simulator)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Tap any preset below to simulate an incoming cellular message. It tests the regex parser, auto-inserts the transaction in Room, and fires a system notification.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmsSyncHelper.sampleSmsPresets.forEach { preset ->
                        PresetSmsCard(
                            preset = preset,
                            onTrigger = {
                                isSimulating = true
                                viewModel.simulateSmsTransaction(preset.sender, preset.body) { tx ->
                                    isSimulating = false
                                    if (tx != null) {
                                        lastLoggedInfo = "Auto-Logged: ${tx.account} ৳${tx.amount.toInt()} (${tx.category}) from '${preset.sender}'"
                                    }
                                }
                            }
                        )
                    }
                }

                // Status of last simulated SMS
                if (lastLoggedInfo != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lastLoggedInfo!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // 4. Custom SMS Test Field
                Text(
                    text = "Custom SMS Test",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text("Sender Name (e.g. bKash, Nagad, CityBank)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 13.sp)
                )

                OutlinedTextField(
                    value = customBody,
                    onValueChange = { customBody = it },
                    label = { Text("SMS Message Body") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    textStyle = TextStyle(fontSize = 12.sp)
                )

                Button(
                    onClick = {
                        isSimulating = true
                        viewModel.simulateSmsTransaction(customSender, customBody) { tx ->
                            isSimulating = false
                            if (tx != null) {
                                lastLoggedInfo = "Auto-Logged: ${tx.account} ৳${tx.amount.toInt()} (${tx.category})"
                            } else {
                                lastLoggedInfo = "Could not parse monetary amount from custom SMS."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate Custom Cellular Message")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PresetSmsCard(
    preset: SampleSms,
    onTrigger: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTrigger),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = preset.body,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
