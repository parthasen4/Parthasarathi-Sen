package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.FinLedgerTheme
import com.example.viewmodel.FinanceViewModel

enum class Screen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    LEDGER("Ledger", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    ACCOUNTS("Accounts", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    PORTFOLIO("Invest & Loans", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    ANALYTICS("Analytics", Icons.Filled.PieChart, Icons.Outlined.PieChartOutline)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinLedgerTheme {
                val viewModel: FinanceViewModel = viewModel(
                    factory = FinanceViewModel.provideFactory(this)
                )
                FinLedgerApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinLedgerApp(viewModel: FinanceViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by viewModel.transactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val investments by viewModel.investments.collectAsStateWithLifecycle(initialValue = emptyList())
    val loans by viewModel.loans.collectAsStateWithLifecycle(initialValue = emptyList())

    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showCreditCardEmiDialog by remember { mutableStateOf(false) }
    var creditCardToConvert by remember { mutableStateOf<Account?>(null) }
    var loanToRepay by remember { mutableStateOf<Loan?>(null) }

    // Intercept back button to return to Dashboard if on sub-screens
    if (currentScreen != Screen.DASHBOARD) {
        BackHandler {
            currentScreen = Screen.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.finance_ledger_logo_1791999567085),
                            contentDescription = "Ledger Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (currentScreen == Screen.DASHBOARD) "Personal Finance" else currentScreen.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                Screen.values().forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTxDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Transaction")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    accounts = accounts,
                    investments = investments,
                    loans = loans,
                    transactions = transactions,
                    categories = categories,
                    onAddTransactionClick = { showAddTxDialog = true },
                    onQuickActionClick = { txType ->
                        showAddTxDialog = true
                    },
                    onConvertToEmiClick = {
                        creditCardToConvert = accounts.firstOrNull { it.type == AccountType.CREDIT_CARD }
                        showCreditCardEmiDialog = true
                    },
                    onSeeAllTransactions = {
                        viewModel.setFilterType(null)
                        viewModel.setSearchQuery("")
                        currentScreen = Screen.LEDGER
                    },
                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                )
                Screen.LEDGER -> LedgerScreen(
                    transactions = transactions,
                    accounts = accounts,
                    categories = categories,
                    loans = loans,
                    selectedFilter = selectedTypeFilter,
                    searchQuery = searchQuery,
                    onFilterSelect = { viewModel.setFilterType(it) },
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
                )
                Screen.ACCOUNTS -> AccountsScreen(
                    accounts = accounts,
                    onAddAccountClick = { showAddAccountDialog = true },
                    onDeleteAccount = { acc -> viewModel.deleteAccount(acc) },
                    onConvertToEmi = { card ->
                        creditCardToConvert = card
                        showCreditCardEmiDialog = true
                    }
                )
                Screen.PORTFOLIO -> InvestmentsLoansScreen(
                    investments = investments,
                    loans = loans,
                    accounts = accounts,
                    onAddInvestmentClick = { showAddInvestmentDialog = true },
                    onAddLoanClick = { showAddLoanDialog = true },
                    onRepayLoanClick = { loan -> loanToRepay = loan },
                    onDeleteInvestment = { inv -> viewModel.deleteInvestment(inv) },
                    onDeleteLoan = { loan -> viewModel.deleteLoan(loan) }
                )
                Screen.ANALYTICS -> AnalyticsScreen(
                    transactions = transactions,
                    categories = categories,
                    accounts = accounts,
                    loans = loans,
                    investments = investments
                )
            }
        }
    }

    // Dialogs
    if (showAddTxDialog) {
        AddTransactionDialog(
            accounts = accounts,
            categories = categories,
            loans = loans,
            onDismiss = { showAddTxDialog = false },
            onSave = { amount, type, catId, fromId, toId, loanId, dateStr, notes ->
                viewModel.logTransaction(amount, type, catId, fromId, toId, loanId, dateStr, notes)
            },
            onQuickAddCategory = { catName, catType ->
                viewModel.addCategory(catName, catType)
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, type, balance, limit ->
                viewModel.addAccount(name, type, balance, limit)
            }
        )
    }

    if (showAddInvestmentDialog) {
        AddInvestmentDialog(
            accounts = accounts,
            onDismiss = { showAddInvestmentDialog = false },
            onSave = { name, type, principal, monthly, rate, tenure, dateStr, accId ->
                viewModel.addInvestment(name, type, principal, monthly, rate, tenure, dateStr, accId)
            }
        )
    }

    if (showAddLoanDialog) {
        AddLoanDialog(
            onDismiss = { showAddLoanDialog = false },
            onSave = { name, total, rate, tenure, dateStr ->
                viewModel.addLoan(name, total, rate, tenure, dateStr)
            }
        )
    }

    loanToRepay?.let { loan ->
        RepayLoanDialog(
            loan = loan,
            accounts = accounts,
            onDismiss = { loanToRepay = null },
            onRepay = { amount, fromAccountId, dateStr, notes ->
                viewModel.logTransaction(
                    amount = amount,
                    type = TransactionType.LOAN_REPAY,
                    catId = null,
                    fromId = fromAccountId,
                    toId = null,
                    loanId = loan.id,
                    dateStr = dateStr,
                    notes = notes
                )
            }
        )
    }

    if (showCreditCardEmiDialog) {
        CreditCardEmiDialog(
            creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD },
            preselectedCardId = creditCardToConvert?.id,
            onDismiss = {
                showCreditCardEmiDialog = false
                creditCardToConvert = null
            },
            onConfirm = { cardId, amount, rate, tenure, dateStr, notes ->
                viewModel.convertCreditCardToEmi(cardId, amount, rate, tenure, dateStr, notes)
                showCreditCardEmiDialog = false
                creditCardToConvert = null
            }
        )
    }
}
