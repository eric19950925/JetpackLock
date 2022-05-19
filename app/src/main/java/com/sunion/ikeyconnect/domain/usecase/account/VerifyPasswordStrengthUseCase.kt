package com.sunion.ikeyconnect.domain.usecase.account

import org.passay.*
import javax.inject.Inject

class VerifyPasswordStrengthUseCase @Inject constructor() {
    /**
     * @return pass or not
     */
    operator fun invoke(password: String): Boolean {
        val passwordValidator = PasswordValidator(
            LengthRule(8, 15),
            CharacterRule(EnglishCharacterData.UpperCase, 1),
            CharacterRule(EnglishCharacterData.LowerCase, 1),
            CharacterRule(EnglishCharacterData.Digit)
        )
        return passwordValidator.validate(PasswordData(password)).isValid
    }
}