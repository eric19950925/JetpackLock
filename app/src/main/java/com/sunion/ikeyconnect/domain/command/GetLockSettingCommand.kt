package com.sunion.ikeyconnect.domain.command

import android.util.Base64
import com.sunion.ikeyconnect.domain.blelock.BleCmdRepository
import com.sunion.ikeyconnect.domain.blelock.unSignedInt
import com.sunion.ikeyconnect.domain.exception.LockStatusException
import com.sunion.ikeyconnect.domain.model.LockConfig
import com.sunion.ikeyconnect.domain.model.LockOrientation
import com.sunion.ikeyconnect.domain.model.LockSetting
import com.sunion.ikeyconnect.domain.model.LockStatus.BATTERY_ALERT
import com.sunion.ikeyconnect.domain.model.LockStatus.BATTERY_GOOD
import com.sunion.ikeyconnect.domain.model.LockStatus.BATTERY_LOW
import com.sunion.ikeyconnect.domain.model.LockStatus.LOCKED
import com.sunion.ikeyconnect.domain.model.LockStatus.UNKNOWN
import com.sunion.ikeyconnect.domain.model.LockStatus.UNLOCKED
import com.sunion.ikeyconnect.domain.toHex
import timber.log.Timber
import java.nio.ByteBuffer

class GetLockSettingCommand(private val iKeyDataTransmission: BleCmdRepository) :
    IKeyCommand<Unit, LockSetting> {
    override val function: Int = 0xD6

    override fun create(key: String, data: Unit): ByteArray {
        return iKeyDataTransmission.createCommand(
            function = function,
            key = Base64.decode(key, Base64.DEFAULT)
        )
    }

    override fun parseResult(key: String, data: ByteArray): LockSetting {
        return resolveD6(
            aesKeyTwo = Base64.decode(key, Base64.DEFAULT),
            notification = data
        )
    }

    override fun match(key: String, data: ByteArray): Boolean {
        return iKeyDataTransmission.decrypt(
            Base64.decode(key, Base64.DEFAULT), data
        )?.let { decrypted ->
            if (decrypted.component3().unSignedInt() == 0xEF) {
                throw LockStatusException.AdminCodeNotSetException()
            } else decrypted.component3().unSignedInt() == 0xD6
        } ?: false
    }

    private fun resolveD6(aesKeyTwo: ByteArray, notification: ByteArray): LockSetting {
        return aesKeyTwo.let { keyTwo ->
            iKeyDataTransmission.decrypt(keyTwo, notification)?.let { decrypted ->
                Timber.d("[D6] decrypted: ${decrypted.toHex()}")
                if (decrypted.component3().unSignedInt() == 0xD6) {
                    decrypted.copyOfRange(4, 4 + decrypted.component4().unSignedInt()).let { bytes ->
                        val autoLockTime = if (bytes[BleCmdRepository.D6Features.AUTOLOCK_DELAY.byte].unSignedInt() !in 1..90) {
                            1
                        } else {
                            bytes[BleCmdRepository.D6Features.AUTOLOCK_DELAY.byte].unSignedInt()
                        }
                        Timber.d("autoLockTime from lock: $autoLockTime")
                        val lockSetting = LockSetting(
                            config = LockConfig(
                                orientation = when (bytes[BleCmdRepository.D6Features.LOCK_ORIENTATION.byte].unSignedInt()) {
                                    0xA0 -> LockOrientation.Right
                                    0xA1 -> LockOrientation.Left
                                    0xA2 -> LockOrientation.NotDetermined
                                    else -> throw LockStatusException.LockOrientationException()
                                },
                                isSoundOn = bytes[BleCmdRepository.D6Features.KEYPRESS_BEEP.byte].unSignedInt() == 0x01,
                                isVacationModeOn = bytes[BleCmdRepository.D6Features.VACATION_MODE.byte].unSignedInt() == 0x01,
                                isAutoLock = bytes[BleCmdRepository.D6Features.AUTOLOCK.byte].unSignedInt() == 0x01,
                                autoLockTime = autoLockTime,
                                isPreamble = bytes[BleCmdRepository.D6Features.PREAMBLE.byte].unSignedInt() == 0x01
                            ),
                            status = when (bytes[BleCmdRepository.D6Features.LOCK_STATUS.byte].unSignedInt()) {
                                0 -> UNLOCKED
                                1 -> LOCKED
                                else -> UNKNOWN
                            },
                            battery = bytes[BleCmdRepository.D6Features.BATTERY.byte].unSignedInt(),
                            batteryStatus = when (bytes[BleCmdRepository.D6Features.LOW_BATTERY.byte].unSignedInt()) {
                                0 -> BATTERY_GOOD
                                1 -> BATTERY_LOW
                                else -> BATTERY_ALERT
                            },
                            timestamp = Integer.reverseBytes(ByteBuffer.wrap(bytes.copyOfRange(
                                BleCmdRepository.D6Features.TIMESTAMP.byte, BleCmdRepository.D6Features.SIZE.byte)).int).toLong()
                        )
                        Timber.d("[D6] LockSetting: $lockSetting, lockSetting: ${lockSetting.timestamp}")
                        return lockSetting
                    }
                } else {
                    throw IllegalArgumentException("Return function byte is not [D6]")
                }
            }
        } ?: throw IllegalArgumentException("Error when decryption")
    }

}