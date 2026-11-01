package com.example.filemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.example.filemanager.ui.FileManagerScreen

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled in onResume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    FileManagerScreen()
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            val perms = mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            val notGranted = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (notGranted.isNotEmpty()) {
                permissionLauncher.launch(notGranted.toTypedArray())
            }
        }
    }
}
