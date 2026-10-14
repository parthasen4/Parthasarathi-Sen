package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.model.*
import com.example.util.FinanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).financeDao()

    val accounts: Flow<List<Account>> = dao.getAllAccounts()
    val categories: Flow<List<Category>> = dao.getAllCategories()
    val transactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val investments: Flow<List<Investment>> = dao.getAllInvestments()
    val loans: Flow<List<Loan>> = dao.getAllLoans()

    // Filter states for UI
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existingAccounts = dao.getAllAccounts().first()
            if (existingAccounts.isEmpty()) {
                // Seed default accounts
                val checkingId = dao.insertAccount(
                    Account(name = "HDFC Salary Account", type = AccountType.BANK, currentBalance = 68500.00)
                )
                val cashId = dao.insertAccount(
                    Account(name = "Cash Wallet", type = AccountType.CASH, currentBalance = 4500.00)
                )
                val cardId = dao.insertAccount(
                    Account(
                        name = "ICICI Coral Credit Card",
                        type = AccountType.CREDIT_CARD,
                        currentBalance = -12450.00,
                        creditLimit = 150000.00
                    )
                )

                // Seed default categories
                val salaryCatId = dao.insertCategory(Category(name = "Salary", type = CategoryType.INCOME))
                val freelanceCatId = dao.insertCategory(Category(name = "Freelance", type = CategoryType.INCOME))
                val investmentCatId = dao.insertCategory(Category(name = "Investments Return", type = CategoryType.INCOME))

                val groceriesCatId = dao.insertCategory(Category(name = "Groceries", type = CategoryType.EXPENSE))
                val diningCatId = dao.insertCategory(Category(name = "Dining & Food", type = CategoryType.EXPENSE))
                val utilitiesCatId = dao.insertCategory(Category(name = "Utilities & Bills", type = CategoryType.EXPENSE))
                val rentCatId = dao.insertCategory(Category(name = "Rent & Housing", type = CategoryType.EXPENSE))
                val shoppingCatId = dao.insertCategory(Category(name = "Shopping", type = CategoryType.EXPENSE))
                val travelCatId = dao.insertCategory(Category(name = "Travel & Commute", type = CategoryType.EXPENSE))

                // Seed sample loan
                val todayStr = FinanceUtils.getTodayDdMmYyyy()
                val emiCar = FinanceUtils.calculateEMI(550000.0, 8.5, 48)
                val loanId = dao.insertLoan(
                    Loan(
                        lenderName = "SBI Car Loan",
                        totalAmount = 550000.0,
                        remainingPrincipal = 385000.0,
                        interestRate = 8.5,
                        tenureMonths = 48,
                        emiAmount = emiCar,
                        startDateDdMmYyyy = "01012025"
                    )
                )

                // Seed sample investment
                dao.insertInvestment(
                    Investment(
                        name = "SBI 2-Yr Fixed Deposit",
                        type = InvestmentType.FD,
                        principalAmount = 150000.0,
                        interestRate = 7.1,
                        tenureMonths = 24,
                        startDateDdMmYyyy = "15062025",
                        linkedAccountId = checkingId
                    )
                )
                dao.insertInvestment(
                    Investment(
                        name = "Post Office RD",
                        type = InvestmentType.RD,
                        principalAmount = 5000.0,
                        monthlyDeposit = 5000.0,
                        interestRate = 6.7,
                        tenureMonths = 12,
                        startDateDdMmYyyy = todayStr,
                        linkedAccountId = checkingId
                    )
                )

                // Seed sample transactions
                val now = System.currentTimeMillis()
                val dayMillis = 86400000L
                dao.baseInsertTransaction(
                    Transaction(
                        amount = 85000.00,
                        type = TransactionType.INCOME,
                        categoryId = salaryCatId,
                        fromAccountId = checkingId,
                        dateDdMmYyyy = todayStr,
                        timestamp = now - (dayMillis * 3),
                        notes = "Monthly tech paycheck"
                    )
                )
                dao.baseInsertTransaction(
                    Transaction(
                        amount = 4650.00,
                        type = TransactionType.EXPENSE,
                        categoryId = groceriesCatId,
                        fromAccountId = cardId,
                        dateDdMmYyyy = todayStr,
                        timestamp = now - (dayMillis * 2),
                        notes = "Supermarket monthly haul"
                    )
                )
                dao.baseInsertTransaction(
                    Transaction(
                        amount = 1280.00,
                        type = TransactionType.EXPENSE,
                        categoryId = diningCatId,
                        fromAccountId = cashId,
                        dateDdMmYyyy = todayStr,
                        timestamp = now - (dayMillis * 1),
                        notes = "Dinner with family"
                    )
                )
                dao.baseInsertTransaction(
                    Transaction(
                        amount = emiCar,
                        type = TransactionType.LOAN_REPAY,
                        categoryId = null,
                        fromAccountId = checkingId,
                        linkedLoanId = loanId,
                        dateDdMmYyyy = todayStr,
                        timestamp = now,
                        notes = "SBI Car Loan monthly EMI"
                    )
                )
            }
        }
    }

    fun setFilterType(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addAccount(name: String, type: AccountType, balance: Double, limit: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertAccount(
                Account(
                    name = name.trim(),
                    type = type,
                    currentBalance = balance,
                    creditLimit = limit
                )
            )
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAccount(account)
        }
    }

    fun addCategory(name: String, type: CategoryType) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCategory(Category(name = name.trim(), type = type))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCategory(category)
        }
    }

    fun logTransaction(
        amount: Double,
        type: TransactionType,
        catId: Long?,
        fromId: Long,
        toId: Long?,
        loanId: Long?,
        dateStr: String,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if ((type == TransactionType.TRANSFER || type == TransactionType.CARD_BILL_PAY) && fromId == toId) {
                return@launch
            }
            val tx = Transaction(
                amount = amount,
                type = type,
                categoryId = catId,
                fromAccountId = fromId,
                toAccountId = toId,
                linkedLoanId = loanId,
                dateDdMmYyyy = dateStr,
                timestamp = FinanceUtils.parseDateToEpoch(dateStr),
                notes = notes?.trim()
            )
            dao.executeFinancialTransaction(tx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.revertAndRemoveFinancialTransaction(tx)
        }
    }

    fun addInvestment(
        name: String,
        type: InvestmentType,
        principal: Double,
        monthly: Double?,
        rate: Double,
        tenure: Int,
        dateStr: String,
        accId: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val inv = Investment(
                name = name.trim(),
                type = type,
                principalAmount = principal,
                monthlyDeposit = monthly,
                interestRate = rate,
                tenureMonths = tenure,
                startDateDdMmYyyy = dateStr,
                linkedAccountId = accId
            )
            dao.insertInvestment(inv)
        }
    }

    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteInvestment(investment)
        }
    }

    fun addLoan(
        name: String,
        total: Double,
        rate: Double,
        tenure: Int,
        dateStr: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val emi = FinanceUtils.calculateEMI(total, rate, tenure)
            val loan = Loan(
                lenderName = name.trim(),
                totalAmount = total,
                remainingPrincipal = total,
                interestRate = rate,
                tenureMonths = tenure,
                emiAmount = emi,
                startDateDdMmYyyy = dateStr
            )
            dao.insertLoan(loan)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteLoan(loan)
        }
    }

    fun convertCreditCardToEmi(
        cardId: Long,
        amount: Double,
        rate: Double,
        tenure: Int,
        dateStr: String,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = dao.getAccountById(cardId) ?: return@launch
            // Relieve the credit card outstanding by the converted amount
            card.currentBalance += amount
            dao.updateAccount(card)

            // Calculate EMI and insert new structured loan
            val emi = FinanceUtils.calculateEMI(amount, rate, tenure)
            val loanName = "${card.name} EMI (${tenure}mo)"
            val loanId = dao.insertLoan(
                Loan(
                    lenderName = loanName,
                    totalAmount = amount,
                    remainingPrincipal = amount,
                    interestRate = rate,
                    tenureMonths = tenure,
                    emiAmount = emi,
                    startDateDdMmYyyy = dateStr
                )
            )

            // Record transaction in ledger for complete audit trail
            val noteDescription = "Converted to ${tenure}-mo EMI @ ${rate}% p.a. (EMI: ${FinanceUtils.formatCurrency(emi)}/mo)${if (!notes.isNullOrBlank()) " • $notes" else ""}"
            dao.baseInsertTransaction(
                Transaction(
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    fromAccountId = cardId,
                    toAccountId = null,
                    linkedLoanId = loanId,
                    dateDdMmYyyy = dateStr,
                    timestamp = FinanceUtils.parseDateToEpoch(dateStr),
                    notes = noteDescription
                )
            )
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FinanceViewModel(context.applicationContext as Application) as T
                }
            }
    }
}
