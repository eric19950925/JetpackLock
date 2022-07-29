package com.sunion.ikeyconnect.domain.usecase.device

import org.passay.*
import javax.inject.Inject

class VerifyAccessCodeStrengthUseCase @Inject constructor() {
    /**
     * @return pass or not
     */
    operator fun invoke(password: String): Boolean {
        val passwordValidator = PasswordValidator(
            LengthRule(4, 8),
            CharacterRule(EnglishCharacterData.Digit)
        )
        return passwordValidator.validate(PasswordData(password)).isValid
    }
}