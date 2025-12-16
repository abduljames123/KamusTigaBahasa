package lat.pam.kamustigabahasa

import android.content.Context
import androidx.annotation.RawRes
import org.json.JSONArray
import org.json.JSONObject

object LocalLoader {

    fun loadFromRawJson(context: Context, @RawRes resId: Int): List<Word> {
        val text = context.resources.openRawResource(resId).bufferedReader().use { it.readText() }

        val arr = try {
            JSONObject(text).getJSONArray("words")
        } catch (_: Exception) {
            JSONArray(text)
        }

        val out = mutableListOf<Word>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)

            val indo = o.optString("indo", o.optString("id")).trim()
            val english = o.optString("english", o.optString("en")).trim()
            val arabic = o.optString("arabic", o.optString("ar")).trim()
            val category = o.optString("category", o.optString("kategori")).trim()

            // id prioritas dari JSON, kalau kosong pakai indo biar stabil
            val id = o.optString("id", indo).trim()

            out.add(
                Word(
                    id = id,
                    indo = indo,
                    english = english,
                    arabic = arabic,
                    category = category
                )
            )
        }
        return out
    }
}
