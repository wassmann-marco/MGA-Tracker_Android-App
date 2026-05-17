package com.example.feuerwehr

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// HINWEIS: TrainingEntry, UserProfile und QS werden NICHT mehr hier definiert,
// sondern zentral im AppViewModel.kt, um Doppelungen zu vermeiden.

// --- DAO (Data Access Object) ---
@Dao
interface TrainingDao {
    @Query("SELECT * FROM training_entries ORDER BY datum DESC, startStunde DESC")
    fun alleEintraege(): Flow<List<TrainingEntry>>

    @Query("SELECT DISTINCT ausbilder FROM training_entries ORDER BY ausbilder ASC")
    fun alleAusbilder(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einfuegen(entry: TrainingEntry): Long

    @Update
    suspend fun aktualisieren(entry: TrainingEntry): Int

    @Delete
    suspend fun loeschen(entry: TrainingEntry): Int

    @Query("SELECT * FROM training_entries WHERE modulId = :modulId AND datum = :datum")
    suspend fun findeDuplikat(modulId: String, datum: String): List<TrainingEntry>

    @Query("SELECT * FROM training_entries")
    suspend fun alleEintraegeFuerExport(): List<TrainingEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun alleEinfuegen(entries: List<TrainingEntry>): List<Long>

    @Query("DELETE FROM training_entries")
    suspend fun loescheAlle()
}

// --- DATABASE ---
@Database(entities = [TrainingEntry::class], version = 3, exportSchema = false)
abstract class TrainingDatabase : RoomDatabase() {
    abstract fun dao(): TrainingDao

    companion object {
        @Volatile
        private var INSTANCE: TrainingDatabase? = null

        fun getInstance(context: Context): TrainingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrainingDatabase::class.java,
                    "training_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- REPOSITORY ---
class TrainingRepository(private val dao: TrainingDao) {

    val eintraege: Flow<List<TrainingEntry>> = dao.alleEintraege()
    val ausbilder: Flow<List<String>> = dao.alleAusbilder()

    suspend fun speichern(entry: TrainingEntry): Long {
        return dao.einfuegen(entry)
    }

    suspend fun aktualisieren(entry: TrainingEntry): Int {
        return dao.aktualisieren(entry)
    }

    suspend fun loeschen(entry: TrainingEntry): Int {
        return dao.loeschen(entry)
    }

    suspend fun isDuplikat(modulId: String, datum: String): Boolean {
        return dao.findeDuplikat(modulId, datum).isNotEmpty()
    }

    suspend fun getAlleFuerExport(): List<TrainingEntry> {
        return dao.alleEintraegeFuerExport()
    }

    suspend fun importiereAlle(entries: List<TrainingEntry>): List<Long> {
        return dao.alleEinfuegen(entries)
    }

    suspend fun loescheAlle() = dao.loescheAlle()
}