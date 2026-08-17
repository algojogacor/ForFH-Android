package com.aryariap.forfh.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test migrasi V1→V2 (MIGRATION_1_2) — pengganti validasi runtime Room di sisi JVM.
 *
 * Room menvalidasi migrasi terhadap entity HANYA saat membuka DB di perangkat; bila SQL migrasi
 * tidak cocok dengan schema entity (mis. kolom berubah nama/tipe, PK berubah, tabel beda nama),
 * Room melempar di runtime dan `.fallbackToDestructiveMigration(true)` akan MEMBUANG semua data
 * user V1 (termasuk ponsel teman yang sudah install V1.0). Validasi di perangkat adalah smoke
 * E2E yang tertunda (item terjadwal) — test ini adalah guard kontrak sisi JVM: setiap perubahan
 * entity tanpa memperbarui SQL migrasi membuat test MERAH.
 *
 * Contract yang dijaga (sumber kebenaran: entity PresensiRecapEntity / KampusInfoEntity /
 * KampusMetaEntity di AppDatabase.kt): untuk tiap tabel, nama tabel + seluruh kolom
 * (nama + affinity INTEGER/TEXT + NOT NULL) + kolom PRIMARY KEY (termasuk composite PK)
 * harus muncul persis di statement CREATE TABLE yang dijalankan MIGRATION_1_2.
 *
 * Catatan: entity dibaca dari kompilasi (annotasi Room) tidak tersedia untuk diproses di unit
 * test JVM murni tanpa Robolectric/room-compiler runtime, jadi schema entity ditulis eksplisit
 * di sini sebagai daftar harapan (expected) — kapan pun entity berubah, file ini ikut berubah.
 */
class Migration12Test {

    /** Harapan satu kolom: nama + affinity SQL + NOT NULL (false = nullable). */
    private data class ColumnSpec(val name: String, val affinity: String, val notNull: Boolean)

    private fun assertTable(sql: String, table: String, columns: List<ColumnSpec>, primaryKey: List<String>) {
        val (tableName, body) = parseCreateTable(sql)
        assertEquals("nama tabel entity", table, tableName)

        val defs = splitColumns(body)
        val pkClause = defs.firstOrNull { it.startsWith("PRIMARY KEY") }
        assertTrue("tabel $table wajib punya klausa PRIMARY KEY: $sql", pkClause != null)

        for (spec in columns) {
            val def = defs.firstOrNull { it.startsWith("`${spec.name}`") }
            assertTrue("kolom `${spec.name}` tidak ada di CREATE TABLE $table: $sql", def != null)
            assertTrue(
                "affinity kolom `${spec.name}` harus ${spec.affinity} (entity): $def",
                def!!.contains(" ${spec.affinity}"),
            )
            if (spec.notNull) {
                assertTrue("kolom `${spec.name}` harus NOT NULL (entity): $def", def.contains("NOT NULL"))
            }
        }

        // Kolom PK harus persis (urutan termasuk composite PK, mis. presensi_recap: kode, nama).
        val pkCols = pkClause!!
            .removePrefix("PRIMARY KEY")
            .trim()
            .removeSurrounding("(", ")")
            .split(",")
            .map { it.trim().removeSurrounding("`") }
        assertEquals("primary key tabel $table (composite PK termasuk urutan)", primaryKey, pkCols)
    }

    /** Parse `CREATE TABLE IF NOT EXISTS \`tbl\` (...)` → (nama tabel, isi body). */
    private fun parseCreateTable(sql: String): Pair<String, String> {
        val match = Regex("CREATE TABLE IF NOT EXISTS `([^`]+)` \\((.+)\\)\\s*$", RegexOption.DOT_MATCHES_ALL)
            .matchEntire(sql.trim())
            ?: error("statement bukan CREATE TABLE yang dikenal: $sql")
        return match.groupValues[1] to match.groupValues[2]
    }

    /** Pecah definisi kolom pada koma level-0 (koma di dalam PRIMARY KEY(...) tidak terpecah). */
    private fun splitColumns(body: String): List<String> {
        val cols = mutableListOf<String>()
        var depth = 0
        val current = StringBuilder()
        for (ch in body) {
            when (ch) {
                '(' -> {
                    depth++
                    current.append(ch)
                }
                ')' -> {
                    depth--
                    current.append(ch)
                }
                ',' -> if (depth == 0) {
                    cols += current.toString().trim()
                    current.setLength(0)
                } else {
                    current.append(ch)
                }
                else -> current.append(ch)
            }
        }
        cols += current.toString().trim()
        return cols.filter { it.isNotEmpty() }
    }

    @Test
    fun `presensi_recap - nama tabel kolom affinity dan composite PK cocok dengan entity`() {
        assertTable(
            sql = AppDatabase.CREATE_PRESENSI_RECAP,
            table = "presensi_recap",
            columns = listOf(
                ColumnSpec("kode", "TEXT", notNull = true),
                ColumnSpec("nama", "TEXT", notNull = true),
                ColumnSpec("tm", "INTEGER", notNull = false),
                ColumnSpec("hadir", "INTEGER", notNull = false),
                ColumnSpec("persen", "INTEGER", notNull = false),
            ),
            primaryKey = listOf("kode", "nama"), // composite PK (entity: primaryKeys = [kode, nama])
        )
    }

    @Test
    fun `kampus_info - nama tabel kolom affinity dan primary key cocok dengan entity`() {
        assertTable(
            sql = AppDatabase.CREATE_KAMPUS_INFO,
            table = "kampus_info",
            columns = listOf(
                ColumnSpec("jenis", "TEXT", notNull = true),
                ColumnSpec("dataJson", "TEXT", notNull = true),
                ColumnSpec("updatedAt", "TEXT", notNull = true),
            ),
            primaryKey = listOf("jenis"),
        )
    }

    @Test
    fun `kampus_meta - nama tabel kolom affinity dan primary key cocok dengan entity`() {
        assertTable(
            sql = AppDatabase.CREATE_KAMPUS_META,
            table = "kampus_meta",
            columns = listOf(
                ColumnSpec("id", "INTEGER", notNull = true),
                ColumnSpec("connected", "INTEGER", notNull = true), // Boolean → affinity INTEGER (Room)
                ColumnSpec("lastSyncAt", "TEXT", notNull = false),
            ),
            primaryKey = listOf("id"),
        )
    }
}
