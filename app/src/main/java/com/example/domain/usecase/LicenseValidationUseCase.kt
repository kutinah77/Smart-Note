package com.example.domain.usecase

import com.example.domain.LicenseManager

class LicenseValidationUseCase {

    fun verifyActivation(deviceId: String, code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        return LicenseManager.verifyActivationCode(deviceId, cleanCode)
    }

    fun isTrialExpired(transactionCount: Int, isActivated: Boolean): Boolean {
        if (isActivated) return false
        val cap = LicenseManager.getSecureLimitVal()
        return transactionCount >= cap
    }

    fun isPermanentCode(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        return cleanCode.startsWith(LicenseManager.getPrefixPerm())
    }

    fun getSecureLimit(): Int {
        return LicenseManager.getSecureLimitVal()
    }
}
