package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType { BANK, CREDIT_CARD, CASH }
enum class CategoryType { INCOME, EXPENSE }
enum class TransactionType { INCOME, EXPENSE, TRANSFER, CARD_BILL_PAY, LOAN_REPAY }
enum class InvestmentType { FD, RD }

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var name: String,
    var type: AccountType,
    var currentBalance: Double,
    var creditLimit: Double? = null
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var name: String,
    val type: CategoryType
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var amount: Double,
    var type: TransactionType,
    var categoryId: Long?,
    var fromAccountId: Long,
    var toAccountId: Long? = null,
    var linkedLoanId: Long? = null,
    var dateDdMmYyyy: String,
    var timestamp: Long,
    var notes: String? = null
)

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var name: String,
    var type: InvestmentType,
    var principalAmount: Double,
    var monthlyDeposit: Double? = null,
    var interestRate: Double,
    var tenureMonths: Int,
    var startDateDdMmYyyy: String,
    var linkedAccountId: Long
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var lenderName: String,
    var totalAmount: Double,
    var remainingPrincipal: Double,
    var interestRate: Double,
    var tenureMonths: Int,
    var emiAmount: Double,
    var startDateDdMmYyyy: String
)
