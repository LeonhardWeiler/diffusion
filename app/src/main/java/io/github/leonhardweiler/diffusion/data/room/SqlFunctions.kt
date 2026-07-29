package io.github.leonhardweiler.diffusion.data.room

import io.requery.android.database.sqlite.SQLiteDatabase
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * What the queries in [RepoDatabaseDao] can call that SQLite does not know by
 * itself. They are registered in [RepoDatabase.buildDatabase], which is also
 * why the database is opened through requery's SQLite rather than the
 * platform's — the platform's cannot be taught new functions.
 */

/**
 * How well a note matches what was searched for.
 *
 * Reads the matchinfo blob of FTS4 directly: two counts, then three numbers per
 * phrase and column, of which only the first — how often the phrase occurs in
 * this row — is looked at. A hit in the path beats a hit in the text, because
 * somebody searching a word that is in a note's name is looking for that note.
 */
object Rank : SQLiteDatabase.Function {
    override fun callback(
        args: SQLiteDatabase.Function.Args?,
        result: SQLiteDatabase.Function.Result?
    ) {
        if (args == null || result == null) return

        val blob = args.getBlob(0) ?: return

        val buffer = ByteBuffer.wrap(blob).order(ByteOrder.nativeOrder())

        val phraseCount = buffer.int
        val columnCount = buffer.int

        var score = 0.0

        for (phrase in 0 until phraseCount) {
            for (column in 0 until columnCount) {

                val hitsThisRow = buffer.int
                buffer.int
                buffer.int

                if (hitsThisRow != 0) {
                    // relativePath column
                    if (column == 0) {
                        result.set(2.0)
                        return
                    }
                    // content column
                    else {
                        score = 1.0
                    }
                }
            }
        }

        result.set(score)
    }
}

/** The folder a path is in, empty for one that is at the top. */
object ParentPath : SQLiteDatabase.Function {
    override fun callback(
        args: SQLiteDatabase.Function.Args?,
        result: SQLiteDatabase.Function.Result?
    ) {
        if (args == null || result == null) return

        val path = args.getString(0) ?: return

        if (path == "") return

        result.set(path.substringBeforeLast("/", missingDelimiterValue = ""))
    }
}

/** The last part of a path, which is what a folder is called. */
object FullName : SQLiteDatabase.Function {
    override fun callback(
        args: SQLiteDatabase.Function.Args?,
        result: SQLiteDatabase.Function.Result?
    ) {
        if (args == null || result == null) return

        val path = args.getString(0) ?: return

        result.set(path.substringAfterLast("/"))
    }
}
