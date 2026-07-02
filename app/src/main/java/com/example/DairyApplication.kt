package com.example

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.AppDatabase
import com.example.data.repository.DairyRepository
import com.example.data.model.Customer
import com.example.data.model.CustomerStatus
import com.example.data.model.BillingCycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import android.util.Log

import com.example.auth.AuthManager
import com.example.data.sync.SyncManager
import com.example.data.repository.SearchManager

class DairyApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        try {
            val projectId = BuildConfig.FIREBASE_PROJECT_ID
            val appId = BuildConfig.FIREBASE_APP_ID
            val apiKey = BuildConfig.FIREBASE_API_KEY
            
            if (projectId.isNotEmpty() && appId.isNotEmpty() && apiKey.isNotEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(projectId)
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("FirebaseInit", "Firebase initialized successfully")
            } else {
                Log.e("FirebaseInit", "Firebase credentials missing in Secrets / BuildConfig")
            }
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Error initializing Firebase", e)
        }
        
        container = AppDataContainer(this)
    }
}

interface AppContainer {
    val dairyRepository: DairyRepository
    val authManager: AuthManager
    val syncManager: SyncManager
    val searchManager: SearchManager
}

class AppDataContainer(private val application: Application) : AppContainer {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    override val authManager: AuthManager by lazy { AuthManager(application) }
    
    // ... migrations ...
    private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `daily_deliveries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `session` TEXT NOT NULL, `status` TEXT NOT NULL)")
        }
    }

    private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `delivery_entries` ADD COLUMN `reason` TEXT")
            db.execSQL("ALTER TABLE `delivery_entries` ADD COLUMN `autoResume` INTEGER NOT NULL DEFAULT 1")
        }
    }

    private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `amount` REAL NOT NULL, `dateMillis` INTEGER NOT NULL, `type` TEXT NOT NULL, `notes` TEXT)")
        }
    }

    private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentMode` TEXT")
            db.execSQL("ALTER TABLE `customers` ADD COLUMN `cycleStartDay` INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE `customers` ADD COLUMN `cycleEndDay` INTEGER NOT NULL DEFAULT 31")
            db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `title` TEXT NOT NULL, `details` TEXT NOT NULL, `reminderDateMillis` INTEGER, `priority` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL)")
        }
    }

    private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `notes` ADD COLUMN `lastUpdatedMillis` INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add soft delete fields to all tables
            val tables = listOf("customers", "delivery_entries", "daily_deliveries", "transactions", "notes")
            for (table in tables) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `deletedAt` INTEGER")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `deletedBy` TEXT")
            }
        }
    }

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "dairy_database"
        )
        .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
        .fallbackToDestructiveMigration()
        .build()
    }

    override val dairyRepository: DairyRepository by lazy {
        DairyRepository(application, database.customerDao(), database.deliveryEntryDao(), database.dailyDeliveryDao(), database.transactionDao(), database.noteDao())
    }
    
    override val syncManager: SyncManager by lazy { SyncManager(database) }
    
    override val searchManager: SearchManager by lazy { SearchManager(application) }
}
