package com.example.billingapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class, Category::class, Folder::class, Bill::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun folderDao(): FolderDao
    abstract fun billDao(): BillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transactions ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories (name)")
                val defaults = listOf("Alga", "Ēdiens", "Transports", "Izklaides", "Veselība", "Komunālie", "Apģērbs", "Izglītība", "Dāvanas", "Cits")
                defaults.forEach { name ->
                    database.execSQL("INSERT OR IGNORE INTO categories (name) VALUES (?)", arrayOf(name))
                }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                database.execSQL("ALTER TABLE transactions ADD COLUMN folderId INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN imagePath TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE transactions ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""CREATE TABLE IF NOT EXISTS bills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    amount REAL NOT NULL,
                    dueDate INTEGER NOT NULL,
                    category TEXT NOT NULL DEFAULT '',
                    description TEXT NOT NULL DEFAULT '',
                    isPaid INTEGER NOT NULL DEFAULT 0,
                    paidAt INTEGER NOT NULL DEFAULT 0,
                    paidLocation TEXT NOT NULL DEFAULT '',
                    isRecurring INTEGER NOT NULL DEFAULT 0,
                    recurringInterval TEXT NOT NULL DEFAULT '',
                    reminderEnabled INTEGER NOT NULL DEFAULT 0,
                    reminderDaysBefore INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    imagePath TEXT NOT NULL DEFAULT ''
                )""")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billing_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
