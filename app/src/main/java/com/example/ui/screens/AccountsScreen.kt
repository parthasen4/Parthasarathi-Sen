package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Account
import com.example.model.AccountType
import com.example.ui.components.AccountCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.FinanceUtils
import kotlin.math.abs

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onAddAccountClick: () -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onConvertToEmi: (Account) -> Unit,
    modifier: Modifier = Modifier
) {
    val bankAccounts = accounts.filter { it.type == AccountType.BANK }
    val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }
    val cashWallets = accounts.filter { it.type == AccountType.CASH }

    val totalBankAndCash = (bankAccounts + cashWallets).sumOf { it.currentBalance }
    val totalCreditDebt = creditCards.sumOf { if (it.currentBalance < 0) abs(it.currentBalance) else 0.0 }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("accounts_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Liquid Funds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinanceUtils.formatCurrency(totalBankAndCash),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Credit Outstanding",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinanceUtils.formatCurrency(totalCreditDebt),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }

        // Add Account Button
        item {
            Button(
                onClick = onAddAccountClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_account_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Account")
            }
        }

        // Bank Accounts Section
        if (bankAccounts.isNotEmpty()) {
            item {
                Text(
                    text = "Bank Accounts (${bankAccounts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(bankAccounts, key = { it.id }) { acc ->
                AccountCard(
                    account = acc,
                    onDelete = { onDeleteAccount(acc) }
                )
            }
        }

        // Credit Cards Section
        if (creditCards.isNotEmpty()) {
            item {
                Text(
                    text = "Credit Cards (${creditCards.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(creditCards, key = { it.id }) { acc ->
                AccountCard(
                    account = acc,
                    onDelete = { onDeleteAccount(acc) },
                    onConvertToEmi = { onConvertToEmi(acc) }
                )
            }
        }

        // Cash Wallets Section
        if (cashWallets.isNotEmpty()) {
            item {
                Text(
                    text = "Cash Wallets (${cashWallets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(cashWallets, key = { it.id }) { acc ->
                AccountCard(
                    account = acc,
                    onDelete = { onDeleteAccount(acc) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
