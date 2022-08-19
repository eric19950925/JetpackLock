package com.sunion.ikeyconnect.domain.command

import android.util.Base64
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import com.sunion.ikeyconnect.domain.model.LockConfig
import com.sunion.ikeyconnect.domain.model.LockOrientation
import com.sunion.ikeyconnect.domain.toHex
import timber.log.Timber
import java.nio.ByteBuffer

class GetLockConfigCommand(private val iKeyDataTransmission: BleCmdRepository) :
    IKeyCommand<Unit, LockConfig> {
    override val function: Int = 0xD4

    override fun create(key: String, data: Unit): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT)
        )
    }

    override fun parseResult(key: String, data: ByteArray): LockConfig {
        return resolveD4(
            Base64.decode(key, Base64.DEFAULT),
            data
        )
    }

    override fun match(key: String, data: ByteArray): Boolean {
        return iKeyDataTransmission.decrypt(
            Base64.decode(key, Base64.DEFAULT), data
        )?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == 0xD4
        } ?: false
    }

    fun resolveD4(aesKeyTwo: ByteArray, notification: ByteArray): LockConfig {
        return aesKeyTwo.let { keyTwo ->
            iKeyDataTransmission.decrypt(keyTwo, notification)?.let { decrypted ->
                if (decrypted.component3().unSignedInt() == 0xD4) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        Timber.d("[D4] ${bytes.toHex()}")

                        val latIntPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(
                            BleCmdRepository.Config.LATITUDE_INTEGER.byte, BleCmdRepository.Config.LATITUDE_DECIMAL.byte)).int)
                        Timber.d("latIntPart: $latIntPart")
                        val latDecimalPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(
                            BleCmdRepository.Config.LATITUDE_DECIMAL.byte, BleCmdRepository.Config.LONGITUDE_INTEGER.byte)).int)
                        val latDecimal = latDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("latDecimalPart: $latIntPart, latDecimal: $latDecimal")
                        val lat = latIntPart.toBigDecimal().plus(latDecimal)
                        Timber.d("lat: $lat, ${lat.toPlainString()}")

                        val lngIntPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(
                            BleCmdRepository.Config.LONGITUDE_INTEGER.byte, BleCmdRepository.Config.LONGITUDE_DECIMAL.byte)).int)
                        Timber.d("lngIntPart: $lngIntPart")
                        val lngDecimalPart = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(
                            BleCmdRepository.Config.LONGITUDE_DECIMAL.byte, BleCmdRepository.Config.SIZE.byte)).int)
                        val lngDecimal = lngDecimalPart.toBigDecimal().movePointLeft(9)
                        Timber.d("lngIntPart: $lngIntPart, lngDecimal: $lngDecimal")
                        val lng = lngIntPart.toBigDecimal().plus(lngDecimal)
                        Timber.d("lng: $lng, ${lng.toPlainString()}")

                        val lockConfig = LockConfig(
                            orientation = when (bytes[BleCmdRepository.Config.LOCK_ORIENTATION.byte].unSignedInt()) {
                                0xA0 -> LockOrientation.Right
                                0xA1 -> LockOrientation.Left
                                0xA2 -> LockOrientation.NotDetermined
                                else -> throw LockStatusException.LockOrientationException()
                            },
                            isSoundOn = bytes[BleCmdRepository.Config.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                            isVacationModeOn = bytes[BleCmdRepository.Config.VACATION_MODE.byte].unSignedInt() == 0x01,
                            isAutoLock = bytes[BleCmdRepository.Config.AUTOLOCK.byte].unSignedInt() == 0x01,
                            autoLockTime = bytes[BleCmdRepository.Config.AUTOLOCK_DELAY.byte].unSignedInt(),
                            isPreamble = bytes[BleCmdRepository.Config.PREAMBLE.byte].unSignedInt() == 0x01,
                            latitude = lat.toDouble(),
                            longitude = lng.toDouble()
                        )
                        Timber.d("[D4] lockConfig: $lockConfig")
                        return lockConfig
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [D4]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }
}