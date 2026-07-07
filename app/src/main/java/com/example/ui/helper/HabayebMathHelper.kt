package com.example.ui.helper

import java.math.BigDecimal
import java.math.RoundingMode

object HabayebMathHelper {
    fun toBigDecimal(value: Double): BigDecimal {
        return try {
            BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            BigDecimal.ZERO.setScale(2)
        }
    }

    fun toBigDecimal(value: String): BigDecimal {
        return try {
            BigDecimal(value).setScale(2, RoundingMode.HALF_UP)
        } catch (e: Exception) {
            BigDecimal.ZERO.setScale(2)
        }
    }

    fun add(a: Double, b: Double): Double {
        return toBigDecimal(a).add(toBigDecimal(b)).toDouble()
    }

    fun subtract(a: Double, b: Double): Double {
        return toBigDecimal(a).subtract(toBigDecimal(b)).toDouble()
    }

    fun multiply(a: Double, b: Double): Double {
        return toBigDecimal(a).multiply(toBigDecimal(b)).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun divide(a: Double, b: Double): Double {
        if (b == 0.0) return 0.0
        return toBigDecimal(a).divide(toBigDecimal(b), 2, RoundingMode.HALF_UP).toDouble()
    }
    
    fun formatWithPrecision(value: Double): String {
        return toBigDecimal(value).toPlainString()
    }
}
