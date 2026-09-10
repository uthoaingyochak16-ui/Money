package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ai.GeminiAiService
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewLogBottomSheet(
    sheetState: SheetState,
    currencySymbol: String,
    editingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amount: Double,
        category: String,
        account: String,
        toAccount: String?,
        note: String,
        timestamp: Long,
        imageUri: String?,
        fileUri: String?,
        fileName: String?,
        remoteLink: String?
    ) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedType by remember {
        mutableStateOf(editingTransaction?.type ?: TransactionType.EXPENSE)
    }

    var amountText by remember {
        mutableStateOf(
            if (editingTransaction != null) {
                if (editingTransaction.amount % 1.0 == 0.0) {
                    editingTransaction.amount.toInt().toString()
                } else {
                    editingTransaction.amount.toString()
                }
            } else ""
        )
    }

    var selectedCategory by remember {
        mutableStateOf(
            editingTransaction?.category ?: if (selectedType == TransactionType.INCOME) "Salary" else "Food"
        )
    }

    var selectedAccount by remember {
        mutableStateOf(editingTransaction?.account ?: "Cash")
    }

    var selectedToAccount by remember {
        mutableStateOf(editingTransaction?.toAccount ?: "bKash")
    }

    var noteText by remember {
        mutableStateOf(editingTransaction?.note ?: "")
    }

    var timestamp by remember {
        mutableStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis())
    }

    // Attachments State
    var imageUri by remember { mutableStateOf(editingTransaction?.imageUri) }
    var fileUri by remember { mutableStateOf(editingTransaction?.fileUri) }
    var fileName by remember { mutableStateOf(editingTransaction?.fileName) }
    var remoteLink by remember { mutableStateOf(editingTransaction?.remoteLink ?: "") }
    var showLinkInput by remember { mutableStateOf(!editingTransaction?.remoteLink.isNullOrBlank()) }

    // AI Smart Fill State
    var showAiSmartFillDialog by remember { mutableStateOf(false) }
    var aiInputText by remember { mutableStateOf("") }
    var isAiAnalyzing by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    // Sub-pickers states
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showToAccountPicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

    // Activity Result Launchers for Media/Files
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fileUri = uri.toString()
            var detectedName: String? = null
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            detectedName = it.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fileName = detectedName ?: uri.lastPathSegment ?: "Document"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header bar: Close, Title, Delete (if editing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_log_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (editingTransaction == null) "New Log" else "Edit Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (editingTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(editingTransaction)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_log_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // AI Quick Fill Header Button
                    IconButton(
                        onClick = { showAiSmartFillDialog = true },
                        modifier = Modifier.testTag("ai_smart_fill_header_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Auto-Fill",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Smart Quick Fill Banner (for New Logs)
            if (editingTransaction == null) {
                Card(
                    onClick = { showAiSmartFillDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✨ AI Smart Fill / Paste SMS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Type naturally or paste bank/bKash SMS to auto-fill",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Segmented Control: Expense | Income | Transfer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income",
                    TransactionType.TRANSFER to "Transfer"
                ).forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                selectedType = type
                                if (type == TransactionType.INCOME && selectedCategory in CategoryConstants.expenseCategories) {
                                    selectedCategory = "Salary"
                                } else if (type == TransactionType.EXPENSE && selectedCategory in CategoryConstants.incomeCategories) {
                                    selectedCategory = "Food"
                                }
                            }
                            .padding(vertical = 8.dp)
                            .testTag("segment_${type.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = amountText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                amountText = newValue
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true,
                        modifier = Modifier.testTag("amount_input_field"),
                        decorationBox = { innerTextField ->
                            if (amountText.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = TextStyle(
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Quick Increment Chips
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 500, 1000, 5000).forEach { inc ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    val cur = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (cur + inc).toInt().toString()
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+$currencySymbol$inc",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Category (for Expense/Income)
            if (selectedType != TransactionType.TRANSFER) {
                FormRow(
                    label = "Category",
                    value = selectedCategory,
                    leadingIcon = {
                        CategoryIconBadge(
                            category = selectedCategory,
                            type = selectedType,
                            size = 28.dp,
                            iconSize = 15.dp
                        )
                    },
                    onClick = { showCategoryPicker = !showCategoryPicker },
                    testTag = "category_selector_row"
                )

                if (showCategoryPicker) {
                    val catList = if (selectedType == TransactionType.EXPENSE) {
                        CategoryConstants.expenseCategories
                    } else {
                        CategoryConstants.incomeCategories
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        catList.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedCategory = cat
                                        showCategoryPicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // 2. Account
            FormRow(
                label = if (selectedType == TransactionType.TRANSFER) "From Account" else "Account",
                value = selectedAccount,
                onClick = { showAccountPicker = !showAccountPicker },
                testTag = "account_selector_row"
            )

            if (showAccountPicker) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryConstants.defaultAccounts.forEach { acc ->
                        val isSelected = selectedAccount == acc
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedAccount = acc
                                    showAccountPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = acc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 3. To Account (for Transfer)
            if (selectedType == TransactionType.TRANSFER) {
                FormRow(
                    label = "To Account",
                    value = selectedToAccount,
                    onClick = { showToAccountPicker = !showToAccountPicker },
                    testTag = "to_account_selector_row"
                )

                if (showToAccountPicker) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryConstants.defaultAccounts.forEach { acc ->
                            val isSelected = selectedToAccount == acc
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedToAccount = acc
                                        showToAccountPicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = acc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            // 4. Date & Time
            FormRow(
                label = "Date",
                value = dateFormat.format(Date(timestamp)),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    val cal = Calendar.getInstance()
                    if (timestamp > System.currentTimeMillis() - 24 * 60 * 60 * 1000L) {
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        timestamp = cal.timeInMillis
                    } else {
                        timestamp = System.currentTimeMillis()
                    }
                },
                testTag = "date_selector_row"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 5. Note / Description
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(90.dp)
                )

                BasicTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("note_input_field"),
                    decorationBox = { innerTextField ->
                        if (noteText.isEmpty()) {
                            Text(
                                text = "e.g. Lunch, Bus, Grocery, etc.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 6. Attachments & Remote Link Section
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Attachments & Links",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons for Attachments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Photo button
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("attach_photo_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo", style = MaterialTheme.typography.labelMedium)
                }

                // File button
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f).testTag("attach_file_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("File", style = MaterialTheme.typography.labelMedium)
                }

                // Remote Link button
                OutlinedButton(
                    onClick = { showLinkInput = !showLinkInput },
                    modifier = Modifier.weight(1f).testTag("attach_link_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Link", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Attached Media Preview Row
            if (imageUri != null || fileUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo thumbnail
                    if (imageUri != null) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Attached receipt",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp)
                            )
                            // Remove button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                                    .clickable { imageUri = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // File badge
                    if (fileUri != null) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fileName ?: "Attached File",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        fileUri = null
                                        fileName = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove file",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Remote Link Input Field
            AnimatedVisibility(visible = showLinkInput) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = remoteLink,
                        onValueChange = { remoteLink = it },
                        placeholder = { Text("https://example.com/receipt or invoice link") },
                        label = { Text("Remote Link URL") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (remoteLink.isNotBlank()) {
                                IconButton(onClick = { remoteLink = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear link", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remote_link_input_field"),
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            val isValid = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSave(
                            selectedType,
                            amt,
                            if (selectedType == TransactionType.TRANSFER) "Transfer" else selectedCategory,
                            selectedAccount,
                            if (selectedType == TransactionType.TRANSFER) selectedToAccount else null,
                            noteText,
                            timestamp,
                            imageUri,
                            fileUri,
                            fileName,
                            if (remoteLink.isNotBlank()) remoteLink.trim() else null
                        )
                        onDismiss()
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = if (editingTransaction == null) "Save Transaction" else "Update Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // AI Smart Auto-Fill Dialog
    if (showAiSmartFillDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAiAnalyzing) {
                    showAiSmartFillDialog = false
                    aiErrorMessage = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Smart Auto-Fill", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste any message, cellular SMS, or type naturally in Bengali/English. AI will parse the amount, category, account, and note automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = aiInputText,
                        onValueChange = {
                            aiInputText = it
                            aiErrorMessage = null
                        },
                        placeholder = {
                            Text("e.g.\n• 'Paid 450 tk for pizza using bKash'\n• 'bKash: Payment Tk 350 to Merchant'\n• 'Salary 45000 tk received in bank'")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("ai_fill_input_text"),
                        textStyle = TextStyle(fontSize = 13.sp)
                    )

                    if (aiErrorMessage != null) {
                        Text(
                            text = aiErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (isAiAnalyzing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing with AI...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (aiInputText.isNotBlank()) {
                            isAiAnalyzing = true
                            aiErrorMessage = null
                            coroutineScope.launch {
                                val result = GeminiAiService.parseLogFromText(aiInputText)
                                isAiAnalyzing = false
                                if (result != null && result.amount > 0.0) {
                                    selectedType = result.type
                                    amountText = if (result.amount % 1.0 == 0.0) result.amount.toInt().toString() else result.amount.toString()
                                    selectedCategory = result.category
                                    selectedAccount = result.account
                                    noteText = result.note
                                    showAiSmartFillDialog = false
                                } else {
                                    aiErrorMessage = "Could not detect transaction details. Please check the text."
                                }
                            }
                        }
                    },
                    enabled = aiInputText.isNotBlank() && !isAiAnalyzing,
                    modifier = Modifier.testTag("ai_parse_submit_btn")
                ) {
                    Text("Auto-Fill")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAiSmartFillDialog = false
                        aiErrorMessage = null
                    },
                    enabled = !isAiAnalyzing
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FormRow(
    label: String,
    value: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}
