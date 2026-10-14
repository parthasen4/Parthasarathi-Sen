package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction as RoomTransaction
import com.example.model.Account
import com.example.model.AccountType
import com.example.model.Category
import com.example.model.Investment
import com.example.model.Loan
import com.example.model.Transaction
import com.example.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun baseInsertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM investments ORDER BY id DESC")
    fun getAllInvestments(): Flow<List<Investment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment): Long

    @Delete
    suspend fun deleteInvestment(investment: Investment)

    @Query("SELECT * FROM loans ORDER BY id DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): Loan?

    // Atomic multi-account ledger updater
    @RoomTransaction
    suspend fun executeFinancialTransaction(tx: Transaction) {
        baseInsertTransaction(tx)

        when (tx.type) {
            TransactionType.INCOME -> {
                val acc = getAccountById(tx.fromAccountId)
                acc?.let {
                    it.currentBalance += tx.amount
                    updateAccount(it)
                }
            }
            TransactionType.EXPENSE -> {
                val acc = getAccountById(tx.fromAccountId)
                acc?.let {
                    if (it.type == AccountType.CREDIT_CARD) {
                        it.currentBalance -= tx.amount // outstanding goes up negatively
                    } else {
                        it.currentBalance -= tx.amount
                    }
                    updateAccount(it)
                }
            }
            TransactionType.TRANSFER, TransactionType.CARD_BILL_PAY -> {
                if (tx.toAccountId != null && tx.fromAccountId != tx.toAccountId) {
                    val fromAcc = getAccountById(tx.fromAccountId)
                    val toAcc = getAccountById(tx.toAccountId!!)

                    fromAcc?.let {
                        it.currentBalance -= tx.amount
                        updateAccount(it)
                    }
                    toAcc?.let {
                        it.currentBalance += tx.amount
                        updateAccount(it)
                    }
                }
            }
            TransactionType.LOAN_REPAY -> {
                val fromAcc = getAccountById(tx.fromAccountId)
                val loan = tx.linkedLoanId?.let { getLoanById(it) }

                fromAcc?.let {
                    it.currentBalance -= tx.amount
                    updateAccount(it)
                }
                loan?.let {
                    it.remainingPrincipal -= tx.amount
                    if (it.remainingPrincipal < 0) it.remainingPrincipal = 0.0
                    updateLoan(it)
                }
            }
        }
    }

    // Atomic ledger deletion & balance reversal
    @RoomTransaction
    suspend fun revertAndRemoveFinancialTransaction(tx: Transaction) {
        when (tx.type) {
            TransactionType.INCOME -> {
                val acc = getAccountById(tx.fromAccountId)
                acc?.let {
                    it.currentBalance -= tx.amount
                    updateAccount(it)
                }
            }
            TransactionType.EXPENSE -> {
                val acc = getAccountById(tx.fromAccountId)
                acc?.let {
                    it.currentBalance += tx.amount
                    updateAccount(it)
                }
            }
            TransactionType.TRANSFER, TransactionType.CARD_BILL_PAY -> {
                if (tx.toAccountId != null && tx.fromAccountId != tx.toAccountId) {
                    val fromAcc = getAccountById(tx.fromAccountId)
                    val toAcc = getAccountById(tx.toAccountId!!)

                    fromAcc?.let {
                        it.currentBalance += tx.amount
                        updateAccount(it)
                    }
                    toAcc?.let {
                        it.currentBalance -= tx.amount
                        updateAccount(it)
                    }
                }
            }
            TransactionType.LOAN_REPAY -> {
                val fromAcc = getAccountById(tx.fromAccountId)
                val loan = tx.linkedLoanId?.let { getLoanById(it) }

                fromAcc?.let {
                    it.currentBalance += tx.amount
                    updateAccount(it)
                }
                loan?.let {
                    it.remainingPrincipal += tx.amount
                    updateLoan(it)
                }
            }
        }
        deleteTransaction(tx)
    }
}
