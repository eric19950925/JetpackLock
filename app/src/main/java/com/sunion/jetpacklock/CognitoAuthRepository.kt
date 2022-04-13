package com.sunion.jetpacklock

import com.amazonaws.AmazonServiceException
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.SignOutOptions
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CognitoAuthRepository @Inject constructor(
    private val mobileClient: AWSMobileClient
): AuthRepository {
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
}