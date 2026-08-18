package com.aryariap.forfh.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScheduleEntity::class,
        TaskEntity::class,
        ScheduledAlarmEntity::class,
        PresensiRecapEntity::class,
        KampusInfoEntity::class,
        KampusMetaEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schedulesDao(): SchedulesDao
    abstract fun tasksDao(): TasksDao
    abstract fun scheduledAlarmsDao(): ScheduledAlarmsDao
    abstract fun kampusInfoDao(): KampusInfoDao

    companion object {
        /** V1 (rilis V1.0) → V2 (V1.1): 3 tabel info kampus baru — V1 user tidak kehilangan data. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_PRESENSI_RECAP)
                db.execSQL(CREATE_KAMPUS_INFO)
                db.execSQL(CREATE_KAMPUS_META)
            }
        }

        /**
         * V2 → V3 (Task 10, ruling R24): kolom syncState di tabel tasks untuk mark selesai
         * optimistik. NOT NULL DEFAULT 'SYNCED' — baris lama dianggap tersinkron (tidak ada
         * yang pending sebelum fitur ini ada).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(SQL_ADD_TASK_SYNC_STATE)
            }
        }

        /** V3 → V4: kolom externalId di tabel tasks untuk tautan langsung event HEBAT. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `externalId` TEXT")
            }
        }

        // SQL CREATE TABLE tabel info kampus (schema V2). internal + const (bukan private):
        // dipakai bersama Migration12Test — contract test JVM yang menjaga SQL ini tetap sinkron
        // dengan entity (schema drift → test merah). Runtime Room menvalidasi migrasi sesungguhnya
        // di perangkat (smoke E2E, item terjadwal) — di sisi JVM inilah penggantinya.
        internal const val CREATE_PRESENSI_RECAP =
            "CREATE TABLE IF NOT EXISTS `presensi_recap` (`kode` TEXT NOT NULL, `nama` TEXT NOT NULL, " +
                "`tm` INTEGER, `hadir` INTEGER, `persen` INTEGER, PRIMARY KEY(`kode`, `nama`))"
        internal const val CREATE_KAMPUS_INFO =
            "CREATE TABLE IF NOT EXISTS `kampus_info` (`jenis` TEXT NOT NULL, `dataJson` TEXT NOT NULL, " +
                "`updatedAt` TEXT NOT NULL, PRIMARY KEY(`jenis`))"
        internal const val CREATE_KAMPUS_META =
            "CREATE TABLE IF NOT EXISTS `kampus_meta` (`id` INTEGER NOT NULL, `connected` INTEGER NOT NULL, " +
                "`lastSyncAt` TEXT, PRIMARY KEY(`id`))"

        // SQL migrasi V2→V3. internal + const: dipakai bersama Migration23Test (contract test
        // JVM, pola MIGRATION_1_2) — SQL ini harus persis dengan schema entity TaskEntity.
        internal const val SQL_ADD_TASK_SYNC_STATE =
            "ALTER TABLE `tasks` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'SYNCED'"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "forfh.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
