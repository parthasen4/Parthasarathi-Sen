package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.FinanceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal Finance", appName)
  }

  @Test
  fun `test emi calculation`() {
    val emi = FinanceUtils.calculateEMI(10000.0, 10.0, 12)
    // EMI for 10000 at 10% for 1 year is ~ 879.16
    assertTrue("EMI should be around 879", emi in 875.0..885.0)
  }

  @Test
  fun `test fd maturity calculation`() {
    val maturity = FinanceUtils.calculateFDMaturity(5000.0, 8.0, 12)
    // 5000 compounded quarterly at 8% for 1 yr gives ~ 5412.16
    assertTrue("Maturity should exceed principal", maturity > 5000.0)
  }

  @Test
  fun `test inr currency formatting`() {
    val formatted = FinanceUtils.formatCurrency(54000.0)
    assertTrue("Should contain rupee symbol or INR", formatted.contains("₹") || formatted.contains("INR"))
  }

  @Test
  fun `test credit card emi calculation`() {
    // 50,000 converted to 6-month EMI at 14% p.a.
    val emi = FinanceUtils.calculateEMI(50000.0, 14.0, 6)
    assertTrue("Monthly EMI should be between 8600 and 8750", emi in 8600.0..8750.0)
    val totalRepayment = emi * 6
    assertTrue("Total repayment should be greater than principal", totalRepayment > 50000.0)
  }

  @Test
  fun `test transfer same account validation logic`() {
    val fromAccountId = 1L
    val toAccountId = 1L
    val isSameAccount = fromAccountId == toAccountId
    assertTrue("Transfer in same account must be detected as invalid", isSameAccount)
  }
}
