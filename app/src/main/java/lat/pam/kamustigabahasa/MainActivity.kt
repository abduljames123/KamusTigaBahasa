package lat.pam.kamustigabahasa

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), WordAdapter.OnItemActionListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val DEBUG_FIRESTORE = true // true = pakai JSON lokal
    }

    private lateinit var adapter: WordAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonAdd: FloatingActionButton
    private lateinit var searchView: SearchView

    private var cacheWords: List<Word> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerViewWords)
        buttonAdd = findViewById(R.id.buttonAdd)
        searchView = findViewById(R.id.searchView)

        // FIX: biar kotak SearchView bisa dipencet (bukan cuma ikon)
        searchView.apply {
            isIconified = false
            setIconifiedByDefault(false)
            clearFocus()

            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true

            val searchText = findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            searchText.isFocusable = true
            searchText.isFocusableInTouchMode = true
            searchText.isClickable = true
            searchText.isLongClickable = true
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WordAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        if (DEBUG_FIRESTORE) {
            val local = LocalLoader.loadFromRawJson(this, R.raw.dictionary)
            Log.d(TAG, "JSON size = ${local.size}")
            cacheWords = local
            adapter.updateList(local)
            setupSearchOffline()

            if (local.isEmpty()) {
                Toast.makeText(
                    this,
                    "JSON kebaca tapi kosong. Cek format & res/raw/dictionary.json",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(this, "Mode Firestore belum dipakai di versi ini", Toast.LENGTH_SHORT).show()
        }

        buttonAdd.setOnClickListener { showAddDialogOffline() }
    }

    private fun setupSearchOffline() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true

            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText?.trim().orEmpty()

                val filtered = if (q.isBlank()) cacheWords else cacheWords.filter { w ->
                    w.indo.startsWith(q, ignoreCase = true)
                            || w.english.startsWith(q, ignoreCase = true)
                            || w.arabic.startsWith(q, ignoreCase = true)
                }

                adapter.updateList(filtered)
                return true
            }
        })
    }

    private fun showAddDialogOffline() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_word_form, null)
        val etIndo = view.findViewById<EditText>(R.id.etIndo)
        val etEng = view.findViewById<EditText>(R.id.etEng)
        val etArab = view.findViewById<EditText>(R.id.etArab)
        val etCat = view.findViewById<EditText>(R.id.etCategory)

        AlertDialog.Builder(this)
            .setTitle("Tambah Kata")
            .setView(view)
            .setPositiveButton("Tambah") { _, _ ->
                val indo = etIndo.text.toString().trim()
                val eng = etEng.text.toString().trim()
                val arb = etArab.text.toString().trim()
                val cat = etCat.text.toString().trim()

                if (indo.isBlank() || eng.isBlank() || arb.isBlank()) {
                    Toast.makeText(this, "Indonesia/English/Arab wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isDuplicateLocal(indo)) {
                    Toast.makeText(this, "Kata \"$indo\" sudah ada!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val w = Word(
                    id = indo.lowercase(),
                    indo = indo,
                    english = eng,
                    arabic = arb,
                    category = cat
                )

                cacheWords = listOf(w) + cacheWords
                adapter.updateList(cacheWords)
                Toast.makeText(this, "Ditambahkan (offline)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onEdit(word: Word) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_word_form, null)
        val etIndo = view.findViewById<EditText>(R.id.etIndo)
        val etEng = view.findViewById<EditText>(R.id.etEng)
        val etArab = view.findViewById<EditText>(R.id.etArab)
        val etCat = view.findViewById<EditText>(R.id.etCategory)

        etIndo.setText(word.indo)
        etEng.setText(word.english)
        etArab.setText(word.arabic)
        etCat.setText(word.category)

        AlertDialog.Builder(this)
            .setTitle("Edit Kata (Offline)")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val indo = etIndo.text.toString().trim()
                val eng = etEng.text.toString().trim()
                val arb = etArab.text.toString().trim()
                val cat = etCat.text.toString().trim()

                if (indo.isBlank() || eng.isBlank() || arb.isBlank()) {
                    Toast.makeText(this, "Indonesia/English/Arab wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // kalau kata indo diganti, cek duplikat (kecuali dirinya sendiri)
                val isRename = !word.indo.equals(indo, ignoreCase = true)
                if (isRename && isDuplicateLocal(indo)) {
                    Toast.makeText(this, "Kata \"$indo\" sudah ada!", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val updated = word.copy(
                    id = indo.lowercase(),
                    indo = indo,
                    english = eng,
                    arabic = arb,
                    category = cat
                )

                cacheWords = cacheWords.map { if (it.id == word.id) updated else it }
                adapter.updateList(cacheWords)
                Toast.makeText(this, "Diupdate (offline)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDelete(word: Word) {
        AlertDialog.Builder(this)
            .setTitle("Hapus kata")
            .setMessage("Hapus \"${word.indo}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                cacheWords = cacheWords.filterNot { it.id == word.id }
                adapter.updateList(cacheWords)
                Toast.makeText(this, "Dihapus (offline)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun isDuplicateLocal(indo: String): Boolean {
        return cacheWords.any { it.indo.equals(indo, ignoreCase = true) }
    }
}
