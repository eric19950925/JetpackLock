package com.sunion.jetpacklock

import android.util.Log
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.SignOutOptions
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.*
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException
import com.sunion.jetpacklock.domain.exception.AuthException
import com.sunion.jetpacklock.domain.exception.IKeyException
import com.sunion.jetpacklock.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
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

    override fun getAccessToken(): Flow<String> {
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