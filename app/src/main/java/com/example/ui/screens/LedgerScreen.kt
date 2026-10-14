package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.TransactionRow
import com.example.util.FinanceUtils

@Composable
fun LedgerScreen(
    transactions: List<Transaction>,
    accounts: List<Account>,
    categories: List<Category>,
    loans: List<Loan>,
    selectedFilter: TransactionType?,
    searchQuery: String,
    onFilterSelect: (TransactionType?) -> Unit,
    onSearchChange: (String) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val accountsMap = accounts.associateBy { it.id }
    val categoriesMap = categories.associateBy { it.id }
    val loansMap = loans.associateBy { it.id }

    val filteredTransactions = transactions.filter { tx ->
        val matchesType = selectedFilter == null || tx.type == selectedFilter
        val catName = tx.categoryId?.let { categoriesMap[it]?.name } ?: ""
        val accName = accountsMap[tx.fromAccountId]?.name ?: ""
        val notes = tx.notes ?: ""
        val matchesQuery = searchQuery.isBlank() ||
                catName.contains(searchQuery, ignoreCase = true) ||
                accName.contains(searchQuery, ignoreCase = true) ||
                notes.contains(searchQuery, ignoreCase = true)

        matchesType && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ledger_screen")
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by category, account, or notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_transactions_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterSelect(null) },
                label = { Text("All") },
                modifier = Modifier.testTag("filter_all")
            )
            TransactionType.values().forEach { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { onFilterSelect(if (selectedFilter == type) null else type) },
                    label = {
                        Text(
                            when (type) {
                                TransactionType.EXPENSE -> "Expense"
                                TransactionType.INCOME -> "Income"
                                TransactionType.TRANSFER -> "Transfer"
                                TransactionType.CARD_BILL_PAY -> "Card Bill"
                                TransactionType.LOAN_REPAY -> "Loan Repay"
                            }
                        )
                    },
                    modifier = Modifier.testTag("filter_${type.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Count summary
        Text(
            text = "${filteredTransactions.size} transactions found",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching transactions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        accountsMap = accountsMap,
                        categoriesMap = categoriesMap,
                        loansMap = loansMap,
                        onDelete = { onDeleteTransaction(tx) }
                    )
                }
            }
        }
    }
}
