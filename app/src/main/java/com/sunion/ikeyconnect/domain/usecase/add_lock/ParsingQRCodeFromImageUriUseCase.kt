package com.sunion.ikeyconnect.domain.usecase.add_lock

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.sunion.ikeyconnect.utils.FileCompressor
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParsingQRCodeFromImageUriUseCase @Inject constructor(private val application: Application) {
    private val compressor = FileCompressor(application)

    operator fun invoke(imageUri: Uri): String? {
        var photo: File? = null

        getRealPathFromUri(imageUri)?.let { path ->
            val file = File(path)
            try {
                photo = compressor.compressToFile(file)
            } catch (error: Throwable) {
//                FirebaseCrashlytics.getInstance().recordException(error)
                Timber.d(error)
            }
        }

        return photo?.let {
            Timber.d("RESULT_OK, RC_GALLERY_PHOTO imageUri, photo $it")
            val uri = it.toUri()
            val inputStream = application.contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap == null) {
                Timber.e("uri is not a bitmap: $uri")
                return null
            }
            val width: Int = bitmap.width
            val height: Int = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.recycle()
            bitmap = null
            val source = RGBLuminanceSource(width, height, pixels)
            val bBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            val result = reader.decode(bBitmap)
            val resultText = result.text
            Timber.d("The content of the QR image is:  + $resultText")
            return resultText
        }
    }

    private fun getRealPathFromUri(contentUri: Uri?): String? {
        return contentUri?.let { uri ->
            val contentResolver = application.contentResolver ?: return null

            // Create file path inside app's data dir
            val filePath = (
                    application.applicationInfo.dataDir.toString() + File.separator +
                            System.currentTimeMillis()
                    )

            val file = File(filePath)
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return null
                val outputStream = FileOutputStream(file)
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
                outputStream.close()
                inputStream.close()
            } catch (exception: IOException) {
//                FirebaseCrashlytics.getInstance().recordException(exception)
                Timber.e(exception)
                return null
            } catch (error: Throwable) {
//                FirebaseCrashlytics.getInstance().recordException(error)
                Timber.e(error)
                return null
            }

            file.absolutePath
        }
    }
}