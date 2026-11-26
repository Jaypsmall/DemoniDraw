package com.jaylizapp.demonidraw.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [GestureEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gestureDao(): GestureDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "demonidraw_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    val dao = database.gestureDao()
                                    dao.insertGesture(GestureEntry(name = "CÁMARA", action = "com.android.camera", isShellCommand = false))
                                    dao.insertGesture(GestureEntry(name = "REBOOT", action = "reboot", isShellCommand = true))
                                    dao.insertGesture(GestureEntry(name = "AJUSTES", action = "com.android.settings", isShellCommand = false))
                                    dao.insertGesture(GestureEntry(name = "WHATSAPP", action = "com.whatsapp", isShellCommand = false))
                                    dao.insertGesture(GestureEntry(name = "ROOT SHELL", action = "su", isShellCommand = true))
                                }
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
