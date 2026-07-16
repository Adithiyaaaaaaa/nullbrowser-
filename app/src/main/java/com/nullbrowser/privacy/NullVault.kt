package com.nullbrowser.privacy

import android.content.Context
import androidx.room.*

@Entity(tableName = "secure_bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM secure_bookmarks ORDER BY timestamp DESC")
    fun getAll(): List<Bookmark>

    @Insert
    fun insert(bookmark: Bookmark)

    @Delete
    fun delete(bookmark: Bookmark)

    @Query("DELETE FROM secure_bookmarks")
    fun deleteAll()
}

@Database(entities = [Bookmark::class], version = 1)
abstract class NullDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: NullDatabase? = null

        fun getDatabase(context: Context): NullDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NullDatabase::class.java,
                    "null_vault.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
