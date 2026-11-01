package com.example.filemanager

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
) {
    val sizeFormatted: String
        get() = if (isDirectory) "—" else formatSize(size)

    val dateFormatted: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(lastModified))

    val icon: String
        get() = when {
            isDirectory -> "📁"
            name.endsWith(".txt") -> "📝"
            name.endsWith(".py") -> "🐍"
            name.endsWith(".json") -> "📦"
            name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") -> "🖼️"
            name.endsWith(".mp3") -> "🎵"
            name.endsWith(".mp4") -> "🎬"
            name.endsWith(".pdf") -> "📕"
            name.endsWith(".zip") -> "🗜️"
            name.endsWith(".apk") -> "📱"
            else -> "📄"
        }

    companion object {
        fun formatSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            var size = bytes.toDouble()
            val units = arrayOf("KB", "MB", "GB", "TB")
            var i = -1
            while (size >= 1024 && i < units.size - 1) {
                size /= 1024
                i++
            }
            return String.format("%.1f %s", size, units[i])
        }
    }
}
