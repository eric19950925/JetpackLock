package com.sunion.ikeyconnect.domain.blelock

import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.sunion.ikeyconnect.domain.exception.ConnectionTokenException
import com.sunion.ikeyconnect.domain.model.DeviceToken
import com.sunion.ikeyconnect.domain.toHex
import com.sunion.ikeyconnect.domain.usecase.device.BleHandShakeUseCase.Companion.CIPHER_MODE
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class BleCmdRepository @Inject constructor(){

    private val commandSerial = AtomicInteger()

    private fun encrypt(key: ByteArray, data: ByteArray): ByteArray? {
        Timber.d("key:\n${key.toHex()}")
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted: ByteArray = cipher.doFinal(data)
            Timber.d("encrypted:\n${encrypted.toHex()}")
            encrypted
        } catch (exception: Exception) {
            Timber.d(exception)
            null
        }
    }

    fun decrypt(key: ByteArray, data: ByteArray): ByteArray? {
//        Timber.d("key:\n${key.toHex()}")
        return try {
            val cipher: Cipher = Cipher.getInstance(CIPHER_MODE)
            val keySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val original: ByteArray = cipher.doFinal(data)
//            Timber.d("decrypted: \n${original.toHex()}")
            original
        } catch (exception: Exception) {
            Timber.d(exception)
            null
        }
    }

    private fun pad(data: ByteArray, padZero: Boolean = false): ByteArray {
        if (data.isEmpty()) throw IllegalArgumentException("Invalid command.")
        val padNumber = 16 - (data.size) % 16
        val padBytes = if (padZero) ByteArray(padNumber) else Random.nextBytes(padNumber)
//        println(padBytes.toHex())
        return if (data.size % 16 == 0) {
            data
        } else {
            data + padBytes
        }
    }

    private fun serialIncrementAndGet(): ByteArray {
        val serial = commandSerial.incrementAndGet()
        val array = ByteArray(2)
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        byteBuffer.putInt(serial)
        byteBuffer.flip()
        byteBuffer.get(array)
        return array
    }

    // byte array equal to [E5] documentation
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun extractToken(byteArray: ByteArray): DeviceToken {
        return if (byteArray.component1().unSignedInt() == 0) {
            throw ConnectionTokenException.IllegalTokenException()
        } else {
            Timber.d("[E5]: ${byteArray.toHex()}")
            val isPermanentToken = byteArray.component2().unSignedInt() == 1
            val isOwnerToken = byteArray.component3().unSignedInt() == 1
            val permission = String(byteArray.copyOfRange(3, 4))
            val token = byteArray.copyOfRange(4, 12)
            if (isPermanentToken) {
                val name = String(byteArray.copyOfRange(12, byteArray.size))
                DeviceToken.PermanentToken(
                    Base64.encodeToString(token, Base64.DEFAULT),
                    isOwnerToken,
                    name,
                    permission
                )
            } else {
                DeviceToken.OneTimeToken(Base64.encodeToString(token, Base64.DEFAULT))
            }
        }
    }

    fun createCommand(
        function: Int,
        key: ByteArray,
        data: ByteArray = byteArrayOf()
    ): ByteArray {
//        Timber.d("create command: [${String.format("%2x", function)}]")
        return when (function) {
            0xC0 -> {
                commandSerial.set(0)
                c0(serialIncrementAndGet(), key)
            }
            0xC1 -> c1(serialIncrementAndGet(), key, data)
            0xC7 -> c7(serialIncrementAndGet(), key, data)
//            0xC8 -> c8(serialIncrementAndGet(), key, data)
//            0xCC -> cc(serialIncrementAndGet(), key)
            0xCE -> ce(serialIncrementAndGet(), key, data)
            0xF0 -> f0(serialIncrementAndGet(), key, data)
//            0xD0 -> d0(serialIncrementAndGet(), key)
//            0xD1 -> d1(serialIncrementAndGet(), key, data)
//            0xD2 -> d2(serialIncrementAndGet(), key)
//            0xD3 -> d3(serialIncrementAndGet(), key, data)
//            0xD4 -> d4(serialIncrementAndGet(), key)
//            0xD5 -> d5(serialIncrementAndGet(), key, data)
//            0xD6 -> d6(serialIncrementAndGet(), key)
//            0xD7 -> d7(serialIncrementAndGet(), key, data)
//            0xD8 -> d8(serialIncrementAndGet(), key)
//            0xD9 -> d9(serialIncrementAndGet(), key, data)
//            0xE0 -> e0(serialIncrementAndGet(), key)
//            0xE1 -> e1(serialIncrementAndGet(), key, data)
//            0xE4 -> e4(serialIncrementAndGet(), key)
//            0xE5 -> e5(serialIncrementAndGet(), key, data)
//            0xE6 -> e6(serialIncrementAndGet(), key, data)
//            0xE7 -> e7(serialIncrementAndGet(), key, data)
//            0xE8 -> e8(serialIncrementAndGet(), key, data)
//            0xEA -> ea(serialIncrementAndGet(), key)
//            0xEB -> eb(serialIncrementAndGet(), key, data)
//            0xEC -> ec(serialIncrementAndGet(), key, data)
//            0xED -> ed(serialIncrementAndGet(), key, data)
//            0xEE -> ee(serialIncrementAndGet(), key, data)
//            0xEF -> ef(serialIncrementAndGet(), key)
            else -> throw IllegalArgumentException("Unknown function")
        }
    }

    fun generateRandomBytes(size: Int): ByteArray = Random.nextBytes(size)

    /**
     * ByteArray [C0] data command, length 16 of random number.
     *
     * @return An encrypted byte array.
     * */
    fun c0(serial: ByteArray, aesKeyOne: ByteArray): ByteArray {
        if (serial.size != 2) throw IllegalArgumentException("Invalid serial")
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC0.toByte() // function
        sendByte[1] = 0x10 // len
        Timber.d("c0: ${(serial + sendByte).toHex()}")
        return encrypt(aesKeyOne, pad(serial + sendByte + generateRandomBytes(0x10)))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    /**
     * ByteArray [C1] data command. To retrieve the token state.
     *
     * @return An encoded byte array of [C1] command.
     * */
    fun c1(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        token: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xC1.toByte() // function
        sendByte[1] = 0x08 // len=8
//        Timber.d("c1: ${(serial + sendByte).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + token)) ?: throw IllegalArgumentException(
            "bytes cannot be null"
        )
    }
    fun c7(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(3)
        sendByte[0] = 0xC7.toByte() // function
        sendByte[1] = (code.size + 1).toByte() // len
        sendByte[2] = (code.size).toByte() // code size
//        Timber.d("c7: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }
    /**
     * ByteArray [CE] data command. Add admin code
     *
     * @return An encoded byte array of [CE] command.
     * */
    fun ce(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xCE.toByte() // function
        sendByte[1] = (code.size).toByte() // len
//        Timber.d("ce: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    fun f0(
        serial: ByteArray,
        aesKeyTwo: ByteArray,
        code: ByteArray
    ): ByteArray {
        val sendByte = ByteArray(2)
        sendByte[0] = 0xF0.toByte() // function
        sendByte[1] = (code.size).toByte() // len
        Timber.d("f0: ${(serial + sendByte + code).toHex()}")
        return encrypt(aesKeyTwo, pad(serial + sendByte + code))
            ?: throw IllegalArgumentException("bytes cannot be null")
    }

    fun resolveF0(aesKeyTwo: ByteArray, notification: ByteArray): String {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xF0) {
                String(decrypted.copyOfRange(2, decrypted.size - 1))
            } else {
                throw IllegalArgumentException("Return function byte is not [F0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

    fun resolveC0(keyOne: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(keyOne, notification)?.let { decrypted ->
//            Timber.d("[C0] decrypted: ${decrypted.toHex()}")
            if (decrypted.component3().unSignedInt() == 0xC0) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C0]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveC1(aesKeyTwo: ByteArray, notification: ByteArray): ByteArray {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
//            Timber.d("[C1] decrypted: ${decrypted.toHex()}")
            if (decrypted.component3().unSignedInt() == 0xC1) {
                return decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt())
            } else {
                throw IllegalArgumentException("Return function byte is not [C1]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveC7(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xC7) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C7]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveCE(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xCE) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [C8]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
    fun resolveD5(aesKeyTwo: ByteArray, notification: ByteArray): Boolean {
        return decrypt(aesKeyTwo, notification)?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xD5) {
                when {
                    decrypted.component5().unSignedInt() == 0x01 -> true
                    decrypted.component5().unSignedInt() == 0x00 -> false
                    else -> throw IllegalArgumentException("Unknown data")
                }
            } else {
                throw IllegalArgumentException("Return function byte is not [D5]")
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }


    fun resolveE5(notification: ByteArray): ByteArray {
        return if (notification.component3().unSignedInt() == 0xE5) {
            notification.copyOfRange(4, 4 + notification.component4().unSignedInt())
        } else {
            throw IllegalArgumentException("Return function byte is not [E5]")
        }
    }

    fun stringCodeToHex(code: String): ByteArray {
        return code.takeIf { it.isNotBlank() }
            ?.filter { it.isDigit() }
            ?.map { Character.getNumericValue(it).toByte() }
            ?.toByteArray()
            ?: throw IllegalArgumentException("Invalid user code string")
    }

    fun generateKeyTwoThen(
        randomNumberOne: ByteArray,
        randomNumberTwo: ByteArray,
        function: (ByteArray) -> Unit
    ) {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        function.invoke(keyTwo)
    }

    fun generateKeyTwo(randomNumberOne: ByteArray, randomNumberTwo: ByteArray): ByteArray {
        val keyTwo = ByteArray(16)
        for (i in 0..15) keyTwo[i] =
            ((randomNumberOne[i].unSignedInt()) xor (randomNumberTwo[i].unSignedInt())).toByte()
        return keyTwo
    }

    fun determineTokenPermission(data: ByteArray): String {
        return String(data.copyOfRange(1, 2))
    }


    fun determineTokenState(data: ByteArray, isLockFromSharing: Boolean): Int {
        return when (data.component1().unSignedInt()) {
            //0 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenException()
            1 -> Log.d("TAG","VALID_TOKEN")
//                DeviceToken.VALID_TOKEN
            // according to documentation, 2 -> the token has been swapped inside the device,
            // hence the one time token no longer valid to connect.
            //2 -> if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.DeviceRefusedException()
            3 -> Log.d("TAG","ONE_TIME_TOKEN")
//                DeviceToken.ONE_TIME_TOKEN
            // 0, and else
            else -> Log.d("TAG","IllegalTokenStateException") //if (isLockFromSharing) throw ConnectionTokenException.LockFromSharingHasBeenUsedException() else throw ConnectionTokenException.IllegalTokenStateException()
        }
    }

}

fun Byte.unSignedInt() = this.toInt() and 0xFF