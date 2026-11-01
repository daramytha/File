package com.example.filemanager.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filemanager.FileItem
import com.example.filemanager.FileManagerViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(vm: FileManagerViewModel = viewModel()) {
    val files by vm.files.collectAsState()
    val currentPath by vm.currentPath.collectAsState()
    val clipboard by vm.clipboard.collectAsState()
    val message by vm.message.collectAsState()

    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📁 File Manager", fontWeight = FontWeight.Bold)
                        Text(
                            currentPath.name.ifEmpty { "/" },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.goBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.goUp() }) {
                        Icon(Icons.Default.ArrowUpward, "Up", tint = Color.White)
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C3E50),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (clipboard.file != null) {
                ExtendedFloatingActionButton(
                    onClick = { vm.paste() },
                    icon = { Icon(Icons.Default.ContentPaste, null) },
                    text = { Text("Paste") },
                    containerColor = Color(0xFF3498DB),
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Path bar
            Surface(
                color = Color(0xFFECF0F1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "📍 ${currentPath.absolutePath}",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Clipboard indicator
            if (clipboard.file != null) {
                Surface(color = Color(0xFFFFF3CD), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${if (clipboard.isCut) "✂️ Cut" else "📋 Copy"}: ${clipboard.file?.name}",
                        modifier = Modifier.padding(8.dp, 4.dp, 8.dp, 4.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF856404)
                    )
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(files, key = { it.file.absolutePath }) { item ->
                    FileRow(
                        item = item,
                        onClick = {
                            if (item.isDirectory) vm.loadDirectory(item.file)
                            else vm.openFile(item.file)
                        },
                        onLongClick = { selectedFile = item.file }
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                }
            }
        }

        // Context bottom sheet
        if (selectedFile != null) {
            FileActionSheet(
                file = selectedFile!!,
                onDismiss = { selectedFile = null },
                onCopy = { vm.copyFile(selectedFile!!); selectedFile = null },
                onCut = { vm.cutFile(selectedFile!!); selectedFile = null },
                onPaste = { vm.paste(); selectedFile = null },
                onRename = { showRenameDialog = true },
                onDelete = { showDeleteDialog = true },
                onInfo = { showInfoDialog = true },
                hasClipboard = clipboard.file != null
            )
        }
    }

    // Dialogs
    if (showDeleteDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus File") },
            text = { Text("Yakin hapus '${selectedFile!!.name}'?\n\n⚠️ Tidak bisa dikembalikan!") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteFile(selectedFile!!)
                    showDeleteDialog = false
                    selectedFile = null
                }) { Text("Hapus", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showRenameDialog && selectedFile != null) {
        var newName by remember { mutableStateOf(selectedFile!!.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nama baru") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameFile(selectedFile!!, newName)
                    showRenameDialog = false
                    selectedFile = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showInfoDialog && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("ℹ️ Info File") },
            text = {
                Text(
                    vm.fileInfo(selectedFile!!),
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Folder Baru") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama folder") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createFolder(name)
                    showNewFolderDialog = false
                }) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showNewFileDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("File Baru") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama file (contoh: note.txt)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createFile(name)
                    showNewFileDialog = false
                }) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(item: FileItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.icon, fontSize = 28.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.sizeFormatted} • ${item.dateFormatted}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        if (item.isDirectory) {
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionSheet(
    file: File,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    hasClipboard: Boolean
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                "📄 ${file.name}",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            HorizontalDivider()

            ActionRow("📋 Copy", "Salin file ke clipboard", onCopy, onDismiss)
            ActionRow("✂️ Cut", "Pindahkan file", onCut, onDismiss)
            if (hasClipboard) {
                ActionRow("📥 Paste ke sini", "Tempel file", onPaste, onDismiss)
            }
            ActionRow("✏️ Rename", "Ubah nama file", onRename, onDismiss)
            ActionRow("ℹ️ Info File", "Lihat detail file", onInfo, onDismiss)
            ActionRow("🗑️ Delete", "Hapus permanen", onDelete, onDismiss, isDanger = true)
        }
    }
}

@Composable
fun ActionRow(label: String, sub: String, onClick: () -> Unit, onDismiss: () -> Unit, isDanger: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickableFake { onClick(); onDismiss() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontWeight = FontWeight.Medium, color = if (isDanger) Color.Red else Color.Unspecified)
            Text(sub, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun Modifier.combinedClickableFake(onClick: () -> Unit): Modifier {
    return this.then(Modifier.padding(0.dp)).let {
        // pakai clickable dari foundation
        androidx.compose.foundation.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ).let { m -> this.then(m) }
    }
}
