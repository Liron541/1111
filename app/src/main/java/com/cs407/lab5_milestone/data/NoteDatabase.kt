package com.cs407.lab5_milestone.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import com.cs407.lab5_milestone.R
import java.util.Date


data class NoteSummary(
    val noteId: Int,
    val noteTitle: String,
    val noteAbstract: String,
    val lastEdited: Date
)

// Converters for Date type
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

// Define User entity
@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Int = 0, // AUTOINCREMENT equivalent
    val username: String,
    val passwordHash: String
)

// Define Note entity
@Entity(tableName = "note")
data class Note(
    @PrimaryKey(autoGenerate = true) val noteId: Int = 0,
    val userId: Int, // Foreign key reference to User
    val noteTitle: String,
    val noteAbstract: String,
    val noteDetail: String?,
    val notePath: String?,
    @TypeConverters(Converters::class) val lastEdited: Date
)


// UserDao interface
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: User): Long

    @Query("DELETE FROM user WHERE username = :username")
    suspend fun deleteUserByUsername(username: String)

    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
}

// NoteDao interface
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note)

    @Query("DELETE FROM note WHERE noteId = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    @Query("SELECT * FROM note WHERE userId = :userId ORDER BY lastEdited DESC")
    suspend fun getNotesForUser(userId: Int): List<Note>

    @Query("SELECT * FROM note WHERE noteId = :noteId")
    suspend fun getNoteById(noteId: Int): Note?

    @Query("SELECT noteId, noteTitle, noteAbstract, lastEdited FROM note WHERE userId = :userId ORDER BY lastEdited DESC")
    fun getNoteListsByUserIdPaged(userId: Int): PagingSource<Int, NoteSummary>
}

// Define the Room Database
@Database(entities = [User::class, Note::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}