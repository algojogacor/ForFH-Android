package com.aryariap.forfh.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract test migrasi V2→V3 (MIGRATION_2_3) — pola Migration12Test (guard kontrak JVM).
 *
 * V1→V2 menambah 3 tabel; V2→V3 menambah SATU kolom `syncState` ke tabel `tasks`
 * (ruling R24: `ALTER TABLE tasks ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'` —
 * baris lama dianggap SYNCED; tidak ada yang pending sebelum fitur ini ada).
 *
 * Drift yang dijaga: bila entity TaskEntity berubah (kolom syncState dihapus/berubah nama/
 * berubah tipe/default) atau SQL migrasi tidak sinkron, test ini MERAH — sama seperti
 * Migration12Test menjaga MIGRATION_1_2 terhadap entity tabel kampus. SQL migrasi harus
 * persis pola yang dipahami Room: `ALTER TABLE \`tasks\` ADD COLUMN \`syncState\` TEXT
 * NOT NULL DEFAULT 'SYNCED'` (pola yang sama dengan yang dihasilkan AutoMigration Room
 * untuk kolom baru non-null berdefault).
 */
class Migration23Test {

    @Test
    fun `MIGRATION_2_3 - versi 2 ke 3 dan SQL ALTER TABLE menambah kolom syncState`() {
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
        assertEquals(
            "ALTER TABLE `tasks` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'SYNCED'",
            AppDatabase.SQL_ADD_TASK_SYNC_STATE,
        )
    }

    @Test
    fun `entity TaskEntity punya kolom syncState bertipe String`() {
        // Entity dibaca lewat refleksi (annotasi Room BINARY — tidak tersedia di JVM murni);
        // guard yang bisa berjalan di JVM: field ada + bertipe String + nilai default = SYNCED.
        val field = TaskEntity::class.java.getDeclaredField("syncState")
        assertEquals(String::class.java, field.type)
    }

    @Test
    fun `nilai default entity syncState = SYNCED - baris lama dianggap tersinkron`() {
        assertEquals("SYNCED", fresh().syncState)
        assertEquals("PENDING", TaskEntity.SyncState.PENDING)
        assertEquals("SYNCED", TaskEntity.SyncState.SYNCED)
        assertEquals("FAILED", TaskEntity.SyncState.FAILED)
    }

    @Test
    fun `default SQL migrasi sama dengan default entity`() {
        // DEFAULT 'SYNCED' di SQL == nilai default konstruktor entity (R24: tidak ada pending saat migrasi)
        val sqlDefault = AppDatabase.SQL_ADD_TASK_SYNC_STATE
            .substringAfter("DEFAULT ").removeSurrounding("'")
        assertEquals(fresh().syncState, sqlDefault)
    }

    private fun fresh() = TaskEntity(
        id = "x", courseId = null, courseName = null, courseCode = null, title = "T",
        description = null, dueAt = null, status = "NOT_STARTED", computedStatus = null,
        priority = "low", courseColor = null, subtasksJson = null,
    )
}
