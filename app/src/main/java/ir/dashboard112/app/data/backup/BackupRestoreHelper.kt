package ir.dashboard112.app.data.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import ir.dashboard112.app.data.repository.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest

data class BackupOperationResult(
    val success: Boolean,
    val messageFa: String,
    val recordCount: Int = 0,
    val sha256Checksum: String = ""
)

/**
 * مدیریت پشتیبان‌گیری و بازیابی ایمن اطلاعات به حافظه محلی یا فضای ابری (Google Drive از طریق SAF اندروید)
 */
class BackupRestoreHelper(
    private val context: Context,
    private val repository: DashboardRepository
) {
    /**
     * بررسی وضعیت اتصال اینترنت دستگاه برای پشتیبان‌گیری ابری (Google Drive)
     */
    fun isInternetAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * نوشتن فایل پشتیبان JSON در مسیر انتخاب‌شده توسط کاربر (حافظه داخلی یا Google Drive)
     */
    suspend fun exportToUri(targetUri: Uri): BackupOperationResult = withContext(Dispatchers.IO) {
        val exportResult = repository.exportBackupJson()
        if (exportResult.isFailure) {
            return@withContext BackupOperationResult(
                success = false,
                messageFa = "خطا در آماده‌سازی داده‌های پشتیبان: ${exportResult.exceptionOrNull()?.message}"
            )
        }
        val jsonContent = exportResult.getOrThrow()
        val checksum = sha256Hex(jsonContent)

        runCatching {
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: error("امکان باز کردن مسیر خروجی وجود ندارد")
        }.fold(
            onSuccess = {
                BackupOperationResult(
                    success = true,
                    messageFa = "پشتیبان‌گیری با موفقیت انجام شد",
                    sha256Checksum = checksum
                )
            },
            onFailure = { err ->
                BackupOperationResult(
                    success = false,
                    messageFa = "خطا در ذخیره فایل پشتیبان: ${err.message}"
                )
            }
        )
    }

    /**
     * خواندن و بازیابی ایمن فایل پشتیبان JSON از مسیر انتخاب‌شده (حافظه داخلی یا Google Drive)
     */
    suspend fun importFromUri(
        sourceUri: Uri,
        replaceExisting: Boolean = true
    ): BackupOperationResult = withContext(Dispatchers.IO) {
        val rawJsonResult = runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: error("امکان خواندن فایل انتخاب‌شده وجود ندارد")
        }

        if (rawJsonResult.isFailure) {
            return@withContext BackupOperationResult(
                success = false,
                messageFa = "خطا در خواندن فایل پشتیبان: ${rawJsonResult.exceptionOrNull()?.message}"
            )
        }

        val rawJson = rawJsonResult.getOrThrow()
        val checksum = sha256Hex(rawJson)
        val importResult = repository.importBackupJson(rawJson, replaceExisting)

        importResult.fold(
            onSuccess = { count ->
                BackupOperationResult(
                    success = true,
                    messageFa = "بازیابی اطلاعات با موفقیت انجام شد ($count رکورد)",
                    recordCount = count,
                    sha256Checksum = checksum
                )
            },
            onFailure = { err ->
                BackupOperationResult(
                    success = false,
                    messageFa = "فایل پشتیبان نامعتبر یا ناسازگار است: ${err.message}"
                )
            }
        )
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
