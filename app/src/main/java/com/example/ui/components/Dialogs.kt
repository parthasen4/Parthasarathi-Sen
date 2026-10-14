package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.FinanceUtils
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    loans: List<Loan>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: TransactionType, catId: Long?, fromId: Long, toId: Long?, loanId: Long?, dateStr: String, notes: String?) -> Unit,
    onQuickAddCategory: (String, CategoryType) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(FinanceUtils.getTodayDdMmYyyy()) }

    var selectedFromAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.type != AccountType.CREDIT_CARD }?.id ?: accounts.firstOrNull()?.id ?: 0L)
    }
    var selectedToAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.type == AccountType.CREDIT_CARD }?.id ?: accounts.getOrNull(1)?.id ?: 0L)
    }

    val filteredCategories = categories.filter {
        if (selectedType == TransactionType.INCOME) it.type == CategoryType.INCOME
        else it.type == CategoryType.EXPENSE
    }
    var selectedCategoryId by remember(filteredCategories) {
        mutableStateOf(filteredCategories.firstOrNull()?.id)
    }

    var selectedLoanId by remember(loans) {
        mutableStateOf(loans.firstOrNull()?.id)
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val availableTransferToAccounts = remember(accounts, selectedFromAccountId) {
        accounts.filter { it.id != selectedFromAccountId }
    }

    LaunchedEffect(selectedFromAccountId, selectedType, availableTransferToAccounts) {
        if (selectedType == TransactionType.TRANSFER) {
            if (selectedToAccountId == selectedFromAccountId || availableTransferToAccounts.none { it.id == selectedToAccountId }) {
                selectedToAccountId = availableTransferToAccounts.firstOrNull()?.id ?: 0L
            }
        }
    }

    var showNewCategoryDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_transaction_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Log Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Transaction Type Tabs (Scrollable Row)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TransactionType.values().forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = TransactionType.values().size),
                            onClick = { selectedType = type },
                            selected = selectedType == type,
                            modifier = Modifier.testTag("tx_type_button_${type.name}")
                        ) {
                            Text(
                                text = when (type) {
                                    TransactionType.EXPENSE -> "Expense"
                                    TransactionType.INCOME -> "Income"
                                    TransactionType.TRANSFER -> "Transfer"
                                    TransactionType.CARD_BILL_PAY -> "Card Bill"
                                    TransactionType.LOAN_REPAY -> "Loan EMI"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // From Account Selection
                Text("From Account", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                AccountDropdownSelector(
                    accounts = accounts,
                    selectedAccountId = selectedFromAccountId,
                    onSelect = { selectedFromAccountId = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedType) {
                    TransactionType.EXPENSE, TransactionType.INCOME -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category", style = MaterialTheme.typography.labelMedium)
                            TextButton(onClick = { showNewCategoryDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Category")
                            }
                        }
                        CategoryDropdownSelector(
                            categories = filteredCategories,
                            selectedCategoryId = selectedCategoryId,
                            onSelect = { selectedCategoryId = it }
                        )
                    }
                    TransactionType.TRANSFER -> {
                        Text("To Destination Account", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (availableTransferToAccounts.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Transfer not allowed: You must have at least 2 distinct accounts to make a transfer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        } else {
                            AccountDropdownSelector(
                                accounts = availableTransferToAccounts,
                                selectedAccountId = selectedToAccountId,
                                onSelect = {
                                    selectedToAccountId = it
                                    errorMessage = null
                                }
                            )
                        }
                    }
                    TransactionType.CARD_BILL_PAY -> {
                        Text("Pay Towards Credit Card", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        val availableCards = accounts.filter { it.type == AccountType.CREDIT_CARD && it.id != selectedFromAccountId }
                        if (availableCards.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CreditOrange.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = CreditOrange, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No credit card found to pay towards. Please add a credit card account first.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CreditOrange
                                    )
                                }
                            }
                        } else {
                            AccountDropdownSelector(
                                accounts = availableCards,
                                selectedAccountId = selectedToAccountId,
                                onSelect = {
                                    selectedToAccountId = it
                                    errorMessage = null
                                }
                            )
                        }
                    }
                    TransactionType.LOAN_REPAY -> {
                        Text("Select Loan to Repay", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (loans.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = LoanPurple.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = LoanPurple, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No active loans found. Please add a loan under Invest & Loans first.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LoanPurple
                                    )
                                }
                            }
                        } else {
                            LoanDropdownSelector(
                                loans = loans,
                                selectedLoanId = selectedLoanId,
                                onSelect = {
                                    selectedLoanId = it
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (ddMMyyyy)") },
                    placeholder = { Text("e.g. 14102026") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_date_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Description (Optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_notes_input"),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = ExpenseRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount <= 0.0) {
                                errorMessage = "Please enter an amount greater than ₹0."
                                return@Button
                            }
                            if (selectedFromAccountId <= 0) {
                                errorMessage = "Please select a source account."
                                return@Button
                            }

                            if (selectedType == TransactionType.TRANSFER) {
                                if (selectedFromAccountId == selectedToAccountId) {
                                    errorMessage = "Transfer in same account not allowed. Please select a different destination account."
                                    return@Button
                                }
                                if (availableTransferToAccounts.isEmpty() || selectedToAccountId <= 0L) {
                                    errorMessage = "Transfer in same account not allowed. No distinct destination account available."
                                    return@Button
                                }
                            }

                            val toId = if (selectedType == TransactionType.TRANSFER || selectedType == TransactionType.CARD_BILL_PAY) selectedToAccountId else null
                            val loanId = if (selectedType == TransactionType.LOAN_REPAY) selectedLoanId else null
                            val catId = if (selectedType == TransactionType.INCOME || selectedType == TransactionType.EXPENSE) selectedCategoryId else null
                            onSave(
                                amount,
                                selectedType,
                                catId,
                                selectedFromAccountId,
                                toId,
                                loanId,
                                dateText.ifBlank { FinanceUtils.getTodayDdMmYyyy() },
                                notesText.ifBlank { null }
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_transaction_button")
                    ) {
                        Text("Save Transaction")
                    }
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (catName.isNotBlank()) {
                        val catType = if (selectedType == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                        onQuickAddCategory(catName.trim(), catType)
                        showNewCategoryDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AccountDropdownSelector(
    accounts: List<Account>,
    selectedAccountId: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.find { it.id == selectedAccountId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.let { "${it.name} (${FinanceUtils.formatCurrency(it.currentBalance)})" } ?: "Select Account",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(acc.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${acc.type.name} • ${FinanceUtils.formatCurrency(acc.currentBalance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(acc.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryDropdownSelector(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.find { it.id == selectedCategoryId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.name ?: if (categories.isEmpty()) "No categories available" else "Select Category",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = {
                        onSelect(cat.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LoanDropdownSelector(
    loans: List<Loan>,
    selectedLoanId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = loans.find { it.id == selectedLoanId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected?.let { "${it.lenderName} (Due: ${FinanceUtils.formatCurrency(it.remainingPrincipal)})" }
                        ?: if (loans.isEmpty()) "No loans found" else "Select Loan",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            loans.forEach { loan ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(loan.lenderName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Remaining: ${FinanceUtils.formatCurrency(loan.remainingPrincipal)} • EMI: ${FinanceUtils.formatCurrency(loan.emiAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(loan.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: AccountType, balance: Double, limit: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var balanceText by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_account_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Add Financial Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. Chase Savings, Wallet, Amex") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Account Type", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AccountType.values().forEachIndexed { index, accType ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AccountType.values().size),
                            onClick = { type = accType },
                            selected = type == accType
                        ) {
                            Text(accType.name.replace("_", " "), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text(if (type == AccountType.CREDIT_CARD) "Initial Outstanding Balance (-₹)" else "Starting Balance (₹)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input"),
                    singleLine = true
                )

                if (type == AccountType.CREDIT_CARD) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Credit Card Limit (₹)") },
                        placeholder = { Text("5000.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_limit_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                var bal = balanceText.toDoubleOrNull() ?: 0.0
                                if (type == AccountType.CREDIT_CARD && bal > 0) {
                                    bal = -bal // Outstanding balances are negative
                                }
                                val limit = limitText.toDoubleOrNull()
                                onSave(name, type, bal, limit)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("save_account_button")
                    ) {
                        Text("Add Account")
                    }
                }
            }
        }
    }
}

@Composable
fun AddInvestmentDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (name: String, type: InvestmentType, principal: Double, monthly: Double?, rate: Double, tenure: Int, dateStr: String, accId: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(InvestmentType.FD) }
    var principalText by remember { mutableStateOf("") }
    var monthlyText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("7.0") }
    var tenureText by remember { mutableStateOf("12") }
    var dateText by remember { mutableStateOf(FinanceUtils.getTodayDdMmYyyy()) }
    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.type != AccountType.CREDIT_CARD }?.id ?: accounts.firstOrNull()?.id ?: 0L)
    }

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val monthly = monthlyText.toDoubleOrNull()
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val tenure = tenureText.toIntOrNull() ?: 0

    val previewMaturity = remember(principal, monthly, rate, tenure, type) {
        if (type == InvestmentType.FD) {
            FinanceUtils.calculateFDMaturity(principal, rate, tenure)
        } else {
            FinanceUtils.calculateRDMaturity(monthly ?: principal, rate, tenure)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_investment_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Add Investment (FD / RD)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    InvestmentType.values().forEachIndexed { index, invType ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = InvestmentType.values().size),
                            onClick = { type = invType },
                            selected = type == invType
                        ) {
                            Text(if (invType == InvestmentType.FD) "Fixed Deposit (FD)" else "Recurring Deposit (RD)")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Investment Name / Bank") },
                    placeholder = { Text("e.g. 2-Year High Yield FD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (type == InvestmentType.FD) {
                    OutlinedTextField(
                        value = principalText,
                        onValueChange = { principalText = it },
                        label = { Text("Principal Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = monthlyText,
                        onValueChange = { monthlyText = it },
                        label = { Text("Monthly Deposit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Rate (% p.a.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tenureText,
                        onValueChange = { tenureText = it },
                        label = { Text("Tenure (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Debit From Account", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                AccountDropdownSelector(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onSelect = { selectedAccountId = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Start Date (ddMMyyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Maturity Preview Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Estimated Maturity Value:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = FinanceUtils.formatCurrency(previewMaturity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && tenure > 0 && selectedAccountId > 0) {
                                val p = if (type == InvestmentType.FD) principal else (monthly ?: 0.0)
                                onSave(name, type, p, monthly, rate, tenure, dateText, selectedAccountId)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("save_investment_button")
                    ) {
                        Text("Save Investment")
                    }
                }
            }
        }
    }
}

@Composable
fun AddLoanDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, total: Double, rate: Double, tenure: Int, dateStr: String) -> Unit
) {
    var lenderName by remember { mutableStateOf("") }
    var totalAmountText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("7.5") }
    var tenureText by remember { mutableStateOf("36") }
    var dateText by remember { mutableStateOf(FinanceUtils.getTodayDdMmYyyy()) }

    val total = totalAmountText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val tenure = tenureText.toIntOrNull() ?: 0

    val calculatedEMI = remember(total, rate, tenure) {
        FinanceUtils.calculateEMI(total, rate, tenure)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_loan_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Add New Loan / Liability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = lenderName,
                    onValueChange = { lenderName = it },
                    label = { Text("Lender / Loan Name") },
                    placeholder = { Text("e.g. Home Mortgage, Car Loan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it },
                    label = { Text("Principal Borrowed (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("Interest Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tenureText,
                        onValueChange = { tenureText = it },
                        label = { Text("Tenure (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Start Date (ddMMyyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // EMI Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Calculated Monthly EMI:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = FinanceUtils.formatCurrency(calculatedEMI),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LoanPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (lenderName.isNotBlank() && total > 0.0 && tenure > 0) {
                                onSave(lenderName, total, rate, tenure, dateText)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("save_loan_button")
                    ) {
                        Text("Add Loan")
                    }
                }
            }
        }
    }
}

@Composable
fun RepayLoanDialog(
    loan: Loan,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onRepay: (amount: Double, fromAccountId: Long, dateStr: String, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(loan.emiAmount.toString()) }
    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull { it.type != AccountType.CREDIT_CARD }?.id ?: accounts.firstOrNull()?.id ?: 0L)
    }
    var notesText by remember { mutableStateOf("EMI Payment for ${loan.lenderName}") }
    val today = FinanceUtils.getTodayDdMmYyyy()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("repay_loan_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Make Loan Payment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${loan.lenderName} • Remaining Due: ${FinanceUtils.formatCurrency(loan.remainingPrincipal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Repayment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Pay From Account", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                AccountDropdownSelector(
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    onSelect = { selectedAccountId = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && selectedAccountId > 0) {
                                onRepay(amt, selectedAccountId, today, notesText)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("confirm_repay_loan_button")
                    ) {
                        Text("Confirm Payment")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardEmiDialog(
    creditCards: List<Account>,
    preselectedCardId: Long? = null,
    initialAmount: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (cardId: Long, amount: Double, rate: Double, tenure: Int, dateStr: String, notes: String?) -> Unit
) {
    var selectedCardId by remember(creditCards, preselectedCardId) {
        mutableStateOf(preselectedCardId ?: creditCards.firstOrNull()?.id ?: 0L)
    }
    val selectedCard = creditCards.find { it.id == selectedCardId }
    val outstandingDue = selectedCard?.let { if (it.currentBalance < 0) abs(it.currentBalance) else 0.0 } ?: 0.0

    var amountText by remember(selectedCard, initialAmount) {
        mutableStateOf(
            if (initialAmount != null && initialAmount > 0) initialAmount.toInt().toString()
            else if (outstandingDue > 0) outstandingDue.toInt().toString()
            else ""
        )
    }
    var rateText by remember { mutableStateOf("14.0") }
    var selectedTenureMonths by remember { mutableIntStateOf(6) }
    var notesText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(FinanceUtils.getTodayDdMmYyyy()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val tenure = selectedTenureMonths

    val calculatedEMI = remember(amount, rate, tenure) {
        FinanceUtils.calculateEMI(amount, rate, tenure)
    }
    val totalRepayment = calculatedEMI * tenure
    val totalInterest = (totalRepayment - amount).coerceAtLeast(0.0)

    val tenureOptions = listOf(3, 6, 9, 12, 18, 24)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("credit_card_emi_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CreditOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = CreditOrange)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Credit Card EMI Conversion",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Convert card dues/spend into easy monthly installments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Selector
                Text("Select Credit Card", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                AccountDropdownSelector(
                    accounts = creditCards,
                    selectedAccountId = selectedCardId,
                    onSelect = {
                        selectedCardId = it
                        val card = creditCards.find { c -> c.id == it }
                        val due = card?.let { c -> if (c.currentBalance < 0) abs(c.currentBalance) else 0.0 } ?: 0.0
                        if (due > 0) amountText = due.toInt().toString()
                    }
                )

                if (selectedCard != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current Outstanding: ${FinanceUtils.formatCurrency(outstandingDue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount to Convert (₹)") },
                    placeholder = { Text("e.g. 10000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emi_amount_input"),
                    singleLine = true
                )

                if (outstandingDue > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { amountText = outstandingDue.toInt().toString() },
                            label = { Text("Full Due (${FinanceUtils.formatCurrency(outstandingDue)})") }
                        )
                        if (outstandingDue > 5000) {
                            SuggestionChip(
                                onClick = { amountText = (outstandingDue / 2).toInt().toString() },
                                label = { Text("50% (${FinanceUtils.formatCurrency(outstandingDue / 2)})") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tenure Selector
                Text("Select EMI Tenure", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tenureOptions.forEach { t ->
                        FilterChip(
                            selected = selectedTenureMonths == t,
                            onClick = { selectedTenureMonths = t },
                            label = { Text("$t Months", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("tenure_chip_$t")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interest Rate
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Annual Interest Rate (% p.a.)") },
                    placeholder = { Text("14.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Item / Purpose (Optional)") },
                    placeholder = { Text("e.g. Laptop purchase, Vacation EMI") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // EMI Calculation Preview Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monthly EMI Payment:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = FinanceUtils.formatCurrency(calculatedEMI),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LoanPurple
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Interest:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = FinanceUtils.formatCurrency(totalInterest),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Repayment:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = FinanceUtils.formatCurrency(totalRepayment),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedCardId <= 0) {
                                errorMessage = "Please select a credit card."
                                return@Button
                            }
                            if (amount <= 0.0) {
                                errorMessage = "Please enter an amount greater than ₹0."
                                return@Button
                            }
                            onConfirm(
                                selectedCardId,
                                amount,
                                rate,
                                tenure,
                                dateText,
                                notesText.ifBlank { null }
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditOrange),
                        modifier = Modifier.testTag("confirm_emi_conversion_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Convert to EMI")
                    }
                }
            }
        }
    }
}
