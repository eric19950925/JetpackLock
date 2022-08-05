package com.sunion.ikeyconnect.domain

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository.Companion.BARCODE_KEY
import com.sunion.ikeyconnect.domain.model.QRCodeContent
import timber.log.Timber
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

internal object LockQRCodeParser {
    private val gson = Gson()

    fun parseWifiQRCodeContent(content: String): QRCodeContent {
        val data = Base64.decode(content.toByteArray(), Base64.DEFAULT)
        val decode = decryptV2(data, BARCODE_KEY.toByteArray())
        val decodeString = String(decode!!)
        Timber.d(decodeString)
        val qrCodeContent = gson.fromJson(decodeString, QRCodeContent::class.java)!!
        return qrCodeContent.copy(a = qrCodeContent.a.chunked(2).joinToString(":") { it })
    }

    fun parseQRCodeContent(content: String): QRCodeContent {
        val data = Base64.decode(content.toByteArray(), Base64.DEFAULT)
        val decode = decryptV1(data, BARCODE_KEY.toByteArray())
        val decodeString = String(decode!!)
//        Log.d("TAG",decodeString)
        Timber.d(decodeString)
        val qrCodeContent = gson.fromJson(decodeString, QRCodeContent::class.java)!!
        return qrCodeContent.copy(a = qrCodeContent.a.chunked(2).joinToString(":") { it })
    }

    @SuppressLint("GetInstance")
    private fun decryptV2(data: ByteArray, key: ByteArray): ByteArray? {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    @SuppressLint("GetInstance")
    private fun decryptV1(data: ByteArray, key: ByteArray): ByteArray? {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/ZeroBytePadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    @SuppressLint("GetInstance")
    fun encryptV2(data: ByteArray, key: ByteArray): ByteArray? {
        val cipher: Cipher = Cipher.getInstance("AES/ECB/ZeroBytePadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    fun pad(data: ByteArray, padZero: Boolean = false): ByteArray {
        if (data.isEmpty()) throw IllegalArgumentException("Invalid command.")
        val padNumber = 16 - (data.size) % 16
        val padBytes = if (padZero) ByteArray(padNumber) else Random.nextBytes(padNumber)
        println(padBytes.toHex())
        return if (data.size % 16 == 0) {
            data
        } else {
            data + padBytes
        }
    }

}

fun ByteArray.toHex(): String {
    return joinToString(", ") { "%02x".format(it).toUpperCase(Locale.getDefault()) }
}