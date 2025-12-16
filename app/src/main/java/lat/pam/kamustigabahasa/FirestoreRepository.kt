package lat.pam.kamustigabahasa

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("words")

    companion object {
        private const val TAG = "FirestoreRepo"
    }

    /** Cek koneksi sederhana */
    suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            Tasks.await(col.limit(1).get())
            true
        } catch (t: Throwable) {
            Log.e(TAG, "ping error: ${t.message}", t)
            false
        }
    }

    /** Dengarkan semua data realtime */
    fun listenAll(onChange: (List<Word>) -> Unit): ListenerRegistration {
        return col.orderBy("indo", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e(TAG, "listenAll error: ${e.message}", e)
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { it.toWordSafe() } ?: emptyList()
                onChange(list)
            }
    }

    /** Search prefix indo */
    fun searchByPrefix(q: String, onChange: (List<Word>) -> Unit): ListenerRegistration {
        val end = q + "\uf8ff"
        return col.orderBy("indo")
            .startAt(q)
            .endAt(end)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e(TAG, "searchByPrefix error: ${e.message}", e)
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.map { it.toWordSafe() } ?: emptyList()
                onChange(list)
            }
    }

    /** Ambil sekali */
    suspend fun getAllOnce(): List<Word> = withContext(Dispatchers.IO) {
        try {
            val snap = Tasks.await(col.orderBy("indo").get())
            snap.documents.map { it.toWordSafe() }
        } catch (t: Throwable) {
            Log.e(TAG, "getAllOnce error: ${t.message}", t)
            emptyList()
        }
    }

    /** Hitung dokumen */
    suspend fun countWords(): Int = withContext(Dispatchers.IO) {
        try {
            val snap = Tasks.await(col.get())
            snap.size()
        } catch (t: Throwable) {
            Log.e(TAG, "countWords error: ${t.message}", t)
            0
        }
    }

    /** Add/Upsert */
    suspend fun addWord(word: Word) {
        val docRef = col.document(word.id)

        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            throw Exception("Kata sudah ada")
        }

        docRef.set(word).await()
    }

    /** Update by id */
    suspend fun updateWord(w: Word) = withContext(Dispatchers.IO) {
        require(w.id.isNotBlank()) { "Word.id kosong saat update" }
        Tasks.await(col.document(w.id).set(w.toMapForFirestore(), SetOptions.merge()))
    }

    /** Delete by id */
    suspend fun deleteWord(id: String) = withContext(Dispatchers.IO) {
        require(id.isNotBlank()) { "id kosong saat delete" }
        Tasks.await(col.document(id).delete())
    }

    /** Seed batch (upsert) */
    suspend fun seed(words: List<Word>) = withContext(Dispatchers.IO) {
        if (words.isEmpty()) return@withContext
        try {
            Tasks.await(db.runBatch { b ->
                words.forEach { w0 ->
                    val id = stableId(w0)
                    b.set(col.document(id), w0.copy(id = id).toMapForFirestore(), SetOptions.merge())
                }
            })
        } catch (t: Throwable) {
            Log.e(TAG, "seed error: ${t.message}", t)
            throw t
        }
    }

    /** Buat ID stabil (biar tidak dobel) */
    private fun stableId(w: Word): String {
        return when {
            w.id.isNotBlank() -> w.id.trim()
            w.indo.isNotBlank() -> w.indo.trim().lowercase()
            else -> col.document().id
        }
    }

    /** Snapshot -> Word aman */
    private fun DocumentSnapshot.toWordSafe(): Word {
        val docId = id

        val indo = getString("indo") ?: ""
        val english = getString("english") ?: ""
        val arabic = getString("arabic") ?: ""
        val category = getString("category") ?: ""

        return Word(
            id = docId,
            indo = indo,
            english = english,
            arabic = arabic,
            category = category
        )
    }

    /** Word -> Map */
    private fun Word.toMapForFirestore() = mapOf(
        "indo" to indo,
        "english" to english,
        "arabic" to arabic,
        "category" to category
    )
}
