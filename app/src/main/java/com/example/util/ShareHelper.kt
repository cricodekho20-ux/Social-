package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun shareVideo(context: Context, uriString: String?, filePath: String?, title: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Shared via Social Video Saver — $title")

                val uri = when {
                    uriString != null && uriString.startsWith("content://") -> Uri.parse(uriString)
                    filePath != null -> {
                        val file = File(filePath)
                        if (file.exists()) {
                            try {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                            } catch (_: Exception) {
                                Uri.fromFile(file)
                            }
                        } else null
                    }
                    else -> null
                }

                if (uri != null) {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(sendIntent, "Share Video via...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWithSystemPlayer(context: Context, uriString: String?, filePath: String?) {
        try {
            val uri = when {
                uriString != null && uriString.startsWith("content://") -> Uri.parse(uriString)
                filePath != null -> Uri.fromFile(File(filePath))
                else -> null
            }

            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Play Video with..."))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
        }
    }
}
