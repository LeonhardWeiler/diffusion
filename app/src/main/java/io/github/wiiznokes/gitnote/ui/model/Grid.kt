package io.github.wiiznokes.gitnote.ui.model

import androidx.room.Embedded
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.R
import io.github.wiiznokes.gitnote.data.room.Note

enum class SortOrder {
    AZ,
    ZA,
    MostRecent,
    Oldest;

    override fun toString(): String {
        val res = when (this) {
            AZ -> R.string.az_sort_order
            ZA -> R.string.za_sort_order
            MostRecent -> R.string.most_recent_sort_order
            Oldest -> R.string.oldest_sort_order
        }
        return MyApp.appModule.uiHelper.getString(res)
    }
}

data class GridNote(
    @Embedded
    val note: Note,
    val isUnique: Boolean,
    val selected: Boolean = false,
)
