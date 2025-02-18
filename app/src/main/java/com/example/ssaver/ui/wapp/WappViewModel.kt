package com.example.ssaver.ui.wapp

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

enum class StatusType {
    IMAGE, VIDEO
}

data class Status(
    val uri: Uri,
    val type: StatusType,
    val isDownloaded: Boolean = false
)

class WappViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "WappViewModel"
    private val _statuses = MutableLiveData<List<Status>>()
    val statuses: LiveData<List<Status>> = _statuses
    private val context = application.applicationContext

    private val whatsappPaths = mutableListOf<String>()
    private val savedStatusPath = "${Environment.getExternalStorageDirectory()}/Download/StatusSaver"

    init {
        Log.d(TAG, "Initializing WappViewModel")
        setupWhatsAppPaths()
        refreshStatuses()
    }

    private fun setupWhatsAppPaths() {
        val baseDir = Environment.getExternalStorageDirectory().absolutePath
        val pm = context.packageManager

        // Check if WhatsApp is installed
        val whatsappInstalled = try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        // Check if WhatsApp Business is installed
        val whatsappBusinessInstalled = try {
            pm.getPackageInfo("com.whatsapp.w4b", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        Log.d(TAG, "WhatsApp installed: $whatsappInstalled")
        Log.d(TAG, "WhatsApp Business installed: $whatsappBusinessInstalled")

        if (whatsappInstalled) {
            // Primary WhatsApp status paths
            whatsappPaths.addAll(listOf(
                // New WhatsApp status path (Android 11+)
                "$baseDir/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
                // Legacy WhatsApp status path
                "$baseDir/WhatsApp/Media/.Statuses"
            ))
        }

        if (whatsappBusinessInstalled) {
            // WhatsApp Business status paths
            whatsappPaths.addAll(listOf(
                // New WhatsApp Business status path (Android 11+)
                "$baseDir/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
                // Legacy WhatsApp Business status path
                "$baseDir/WhatsApp Business/Media/.Statuses"
            ))
        }

        // Log all paths and their accessibility
        whatsappPaths.forEach { path ->
            val file = File(path)
            Log.d(TAG, """
                Status Path: $path
                Exists: ${file.exists()}
                Is Directory: ${file.isDirectory}
                Can Read: ${file.canRead()}
                Parent exists: ${file.parentFile?.exists()}
                Parent readable: ${file.parentFile?.canRead()}
                Absolute path: ${file.absolutePath}
            """.trimIndent())
        }
    }

    fun refreshStatuses() {
        Log.d(TAG, "Refreshing statuses")
        viewModelScope.launch {
            try {
                val statusList = mutableListOf<Status>()
                
                withContext(Dispatchers.IO) {
                    // Create saved status directory if it doesn't exist
                    val savedDir = File(savedStatusPath)
                    if (!savedDir.exists()) {
                        val created = savedDir.mkdirs()
                        Log.d(TAG, "Created saved status directory: $created at path: $savedStatusPath")
                    }

                    // Try direct file access first as it's more reliable for current statuses
                    findStatusesViaDirectAccess(statusList)

                    // If no statuses found and we have MANAGE_EXTERNAL_STORAGE permission on Android 11+,
                    // try MediaStore as fallback
                    if (statusList.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                        findStatusesViaMediaStore(statusList)
                    }
                }

                Log.d(TAG, "Found ${statusList.size} total statuses. Images: ${statusList.count { it.type == StatusType.IMAGE }}, Videos: ${statusList.count { it.type == StatusType.VIDEO }}")
                _statuses.value = statusList
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing statuses", e)
                e.printStackTrace()
            }
        }
    }

    private fun findStatusesViaMediaStore(statusList: MutableList<Status>) {
        // Only try MediaStore if we have no statuses and running on Android 11 or higher with storage manager permission
        if (statusList.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_ADDED
            )

            // Only look for files in the .Statuses directory
            val selection = "${MediaStore.MediaColumns.DATA} LIKE ? AND ${MediaStore.MediaColumns.DATE_ADDED} >= ?"
            val twentyFourHoursAgo = (System.currentTimeMillis() / 1000) - (24 * 60 * 60)
            
            // Query for images in .Statuses directory
            whatsappPaths.forEach { statusPath ->
                val selectionArgs = arrayOf(
                    "$statusPath/%",
                    twentyFourHoursAgo.toString()
                )

                // Query for images
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        Log.d(TAG, "Found image status: $path")
                        statusList.add(Status(uri, StatusType.IMAGE, isStatusDownloaded(File(path).name)))
                    }
                }

                // Query for videos
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                        Log.d(TAG, "Found video status: $path")
                        statusList.add(Status(uri, StatusType.VIDEO, isStatusDownloaded(File(path).name)))
                    }
                }
            }
        }
    }

    private fun findStatusesViaDirectAccess(statusList: MutableList<Status>) {
        whatsappPaths.forEach { path ->
            val statusDir = File(path)
            Log.d(TAG, "Checking directory: $path")
            Log.d(TAG, "Directory exists: ${statusDir.exists()}, isDirectory: ${statusDir.isDirectory}, canRead: ${statusDir.canRead()}, absolutePath: ${statusDir.absolutePath}")
            
            if (statusDir.exists() && statusDir.canRead()) {
                statusDir.listFiles()?.let { files ->
                    Log.d(TAG, "Found ${files.size} files in $path")
                    files.forEach { file ->
                        try {
                            // Only include files that are less than 24 hours old
                            val lastModified = file.lastModified()
                            val isRecent = (System.currentTimeMillis() - lastModified) <= (24 * 60 * 60 * 1000)
                            
                            if (isRecent) {
                                Log.d(TAG, "Processing file: ${file.name}, size: ${file.length()}, canRead: ${file.canRead()}, absolutePath: ${file.absolutePath}")
                                val uri = Uri.fromFile(file)
                                when {
                                    file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") -> {
                                        Log.d(TAG, "Found image: ${file.name}")
                                        statusList.add(Status(uri, StatusType.IMAGE, isStatusDownloaded(file.name)))
                                    }
                                    file.name.endsWith(".mp4") -> {
                                        Log.d(TAG, "Found video: ${file.name}")
                                        statusList.add(Status(uri, StatusType.VIDEO, isStatusDownloaded(file.name)))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing file: ${file.name}", e)
                            e.printStackTrace()
                        }
                    }
                } ?: Log.e(TAG, "listFiles() returned null for $path")
            } else {
                Log.w(TAG, "Directory access issue - exists: ${statusDir.exists()}, canRead: ${statusDir.canRead()}, path: $path")
            }
        }
    }

    private fun isStatusDownloaded(fileName: String): Boolean {
        val file = File("$savedStatusPath/$fileName")
        val exists = file.exists()
        Log.d(TAG, "Checking if status is downloaded: $fileName, exists: $exists")
        return exists
    }

    fun downloadStatus(status: Status): Boolean {
        return try {
            Log.d(TAG, "Attempting to download status: ${status.uri}")
            val sourceFile = File(status.uri.path!!)
            val destFile = File(savedStatusPath, sourceFile.name)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Scan the file so it appears in gallery
            MediaScannerConnection.scanFile(
                getApplication(),
                arrayOf(destFile.toString()),
                null
            ) { path, uri -> 
                Log.d(TAG, "Media scan completed - Path: $path, URI: $uri")
            }

            // Refresh the list to update download marks
            refreshStatuses()
            
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error downloading status", e)
            false
        }
    }

    fun shareStatus(status: Status, context: Context) {
        try {
            Log.d(TAG, "Attempting to share status: ${status.uri}")
            val file = File(status.uri.path!!)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (status.type) {
                    StatusType.IMAGE -> "image/*"
                    StatusType.VIDEO -> "video/*"
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, uri)
            }

            Log.d(TAG, "Launching share intent")
            context.startActivity(Intent.createChooser(intent, null))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing status", e)
        }
    }
} 