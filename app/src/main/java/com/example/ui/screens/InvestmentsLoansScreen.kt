package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.InvestmentCard
import com.example.ui.components.LoanCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.InvestmentGold
import com.example.ui.theme.LoanPurple
import com.example.util.FinanceUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsLoansScreen(
    investments: List<Investment>,
    loans: List<Loan>,
    accounts: List<Account>,
    onAddInvestmentClick: () -> Unit,
    onAddLoanClick: () -> Unit,
    onRepayLoanClick: (Loan) -> Unit,
    onDeleteInvestment: (Investment) -> Unit,
    onDeleteLoan: (Loan) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val accountsMap = accounts.associateBy { it.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("investments_loans_screen")
    ) {
        // Tab Row
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Investments (${investments.size})", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tab_investments")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Loans (${loans.size})", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("tab_loans")
            )
        }

        if (selectedTab == 0) {
            // Investments Tab
            val totalPrincipal = investments.sumOf {
                if (it.type == InvestmentType.FD) it.principalAmount
                else (it.monthlyDeposit ?: it.principalAmount) * it.tenureMonths
            }
            val totalMaturity = investments.sumOf {
                if (it.type == InvestmentType.FD) {
                    FinanceUtils.calculateFDMaturity(it.principalAmount, it.interestRate, it.tenureMonths)
                } else {
                    FinanceUtils.calculateRDMaturity(it.monthlyDeposit ?: it.principalAmount, it.interestRate, it.tenureMonths)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Investment Overview Banner
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
                                Text("Total Invested", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = FinanceUtils.formatCurrency(totalPrincipal),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = InvestmentGold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Projected Maturity", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                                Text(
                                    text = FinanceUtils.formatCurrency(totalMaturity),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddInvestmentClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_investment_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Investment (FD / RD)")
                    }
                }

                if (investments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No investments recorded yet.\nAdd Fixed or Recurring Deposits to track returns!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(investments, key = { it.id }) { inv ->
                        InvestmentCard(
                            investment = inv,
                            linkedAccountName = accountsMap[inv.linkedAccountId]?.name,
                            onDelete = { onDeleteInvestment(inv) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        } else {
            // Loans Tab
            val totalRemainingDebt = loans.sumOf { it.remainingPrincipal }
            val totalMonthlyEmi = loans.sumOf { it.emiAmount }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Loans Overview Banner
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
                                Text("Outstanding Debt", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                                Text(
                                    text = FinanceUtils.formatCurrency(totalRemainingDebt),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Monthly EMI", style = MaterialTheme.typography.bodySmall, color = LoanPurple)
                                Text(
                                    text = FinanceUtils.formatCurrency(totalMonthlyEmi),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = LoanPurple
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddLoanClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_loan_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Loan / Liability")
                    }
                }

                if (loans.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active loans. You are debt-free!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(loans, key = { it.id }) { loan ->
                        LoanCard(
                            loan = loan,
                            onRepayClick = { onRepayLoanClick(loan) },
                            onDelete = { onDeleteLoan(loan) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}
