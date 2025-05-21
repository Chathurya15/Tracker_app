package com.example.mad

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.lang.Exception

class BackupRestoreHelper(private val context: Context) {
    private val gson = Gson()
    private val backupFileName = "finance_tracker_backup.json"
    private val backupFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        backupFileName
    )

    private fun getBackupFile(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), backupFileName)
        } else {
            @Suppress("DEPRECATION")
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                backupFileName)
        }
    }

    fun exportData(transactions: List<Transaction>, callback: (Boolean, String) -> Unit) {
        try {
            val jsonString = gson.toJson(transactions)
            val file = getBackupFile()

            file.parentFile?.mkdirs()
            FileOutputStream(file).use { fos ->
                fos.write(jsonString.toByteArray())
            }
            callback(true, "Backup successful")
        } catch (e: Exception) {
            Log.e("BackupRestoreHelper", "Export failed", e)
            callback(false, "Backup failed: ${e.localizedMessage}")
        }
    }

    fun importData(callback: (List<Transaction>?, String) -> Unit) {
        try {
            val file = getBackupFile()
            if (!file.exists()) {
                callback(null, "Backup file not found")
                return
            }

            val jsonString = file.readText()
            if (jsonString.isBlank()) {
                callback(null, "Empty backup file")
                return
            }

            val type = object : TypeToken<List<Transaction>>() {}.type
            val transactions = gson.fromJson<List<Transaction>>(jsonString, type)
            transactions?.let {
                callback(it, "Successfully imported ${it.size} transactions")
            } ?: callback(null, "Failed to parse backup file")
        } catch (e: Exception) {
            Log.e("BackupRestore", "Import failed", e)
            callback(null, "Import failed: ${e.localizedMessage}")
        }
    }
}
