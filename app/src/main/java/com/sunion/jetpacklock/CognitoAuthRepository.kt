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
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CognitoAuthRepository @Inject constructor(
    private val mobileClient: AWSMobileClient,
    private val dispatcher: CoroutineDispatcher
): AuthRepository {

    companion object {
        private const val USER_ATTRIBUTE_EMAIL = "email"
        private const val USER_ATTRIBUTE_NAME = "name"
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun signIn(username: String, password: String) = callbackFlow {
        val callback = object : Callback<SignInResult> {
            override fun onResult(result: SignInResult?) {
                result ?: cancel(CancellationException("SignIn Error", Exception("No result")))
                if (result?.signInState == SignInState.DONE){
                    trySend("success")
                }
                else {
                    val message = "state != SignInState.DONE"
                    cancel(message)
                }
                close()
            }

            override fun onError(e: Exception?) {
                val message = if (e is AmazonServiceException) e.errorMessage
                else "SignIn Error"
                cancel(message, /*mappingException(e)*/e)
            }
        }
        mobileClient.signIn(username.trim(), password, null, callback)
        awaitClose()
    }.catch { e -> throw e /*e.unwrapFromFlow()*/ }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun signOut() = callbackFlow {
        val callback = object : Callback<Void> {
            override fun onResult(result: Void?) {
                trySend(Unit)
                close()
            }

            override fun onError(e: java.lang.Exception?) {
                val message = if (e is AmazonServiceException) e.errorMessage
                else "logout Error"
                cancel(message, /*mappingException(e)*/e)
            }
        }
        mobileClient.signOut(SignOutOptions.builder().build(), callback)
        awaitClose()
    }

    override fun getIdToken(): Flow<String> = flow {
        Log.d("TAG",mobileClient.tokens.idToken.tokenString)
        emit(mobileClient.tokens.idToken.tokenString)
    }

    override fun getStateDetails(): Flow<String> = flow {
        emit(mobileClient.currentUserState().userState.name)
    }.flowOn(dispatcher)

    override fun isSignedIn(): Flow<Boolean> = flow {
        emit(mobileClient.isSignedIn)
    }.flowOn(dispatcher)


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun requestForgotPassword(username: String) = callbackFlow {
        val callback = object : Callback<ForgotPasswordResult> {
            override fun onResult(result: ForgotPasswordResult?) {
                result ?: cancel(
                    CancellationException("RequestForgotPassword Error", Exception("No result"))
                )
                if (result!!.state == ForgotPasswordState.CONFIRMATION_CODE) {
                    trySend(Unit)
                } else {
                    val message = "state != ForgotPasswordState.CONFIRMATION_CODE"
                    Log.d("TAG","$message ,state:${result.state}")
                    cancel(message)
                }
                close()
            }

            override fun onError(e: java.lang.Exception?) {
                val message = if (e is AmazonServiceException) e.errorMessage
                else "RequestForgotPassword Error"
                cancel(message, /*mappingException(e)*/e)
            }
        }
        mobileClient.forgotPassword(username.trim(), callback)
        awaitClose()
    }.catch { e -> throw e /*e.unwrapFromFlow()*/ }

    private var cachedName = ""
    private var cachedEmail = ""

    override fun isUsernameExist(username: String) = flow {
        emit(false)
    }.flowOn(dispatcher)

    override fun signUp(username: String, password: String) = flow {
        val value = runCatching {
            mobileClient.signUp(
                username.trim(),
                password,
                mapOf(),
                mapOf()
            ).confirmationState
        }.getOrElse { throw mappingException(it) }
        emit(value)
    }.flowOn(dispatcher)

//    @OptIn(ExperimentalCoroutinesApi::class)
//    override fun signUp(username: String, password: String) = callbackFlow {
//        val callback = object : Callback<SignUpResult> {
//            override fun onResult(result: SignUpResult?) {
//                result ?: cancel(CancellationException("SignIn Error", Exception("No result")))
//                trySend(result!!.confirmationState)
//                close()
//            }
//
//            override fun onError(e: Exception?) {
//                val message = if (e is AmazonServiceException) e.errorMessage
//                else "SignUp Error"
//                cancel(message, /*mappingException(e)*/e)
//            }
//        }
//        mobileClient.signUp(username.trim(), password, mapOf("username" to username), mapOf(), callback)
//        awaitClose()
//    }.catch { e -> throw e /*e.unwrapFromFlow()*/ }

    override fun confirmSignUp(username: String, confirmCode: String) = flow {
        val value = runCatching {
            mobileClient.confirmSignUp(username.trim(), confirmCode).confirmationState
        }.getOrElse { throw mappingException(it) }
        emit(value)
    }.flowOn(dispatcher)

    override fun resendSignUpConfirmCode(username: String) = flow {
        mobileClient.resendSignUp(username.trim())
        emit(Unit)
    }.flowOn(dispatcher)



    override fun confirmForgotPassword(newPassword: String, confirmCode: String) = flow {
        val result = runCatching { mobileClient.confirmForgotPassword(newPassword, confirmCode) }
            .getOrElse { throw mappingException(it) }
        if (result.state == ForgotPasswordState.DONE) {
            emit(Unit)
        } else {
            val message = "state != ForgotPasswordState.DONE"
            Log.e("TAG","$message ,state:${result.state}")
            throw IKeyException(message)
        }
    }.flowOn(dispatcher)

    override fun getAccessToken(): Flow<String> = flow {
        val value = runCatching { mobileClient.tokens.accessToken.tokenString }
            .getOrElse { throw mappingException(it) }
        emit(value)
    }.flowOn(dispatcher)

    override fun getUsername(): String? = runCatching { mobileClient.username }.getOrNull()

    override fun getEmail() = flow {
        if (cachedEmail.isNotEmpty()) {
            emit(cachedEmail)
            return@flow
        }
        val userAttributes = getUserAttributes().single()
        val value = userAttributes[USER_ATTRIBUTE_EMAIL]
        value?.let { cachedEmail = it }
        emit(value)
    }.flowOn(dispatcher)

    override fun getName() = flow {
        if (cachedName.isNotEmpty()) {
            emit(cachedName)
            return@flow
        }
        val userAttributes = getUserAttributes().single()
        val value = userAttributes[USER_ATTRIBUTE_NAME]
        value?.let { cachedName = it }
        emit(value)
    }.flowOn(dispatcher)

    override fun setName(name: String): Flow<Unit> = flow {
        cachedName = name
        mobileClient.updateUserAttributes(mapOf(USER_ATTRIBUTE_NAME to name), mapOf())
        emit(Unit)
    }.flowOn(dispatcher)

    override fun signOutAllDevice() = flow {
        val signOutOptions = SignOutOptions.builder().signOutGlobally(true).build()
        mobileClient.signOut(signOutOptions)
        emit(Unit)
    }.flowOn(dispatcher)

    override fun changePassword(oldPassword: String, newPassword: String) = flow {
        mobileClient.changePassword(oldPassword, newPassword)
        emit(Unit)
    }.flowOn(dispatcher)

    private fun getUserAttributes(): Flow<Map<String, String>> = flow {
        val value = runCatching { mobileClient.userAttributes }
            .getOrElse {
               throw mappingException(it)
            }
        emit(value)
    }.flowOn(dispatcher)

    private fun mappingException(exception: Throwable): Throwable =
        when (exception) {
            is AmazonServiceException -> {
                when (exception) {
                    is UsernameExistsException -> com.sunion.jetpacklock.domain.exception.UsernameExistsException()
                    is UserNotFoundException -> com.sunion.jetpacklock.domain.exception.UserNotFoundException()
                    else -> AuthException(exception.errorMessage)
                }
            }
            else -> exception
        }
}