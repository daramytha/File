package com.example.filemanager

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ClipboardState(val file: File? = null, val isCut: Boolean = false)

class FileManagerViewModel(app: Application) : AndroidViewModel(app) {

    private val _currentPath = MutableStateFlow(File("/storage/emulated/0"))
    val currentPath: StateFlow<File> = _currentPath

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    private val _clipboard = MutableStateFlow(ClipboardState())
    val clipboard: StateFlow<ClipboardState> = _clipboard

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _history = mutableListOf<File>()
    private var historyIndex = -1

    init {
        loadDirectory(_currentPath.value)
    }

    fun loadDirectory(dir: File, addToHistory: Boolean = true) {
        if (!dir.exists() || !dir.isDirectory) {
            _message.value = "Folder tidak dapat diakses"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            val items = withContext(Dispatchers.IO) {
                dir.listFiles()?.map { f ->
                    FileItem(f, f.name, f.isDirectory, f.length(), f.lastModified())
                }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?: emptyList()
            }
            _currentPath.value = dir
            _files.value = items
            if (addToHistory) {
                if (historyIndex < _history.size - 1) {
                    _history.subList(historyIndex + 1, _history.size).clear()
                }
                if (_history.lastOrNull() != dir) {
                    _history.add(dir)
                    historyIndex = _history.size - 1
                }
            }
            _loading.value = false
        }
    }

    fun goUp() {
        val parent = _currentPath.value.parentFile
        if (parent != null && parent.canRead()) loadDirectory(parent)
    }

    fun goBack() {
        if (historyIndex > 0) {
            historyIndex--
            loadDirectory(_history[historyIndex], addToHistory = false)
        }
    }

    fun refresh() = loadDirectory(_currentPath.value, addToHistory = false)

    fun copyFile(file: File) { _clipboard.value = ClipboardState(file, isCut = false) }
    fun cutFile(file: File) { _clipboard.value = ClipboardState(file, isCut = true) }

    fun paste() {
        val clip = _clipboard.value.file ?: return
        val target = File(_currentPath.value, clip.name)
        if (!clip.exists()) {
            _message.value = "File sumber tidak ada"
            _clipboard.value = ClipboardState()
            return
        }
        if (target.exists()) {
            _message.value = "File '${clip.name}' sudah ada"
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    if (_clipboard.value.isCut) {
                        if (!clip.renameTo(target)) {
                            clip.copyTo(target, overwrite = true)
                            clip.deleteRecursively()
                        }
                        "Dipindahkan: ${clip.name}"
                    } else {
                        clip.copyRecursively(target, overwrite = true)
                        "Dicopy: ${clip.name}"
                    }
                } catch (e: Exception) {
                    "Gagal: ${e.message}"
                }
            }
            if (_clipboard.value.isCut) _clipboard.value = ClipboardState()
            _message.value = result
            refresh()
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                file.deleteRecursively()
            }
            _message.value = if (ok) "Dihapus: ${file.name}" else "Gagal menghapus"
            refresh()
        }
    }

    fun renameFile(file: File, newName: String) {
        if (newName.isBlank() || newName == file.name) return
        val target = File(file.parentFile, newName)
        if (target.exists()) {
            _message.value = "Nama sudah dipakai"
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { file.renameTo(target) }
            _message.value = if (ok) "Diubah: $newName" else "Gagal rename"
            refresh()
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        val dir = File(_currentPath.value, name)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { dir.mkdir() }
            _message.value = if (ok) "Folder dibuat: $name" else "Gagal buat folder"
            refresh()
        }
    }

    fun createFile(name: String) {
        if (name.isBlank()) return
        val f = File(_currentPath.value, name)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { f.createNewFile() } catch (e: Exception) { false }
            }
            _message.value = if (ok) "File dibuat: $name" else "Gagal buat file"
            refresh()
        }
    }

    fun openFile(file: File) {
        try {
            val ctx = getApplication<Application>()
            val uri: Uri = FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            _message.value = "Tidak ada app untuk membuka file ini"
        }
    }

    fun fileInfo(file: File): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return """
            📄 Nama: ${file.name}
            
            📍 Lokasi: ${file.parent}
            
            🗂️ Tipe: ${if (file.isDirectory) "Folder" else file.extension.ifEmpty { "File" }}
            
            📊 Ukuran: ${if (file.isDirectory) "—" else FileItem.formatSize(file.length())}
            
            🕐 Dimodifikasi: ${sdf.format(java.util.Date(file.lastModified()))}
            
            🔐 Bisa dibaca: ${file.canRead()}
            ✏️ Bisa ditulis: ${file.canWrite()}
            
            🔒 Path:
            ${file.absolutePath}
        """.trimIndent()
    }

    fun clearMessage() { _message.value = null }

    private fun getMimeType(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }
}
