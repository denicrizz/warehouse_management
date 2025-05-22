// MainActivity.kt
package com.arijayacloud.warehouse_management
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arijayacloud.warehouse_management.firebase.FirebaseService
import com.arijayacloud.warehouse_management.ui.theme.Warehouse_managementTheme
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Warehouse_managementTheme {
                WarehouseApp() // Fungsi Composable utama
            }
        }
    }
}


@Composable
fun WarehouseApp() {
    var sku by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            sku = result.data?.getStringExtra("scanned_code") ?: ""
            showDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val intent = Intent(context, ScanActivity::class.java)
                launcher.launch(intent)
            }) {
                Text("+")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Warehouse Management", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }

    if (showDialog) {
        InputDialog(sku = sku) {
            showDialog = false
            sku = "" // reset sku setelah dialog dismiss
        }
    }
}

@Composable
fun InputDialog(sku: String, onDismiss: () -> Unit) {
    var nama by remember { mutableStateOf(TextFieldValue("")) }
    var lokasi by remember { mutableStateOf(TextFieldValue("")) }
    var deskripsi by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                if (sku.isNotEmpty() && nama.text.isNotEmpty() && lokasi.text.isNotEmpty()) {
                    FirebaseService.addBarang(
                        sku = sku,
                        nama = nama.text,
                        lokasi = lokasi.text,
                        deskripsi = deskripsi.text,
                        onSuccess = {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val tanggal = sdf.format(Date())
                            FirebaseService.logTransaksi(sku, "masuk", tanggal, 1, lokasi.text)
                            Toast.makeText(context, "Barang berhasil disimpan", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        onFailure = {
                            Toast.makeText(context, "Gagal menyimpan barang", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Data tidak lengkap", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        title = { Text("Input Data Barang") },
        text = {
            Column {
                Text("Kode SKU: $sku")
                OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Barang") })
                OutlinedTextField(value = lokasi, onValueChange = { lokasi = it }, label = { Text("Lokasi Penyimpanan") })
                OutlinedTextField(value = deskripsi, onValueChange = { deskripsi = it }, label = { Text("Deskripsi") })
            }
        }
    )
}
