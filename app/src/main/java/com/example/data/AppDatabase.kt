package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ConceptDao
import com.example.data.dao.HistoryDao
import com.example.data.entity.ConceptItem
import com.example.data.entity.QuestionHistory

@Database(
    entities = [ConceptItem::class, QuestionHistory::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conceptDao(): ConceptDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE concepts ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE concepts ADD COLUMN easeFactor REAL NOT NULL DEFAULT 2.5"
                )
                db.execSQL(
                    "ALTER TABLE concepts ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE concepts ADD COLUMN nextReviewAt INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE concepts ADD COLUMN lapses INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE concepts ADD COLUMN masteryScore REAL NOT NULL DEFAULT 0.0"
                )
                db.execSQL(
                    "ALTER TABLE concepts ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Medium'"
                )
                db.execSQL(
                    "ALTER TABLE question_history ADD COLUMN perQuestionResultsJson TEXT"
                )
                db.execSQL(
                    "ALTER TABLE question_history ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'Medium'"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "unlock_learn_db"
                            )
                            .addMigrations(MIGRATION_2_3)
                            .fallbackToDestructiveMigration(dropAllTables = false)
                            .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
