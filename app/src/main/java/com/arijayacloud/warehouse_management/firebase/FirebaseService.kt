package com.arijayacloud.warehouse_management.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

object FirebaseService {

    val db = FirebaseFirestore.getInstance().apply {
        // Aktifkan offline persistence agar data bisa diakses meskipun offline
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    private const val COLLECTION_BARANG = "barang"
    private const val COLLECTION_TRANSAKSI = "transaksi"

    data class Barang(
        val sku: String = "",
        val nama: String = "",
        val lokasi: String = "",
        val deskripsi: String = "",
        val stok: Int = 0,
        val tanggalDibuat: String = "",
        val tanggalUpdate: String = ""
    )

    data class Transaksi(
        val id: String = "",
        val sku: String = "",
        val jenis: String = "", // "masuk" atau "keluar"
        val tanggal: String = "",
        val jumlah: Int = 0,
        val lokasi: String = "",
        val keterangan: String = ""
    )

    fun addBarang(
        sku: String,
        nama: String,
        lokasi: String,
        deskripsi: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentTime = getCurrentTimestamp()

        db.collection(COLLECTION_BARANG)
            .document(sku)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Barang sudah ada, update stok
                    val currentStok = document.getLong("stok")?.toInt() ?: 0
                    updateBarang(
                        sku = sku,
                        nama = nama,
                        lokasi = lokasi,
                        deskripsi = deskripsi,
                        stok = currentStok + 1,
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                } else {
                    // Barang baru, buat dokumen baru
                    val barang = Barang(
                        sku = sku,
                        nama = nama,
                        lokasi = lokasi,
                        deskripsi = deskripsi,
                        stok = 1,
                        tanggalDibuat = currentTime,
                        tanggalUpdate = currentTime
                    )
                    db.collection(COLLECTION_BARANG)
                        .document(sku)
                        .set(barang)
                        .addOnSuccessListener {
                            Log.d("FirebaseService", "Barang berhasil ditambahkan: $sku")
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirebaseService", "Error menambah barang: ", exception)
                            onFailure(exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error cek SKU: ", exception)
                onFailure(exception)
            }
    }

    private fun updateBarang(
        sku: String,
        nama: String,
        lokasi: String,
        deskripsi: String,
        stok: Int,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "nama" to nama,
            "lokasi" to lokasi,
            "deskripsi" to deskripsi,
            "stok" to stok,
            "tanggalUpdate" to getCurrentTimestamp()
        )

        db.collection(COLLECTION_BARANG)
            .document(sku)
            .update(updates)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Barang berhasil diupdate: $sku")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error update barang: ", exception)
                onFailure(exception)
            }
    }

    fun logTransaksi(
        sku: String,
        jenis: String,
        tanggal: String,
        jumlah: Int,
        lokasi: String,
        keterangan: String = "",
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val transaksiId = db.collection(COLLECTION_TRANSAKSI).document().id

        val transaksi = Transaksi(
            id = transaksiId,
            sku = sku,
            jenis = jenis,
            tanggal = tanggal,
            jumlah = jumlah,
            lokasi = lokasi,
            keterangan = keterangan
        )

        db.collection(COLLECTION_TRANSAKSI)
            .document(transaksiId)
            .set(transaksi)
            .addOnSuccessListener {
                Log.d("FirebaseService", "Transaksi berhasil dicatat: $transaksiId")
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error catat transaksi: ", exception)
                onFailure?.invoke(exception)
            }
    }

    fun getBarang(
        sku: String,
        onSuccess: (Barang?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_BARANG)
            .document(sku)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val barang = document.toObject(Barang::class.java)
                    onSuccess(barang)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error get barang: ", exception)
                onFailure(exception)
            }
    }

    fun getAllBarang(
        onSuccess: (List<Barang>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_BARANG)
            .orderBy("tanggalUpdate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val barangList = documents.mapNotNull { it.toObject(Barang::class.java) }
                onSuccess(barangList)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error get all barang: ", exception)
                onFailure(exception)
            }
    }

    fun getTransaksiHistory(
        sku: String,
        onSuccess: (List<Transaksi>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_TRANSAKSI)
            .whereEqualTo("sku", sku)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val transaksiList = documents.mapNotNull { it.toObject(Transaksi::class.java) }
                onSuccess(transaksiList)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error get transaksi history: ", exception)
                onFailure(exception)
            }
    }

    fun kurangiStok(
        sku: String,
        jumlah: Int,
        lokasi: String,
        keterangan: String = "",
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_BARANG)
            .document(sku)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentStok = document.getLong("stok")?.toInt() ?: 0
                    if (currentStok >= jumlah) {
                        val newStok = currentStok - jumlah
                        db.collection(COLLECTION_BARANG)
                            .document(sku)
                            .update(
                                mapOf(
                                    "stok" to newStok,
                                    "tanggalUpdate" to getCurrentTimestamp()
                                )
                            )
                            .addOnSuccessListener {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val tanggal = sdf.format(Date())

                                logTransaksi(
                                    sku = sku,
                                    jenis = "keluar",
                                    tanggal = tanggal,
                                    jumlah = jumlah,
                                    lokasi = lokasi,
                                    keterangan = keterangan,
                                    onSuccess = onSuccess,
                                    onFailure = onFailure
                                )
                            }
                            .addOnFailureListener(onFailure)
                    } else {
                        onFailure(Exception("Stok tidak mencukupi. Stok tersedia: $currentStok"))
                    }
                } else {
                    onFailure(Exception("Barang dengan SKU $sku tidak ditemukan"))
                }
            }
            .addOnFailureListener(onFailure)
    }

    fun deleteBarang(
        sku: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(COLLECTION_BARANG)
            .document(sku)
            .delete()
            .addOnSuccessListener {
                Log.d("FirebaseService", "Barang berhasil dihapus: $sku")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error hapus barang: ", exception)
                onFailure(exception)
            }
    }
    fun getAllTransaksiMasuk(
        onSuccess: (List<Transaksi>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("transaksi")
            .whereEqualTo("jenis", "masuk")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val transaksiList = documents.mapNotNull { it.toObject(Transaksi::class.java) }
                onSuccess(transaksiList)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseService", "Error get transaksi masuk: ", exception)
                onFailure(exception)
            }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}
