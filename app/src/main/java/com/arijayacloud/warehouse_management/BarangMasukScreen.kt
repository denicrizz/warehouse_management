package com.arijayacloud.warehouse_management.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arijayacloud.warehouse_management.firebase.FirebaseService


@Composable
fun RiwayatBarangMasukScreen() {
    var riwayatMasuk by remember { mutableStateOf(listOf<FirebaseService.Transaksi>()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        FirebaseService.db.collection("transaksi")
            .whereEqualTo("jenis", "masuk")
            .get()
            .addOnSuccessListener { documents ->
                val list = documents.mapNotNull { it.toObject(FirebaseService.Transaksi::class.java) }
                riwayatMasuk = list
            }
            .addOnFailureListener { e ->
                errorMessage = "Gagal memuat data: ${e.message}"
            }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
//        Button(onClick = onBack) {
//            Text("Kembali")
//        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
        }

        LazyColumn {
            items(riwayatMasuk) { transaksi ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("SKU: ${transaksi.sku}")
                        Text("Jumlah: ${transaksi.jumlah}")
                        Text("Tanggal: ${transaksi.tanggal}")
                        Text("Lokasi: ${transaksi.lokasi}")
                        Text("Keterangan: ${transaksi.keterangan}")
                    }
                }
            }
        }
    }
}
