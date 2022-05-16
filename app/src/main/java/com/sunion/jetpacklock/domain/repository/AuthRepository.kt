package com.sunion.jetpacklock.domain.repository

import com.amazonaws.mobile.client.UserStateDetails
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun signIn(username: String, password: String): Flow<String>
    fun signOut(): Flow<Unit>
    fun getIdToken(): Flow<String>
    fun getStateDetails(): Flow<String>

    fun isSignedIn(): Flow<Boolean>
    fun requestForgotPassword(username: String): Flow<Unit>

    fun isUsernameExist(username: String): Flow<Boolean>

    /**
     * @return is confirmation
     */
    fun signUp(username: String, password: String): Flow<Boolean>

    /**
     * @return is confirmation
     */
    fun confirmSignUp(username: String, confirmCode: String): Flow<Boolean>

    fun resendSignUpConfirmCode(username: String): Flow<Unit>


    fun confirmForgotPassword(newPassword: String, confirmCode: String): Flow<Unit>

    fun getAccessToken(): Flow<String>

    fun getUsername(): String?

    fun getEmail(): Flow<String?>

    fun getName(): Flow<String?>

    fun setName(name: String): Flow<Unit>

    fun signOutAllDevice(): Flow<Unit>

    fun changePassword(oldPassword: String, newPassword: String): Flow<Unit>

}