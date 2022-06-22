package com.sunion.ikeyconnect

import com.amazonaws.mobile.client.AWSMobileClient
import com.sunion.ikeyconnect.domain.Interface.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CognitoAmplifyRepository @Inject constructor(
    private val mobileClient: AWSMobileClient, // change to amplify
    private val dispatcher: CoroutineDispatcher
): AuthRepository {
    override fun signIn(username: String, password: String): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun signOut(): Flow<Unit> {
        TODO("Not yet implemented")
    }

    override fun getIdToken(): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun getIdentityId(): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun getStateDetails(): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun isSignedIn(): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun requestForgotPassword(username: String): Flow<Unit> {
        TODO("Not yet implemented")
    }

    override fun isUsernameExist(username: String): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun signUp(username: String, password: String): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun confirmSignUp(username: String, confirmCode: String): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun resendSignUpConfirmCode(username: String): Flow<Unit> {
        TODO("Not yet implemented")
    }

    override fun confirmForgotPassword(newPassword: String, confirmCode: String): Flow<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getAccessToken(): String {
        TODO("Not yet implemented")
    }

    override fun getUsername(): String? {
        TODO("Not yet implemented")
    }

    override fun getEmail(): Flow<String?> {
        TODO("Not yet implemented")
    }

    override fun getName(): Flow<String?> {
        TODO("Not yet implemented")
    }

    override fun setName(name: String): Flow<Unit> {
        TODO("Not yet implemented")
    }

    override fun signOutAllDevice(): Flow<Unit> {
        TODO("Not yet implemented")
    }

    override fun changePassword(oldPassword: String, newPassword: String): Flow<Unit> {
        TODO("Not yet implemented")
    }


}