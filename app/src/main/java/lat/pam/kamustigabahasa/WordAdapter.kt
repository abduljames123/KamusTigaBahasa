package lat.pam.kamustigabahasa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WordAdapter(
    private var words: MutableList<Word>,
    private val listener: OnItemActionListener
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    interface OnItemActionListener {
        fun onEdit(word: Word)
        fun onDelete(word: Word)
    }

    inner class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIndo: TextView = view.findViewById(R.id.tvIndo)
        val tvEnglish: TextView = view.findViewById(R.id.tvEnglish)
        val tvArabic: TextView = view.findViewById(R.id.tvArabic)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word_with_actions, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val w = words[position]
        holder.tvIndo.text = w.indo
        holder.tvEnglish.text = w.english
        holder.tvArabic.text = w.arabic
        holder.tvCategory.text = w.category

        holder.btnEdit.setOnClickListener { listener.onEdit(w) }
        holder.btnDelete.setOnClickListener { listener.onDelete(w) }
    }

    override fun getItemCount(): Int = words.size

    fun updateList(newWords: List<Word>) {
        words.clear()
        words.addAll(newWords)
        notifyDataSetChanged()
    }
}
