package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

object FinanceUtils {

    fun parseDateToEpoch(dateStr: String): Long {
        val clean = dateStr.trim().replace("/", "").replace("-", "").replace(".", "")
        return try {
            if (clean.length == 8) {
                val sdf = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).apply { isLenient = false }
                sdf.parse(clean)?.time ?: System.currentTimeMillis()
            } else {
                val altSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
                altSdf.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun getTodayDateFormatted(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getTodayDdMmYyyy(): String {
        val sdf = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatEpochToDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDdMmYyyyToDisplay(dateStr: String): String {
        val clean = dateStr.trim().replace("/", "").replace("-", "")
        if (clean.length == 8) {
            val day = clean.substring(0, 2)
            val month = clean.substring(2, 4)
            val year = clean.substring(4, 8)
            return "$day/$month/$year"
        }
        return dateStr
    }

    fun calculateEMI(principal: Double, annualRate: Double, tenureMonths: Int): Double {
        if (tenureMonths <= 0 || principal <= 0.0) return 0.0
        val monthlyRate = (annualRate / 12.0) / 100.0
        return if (monthlyRate <= 0.0) {
            principal / tenureMonths
        } else {
            val numerator = principal * monthlyRate * (1 + monthlyRate).pow(tenureMonths.toDouble())
            val denominator = (1 + monthlyRate).pow(tenureMonths.toDouble()) - 1
            if (denominator != 0.0) numerator / denominator else principal / tenureMonths
        }
    }

    /**
     * Fixed Deposit Maturity with quarterly compounding:
     * A = P * (1 + r/(4*100))^(4 * (months/12))
     */
    fun calculateFDMaturity(principal: Double, annualRate: Double, tenureMonths: Int): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return principal
        val years = tenureMonths / 12.0
        val n = 4.0 // quarterly compounding
        val rateFraction = (annualRate / 100.0) / n
        return principal * (1.0 + rateFraction).pow(n * years)
    }

    /**
     * Recurring Deposit Maturity:
     * Maturity = P * n + Interest accrued on monthly deposits
     */
    fun calculateRDMaturity(monthlyDeposit: Double, annualRate: Double, tenureMonths: Int): Double {
        if (monthlyDeposit <= 0.0 || tenureMonths <= 0) return 0.0
        val n = tenureMonths.toDouble()
        val totalInvested = monthlyDeposit * n
        val interest = monthlyDeposit * (n * (n + 1.0) / 24.0) * (annualRate / 100.0)
        return totalInvested + interest
    }

    /**
     * Formats amount in Indian Rupee (INR) currency format with ₹ symbol
     */
    fun formatCurrency(amount: Double): String {
        return try {
            val isNegative = amount < 0
            val absAmount = abs(amount)
            val locale = Locale("en", "IN")
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = Currency.getInstance("INR")
            formatter.maximumFractionDigits = 2
            formatter.minimumFractionDigits = 2
            var formatted = formatter.format(absAmount)
            if (!formatted.contains("₹")) {
                formatted = formatted.replace("INR", "₹").replace("Rs.", "₹").replace("Rs", "₹").trim()
            }
            if (!formatted.startsWith("₹")) {
                formatted = "₹" + formatted.replace("₹", "").trim()
            }
            if (isNegative) "-$formatted" else formatted
        } catch (e: Exception) {
            val isNegative = amount < 0
            val absAmt = abs(amount)
            val prefix = if (isNegative) "-₹" else "₹"
            String.format(Locale("en", "IN"), "%s%,.2f", prefix, absAmt)
        }
    }
}
