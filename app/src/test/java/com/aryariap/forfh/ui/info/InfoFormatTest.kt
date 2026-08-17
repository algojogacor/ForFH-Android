package com.aryariap.forfh.ui.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InfoFormat — helper murni layar Info (Task 8): label Indonesia utk field UPPERCASE_SNAKE
 * Kampus Kita, judul per jenis, parse dataJson mentah → blok label:nilai, format baris
 * presensi, format updatedAt. Semua angka hanya dari data (R-17); string tanpa em dash (R-02).
 */
class InfoFormatTest {

    // ---------- label map ----------

    @Test
    fun `fieldLabel - field dikenal mendapat label Indonesia`() {
        assertEquals("NIM", InfoFormat.fieldLabel("NIM_MHS"))
        assertEquals("Nama", InfoFormat.fieldLabel("NM_PENGGUNA"))
        assertEquals("Prodi", InfoFormat.fieldLabel("NM_PROGRAM_STUDI"))
        assertEquals("Jenis Kelamin", InfoFormat.fieldLabel("JK"))
        assertEquals("Status Akademik", InfoFormat.fieldLabel("STATUS_AKADEMIK"))
        assertEquals("Nama MK", InfoFormat.fieldLabel("NM_MATA_KULIAH"))
        assertEquals("Kode MK", InfoFormat.fieldLabel("KODE_MK"))
        assertEquals("Kelas", InfoFormat.fieldLabel("NAMA_KELAS"))
        assertEquals("Dosen", InfoFormat.fieldLabel("NM_DOSEN"))
        assertEquals("Kegiatan", InfoFormat.fieldLabel("KEGIATAN"))
        assertEquals("Tanggal Mulai", InfoFormat.fieldLabel("TGL_MULAI"))
        assertEquals("Tanggal Selesai", InfoFormat.fieldLabel("TGL_SELESAI"))
    }

    @Test
    fun `fieldLabel - field tak dikenal di-humanisasi Title Case, bukan key mentah`() {
        assertEquals("Asing Baru", InfoFormat.fieldLabel("ASING_BARU"))
        assertEquals("Nm Mata Kuliah 2", InfoFormat.fieldLabel("NM_MATA_KULIAH_2"))
    }

    @Test
    fun `fieldLabel - kunci lowercase dari respons asli ikut ter-label`() {
        assertEquals("Nama MK", InfoFormat.fieldLabel("nm_mata_kuliah"))
        assertEquals("Kelas", InfoFormat.fieldLabel("nama_kelas"))
        assertEquals("Hari", InfoFormat.fieldLabel("hari"))
        assertEquals("Ruangan", InfoFormat.fieldLabel("ruang"))
        assertEquals("Kuota", InfoFormat.fieldLabel("kuota"))
        assertEquals("Peserta", InfoFormat.fieldLabel("peserta"))
        assertEquals("Angkatan", InfoFormat.fieldLabel("THN_ANGKATAN_MHS"))
        assertEquals("Jenjang", InfoFormat.fieldLabel("NM_JENJANG"))
        assertEquals("Status Akademik", InfoFormat.fieldLabel("NM_STATUS_PENGGUNA"))
    }

    @Test
    fun `fieldLabel - seluruh map web (campusMeta) ter-salin`() {
        assertEquals("Nama", InfoFormat.fieldLabel("NAMA"))
        assertEquals("Nama MK", InfoFormat.fieldLabel("NAMA_MK"))
        assertEquals("Kode MK", InfoFormat.fieldLabel("KD_MATA_KULIAH"))
        assertEquals("Kode", InfoFormat.fieldLabel("KODE"))
        assertEquals("SKS", InfoFormat.fieldLabel("KREDIT_SEMESTER"))
        assertEquals("Kelas", InfoFormat.fieldLabel("KELAS"))
        assertEquals("Dosen", InfoFormat.fieldLabel("DOSEN"))
        assertEquals("Dosen Wali", InfoFormat.fieldLabel("DOSEN_WALI"))
        assertEquals("Nilai", InfoFormat.fieldLabel("NILAI_HURUF"))
        assertEquals("Skor", InfoFormat.fieldLabel("NILAI"))
        assertEquals("Semester", InfoFormat.fieldLabel("ID_SEMESTER"))
        assertEquals("Tahun Ajaran", InfoFormat.fieldLabel("TAHUN_AJARAN"))
        assertEquals("Periode", InfoFormat.fieldLabel("NM_SEMESTER"))
        assertEquals("Prodi", InfoFormat.fieldLabel("PRODI"))
        assertEquals("Jumlah MK", InfoFormat.fieldLabel("JUM_MK"))
        assertEquals("SKS Tempuh", InfoFormat.fieldLabel("SKS_TEMPUH"))
        assertEquals("Tanggal", InfoFormat.fieldLabel("TANGGAL"))
        assertEquals("Tanggal Awal", InfoFormat.fieldLabel("TANGGAL_AWAL"))
        assertEquals("Berlaku", InfoFormat.fieldLabel("BERLAKU"))
        assertEquals("Jam", InfoFormat.fieldLabel("JAM"))
        assertEquals("Ruangan", InfoFormat.fieldLabel("RUANGAN"))
        assertEquals("Status", InfoFormat.fieldLabel("NAMA_STATUS"))
        assertEquals("Keterangan", InfoFormat.fieldLabel("KET"))
        assertEquals("Uraian", InfoFormat.fieldLabel("URAIAN"))
        assertEquals("Nominal", InfoFormat.fieldLabel("NOMINAL"))
        assertEquals("Jumlah", InfoFormat.fieldLabel("JUMLAH"))
        assertEquals("Agenda", InfoFormat.fieldLabel("AGENDA"))
        assertEquals("Kegiatan", InfoFormat.fieldLabel("NM_KEGIATAN"))
        assertEquals("Tanggal Mulai", InfoFormat.fieldLabel("TGL_MULAI_JSF"))
        assertEquals("Tanggal Selesai", InfoFormat.fieldLabel("TGL_SELESAI_JSF"))
        assertEquals("Tanggal Bayar", InfoFormat.fieldLabel("TGL_BAYAR"))
        assertEquals("Nominal Dibayar", InfoFormat.fieldLabel("NOMINAL_BAYAR"))
        assertEquals("Lama Studi", InfoFormat.fieldLabel("LAMA_STUDI"))
        assertEquals("No. Ujian", InfoFormat.fieldLabel("NO_UJIAN"))
        assertEquals("Tgl Verifikasi", InfoFormat.fieldLabel("TGL_VERIFIKASI_PENDIDIKAN"))
        assertEquals("Nama Dosen", InfoFormat.fieldLabel("NAMA_DOSEN"))
        assertEquals("NIP", InfoFormat.fieldLabel("NIP_DOSEN"))
        assertEquals("NIDN", InfoFormat.fieldLabel("NIDN_DOSEN"))
        assertEquals("Email", InfoFormat.fieldLabel("EMAIL"))
        assertEquals("Hadir", InfoFormat.fieldLabel("JML_HADIR"))
        assertEquals("Total TM", InfoFormat.fieldLabel("TOTAL_TM"))
        assertEquals("Persen", InfoFormat.fieldLabel("PERSEN"))
    }

    @Test
    fun `jenisTitle - jenis dikenal mendapat judul Indonesia`() {
        assertEquals("Status Mahasiswa", InfoFormat.jenisTitle("status_mhs"))
        assertEquals("Kalender Akademik", InfoFormat.jenisTitle("kalender_akademik"))
        assertEquals("Peserta Mata Kuliah", InfoFormat.jenisTitle("peserta_mk"))
        assertEquals("Riwayat HER", InfoFormat.jenisTitle("hist_her"))
        assertEquals("Instruksi Tugas", InfoFormat.jenisTitle("instruksi_tugas"))
    }

    @Test
    fun `jenisTitle - jenis baru tak dikenal di-humanisasi Title Case`() {
        assertEquals("Jenis Baru", InfoFormat.jenisTitle("jenis_baru"))
    }

    // ---------- humanizeField ----------

    @Test
    fun `humanizeField - UPPERCASE_SNAKE jadi Title Case`() {
        assertEquals("Asing Baru", InfoFormat.humanizeField("ASING_BARU"))
        assertEquals("Thn Angkatan Mhs", InfoFormat.humanizeField("THN_ANGKATAN_MHS"))
    }

    @Test
    fun `humanizeField - lowercase snake jadi Title Case`() {
        assertEquals("Nm Mata Kuliah", InfoFormat.humanizeField("nm_mata_kuliah"))
        assertEquals("Sudah Siap", InfoFormat.humanizeField("sudah_siap"))
    }

    @Test
    fun `humanizeField - kata tunggal`() {
        assertEquals("Asing", InfoFormat.humanizeField("ASING"))
        assertEquals("Asing", InfoFormat.humanizeField("asing"))
        assertEquals("Hari2", InfoFormat.humanizeField("HARI2"))
    }

    @Test
    fun `humanizeField - camelCase dipecah`() {
        assertEquals("Course Id", InfoFormat.humanizeField("courseId"))
        assertEquals("Kode Mk", InfoFormat.humanizeField("kodeMk"))
    }

    @Test
    fun `humanizeField - kosong dikembalikan apa adanya`() {
        assertEquals("", InfoFormat.humanizeField(""))
        assertEquals(" ", InfoFormat.humanizeField(" "))
    }

    // ---------- formatRupiah ----------

    @Test
    fun `formatRupiah - angka bulat dengan pemisah ribuan titik`() {
        assertEquals("Rp 1.234.567", InfoFormat.formatRupiah("1234567"))
        assertEquals("Rp 15.000", InfoFormat.formatRupiah("15000"))
        assertEquals("Rp 1.000.000.000", InfoFormat.formatRupiah("1000000000"))
        assertEquals("Rp 0", InfoFormat.formatRupiah("0"))
    }

    @Test
    fun `formatRupiah - desimal dibulatkan ke rupiah penuh`() {
        assertEquals("Rp 1.234.567", InfoFormat.formatRupiah("1234567.4"))
        assertEquals("Rp 1.234.568", InfoFormat.formatRupiah("1234567.5"))
    }

    @Test
    fun `formatRupiah - format grup Indonesia diterima`() {
        assertEquals("Rp 1.234.567", InfoFormat.formatRupiah("1.234.567"))
        assertEquals("Rp 5.000", InfoFormat.formatRupiah("5.000"))
    }

    @Test
    fun `formatRupiah - bukan angka - null (baris dilewati, bukan nilai palsu)`() {
        assertNull(InfoFormat.formatRupiah(null))
        assertNull(InfoFormat.formatRupiah(""))
        assertNull(InfoFormat.formatRupiah("abc"))
        assertNull(InfoFormat.formatRupiah("-5000"))
        assertNull(InfoFormat.formatRupiah("Rp 5000"))
    }

    // ---------- formatIsoDate ----------

    @Test
    fun `formatIsoDate - ISO lokal jadi tanggal Indonesia`() {
        assertEquals("24 Agu 2026", InfoFormat.formatIsoDate("2026-08-24"))
        assertEquals("1 Sep 2026", InfoFormat.formatIsoDate("2026-09-01"))
    }

    @Test
    fun `formatIsoDate - ISO lokal dengan offset (format kalender akademik) jadi tanggal dan jam`() {
        assertEquals("3 Agu 2026 08:00", InfoFormat.formatIsoDate("2026-08-03T08:00:00+07:00"))
        assertEquals("2 Sep 2026 16:00", InfoFormat.formatIsoDate("2026-09-02T16:00:00+07:00"))
    }

    @Test
    fun `formatIsoDate - offset tanpa detik tetap diparse`() {
        assertEquals("3 Agu 2026 08:00", InfoFormat.formatIsoDate("2026-08-03T08:00+07:00"))
    }

    @Test
    fun `formatIsoDate - tanggal polos dengan hari dua digit tidak di-pad`() {
        assertEquals("12 Okt 2026", InfoFormat.formatIsoDate("2026-10-12"))
    }

    @Test
    fun `formatIsoDate - null dan blank - null`() {
        assertNull(InfoFormat.formatIsoDate(null))
        assertNull(InfoFormat.formatIsoDate(""))
        assertNull(InfoFormat.formatIsoDate("   "))
    }

    @Test
    fun `formatIsoDate - tak bisa diparse ditampilkan apa adanya (jujur)`() {
        assertEquals("17-08-2026", InfoFormat.formatIsoDate("17-08-2026"))
        assertEquals("2026-08-03T08:00:00+08:00x", InfoFormat.formatIsoDate("2026-08-03T08:00:00+08:00x"))
    }

    // ---------- baris presensi ----------

    @Test
    fun `formatPresensi - semua angka tersedia`() {
        assertEquals("Hadir 13 dari 14 pertemuan (93%)", InfoFormat.formatPresensi(14, 13, 93))
    }

    @Test
    fun `formatPresensi - tanpa persen`() {
        assertEquals("Hadir 13 dari 14 pertemuan", InfoFormat.formatPresensi(14, 13, null))
    }

    @Test
    fun `formatPresensi - sebagian tersedia`() {
        assertEquals("Hadir 13", InfoFormat.formatPresensi(null, 13, null))
        assertEquals("14 pertemuan", InfoFormat.formatPresensi(14, null, null))
        assertEquals("93%", InfoFormat.formatPresensi(null, null, 93))
    }

    @Test
    fun `formatPresensi - tidak ada angka - teks jujur, bukan angka palsu`() {
        assertEquals("Kehadiran belum tersedia", InfoFormat.formatPresensi(null, null, null))
    }

    @Test
    fun `formatPresensi - nol tetap ditampilkan (data nyata)`() {
        assertEquals("Hadir 0 dari 14 pertemuan (0%)", InfoFormat.formatPresensi(14, 0, 0))
    }

    // ---------- updatedAt ----------

    @Test
    fun `formatUpdatedAt - ISO Z diparse ke WIB (UTC+7)`() {
        assertEquals("17 Agu 2026, 11:05", InfoFormat.formatUpdatedAt("2026-08-17T04:05:06.123Z"))
    }

    @Test
    fun `formatUpdatedAt - tak bisa diparse - null (jangan ditampilkan)`() {
        assertNull(InfoFormat.formatUpdatedAt(""))
        assertNull(InfoFormat.formatUpdatedAt("bukan-iso"))
        assertNull(InfoFormat.formatUpdatedAt("2026-08-17"))
    }

    @Test
    fun `kampusUpdatedText - footer umur data kampus, tanpa em dash`() {
        assertEquals(
            "Info terakhir diperbarui 17 Agu 2026, 11:05",
            InfoFormat.kampusUpdatedText("2026-08-17T04:05:06.123Z"),
        )
        assertEquals("Info kampus belum pernah diperbarui", InfoFormat.kampusUpdatedText(null))
        assertEquals("Info kampus belum pernah diperbarui", InfoFormat.kampusUpdatedText("rusak"))
    }

    // ---------- parse dataJson ----------

    @Test
    fun `kampusRows - status_mhs sample - satu blok, label Indonesia`() {
        val rows = InfoFormat.kampusRows(
            """
            [{"NIM_MHS":"626103051310","NM_PENGGUNA":"Arya Rizky","NM_PROGRAM_STUDI":"Ilmu Hukum",
              "JENJANG":"S1","FAKULTAS":"Fakultas Hukum","ANGKATAN":"2026",
              "STATUS_AKADEMIK":"Aktif","JK":"L","AGAMA":"Islam"}]
            """.trimIndent(),
        )
        assertEquals(1, rows.blocks.size)
        assertEquals(0, rows.skippedRecords)
        val map = rows.blocks.single().rows.toMap()
        assertEquals(9, map.size)
        assertEquals("626103051310", map["NIM"])
        assertEquals("Arya Rizky", map["Nama"])
        assertEquals("Ilmu Hukum", map["Prodi"])
        assertEquals("S1", map["Jenjang"])
        assertEquals("Fakultas Hukum", map["Fakultas"])
        assertEquals("2026", map["Angkatan"])
        assertEquals("Aktif", map["Status Akademik"])
        assertEquals("L", map["Jenis Kelamin"])
        assertEquals("Islam", map["Agama"])
    }

    @Test
    fun `kampusRows - beberapa record - satu blok per record, urutan dipertahankan`() {
        val rows = InfoFormat.kampusRows(
            """
            [{"KODE_MK":"FHK25601032","NM_MATA_KULIAH":"Hak Asasi Manusia","NAMA_KELAS":"A-1","SKS":"3","NM_DOSEN":"Dr. X"},
             {"KODE_MK":"FHK25601033","NM_MATA_KULIAH":"Hukum Acara Pidana","NAMA_KELAS":"A-1","SKS":"3"}]
            """.trimIndent(),
        )
        assertEquals(2, rows.blocks.size)
        assertEquals("FHK25601032", rows.blocks[0].rows.first().second)
        assertEquals("Hak Asasi Manusia", rows.blocks[0].rows[1].second)
        assertEquals("FHK25601033", rows.blocks[1].rows.first().second)
    }

    @Test
    fun `kampusRows - nilai null dan kosong dilewati, key tak dikenal di-humanisasi`() {
        val rows = InfoFormat.kampusRows(
            """[{"TGL_SELESAI":null,"KEGIATAN":"","TGL_MULAI":"2026-08-24","ASING":"?"}]""",
        )
        assertEquals(1, rows.blocks.size)
        val map = rows.blocks.single().rows.toMap()
        assertEquals(2, map.size)
        assertEquals("2026-08-24", map["Tanggal Mulai"])
        assertEquals("?", map["Asing"])
    }

    @Test
    fun `kampusRows - record non-object dilewati tanpa crash`() {
        val rows = InfoFormat.kampusRows("""[null,"teks",5,{"A":"B"}]""")
        assertEquals(1, rows.blocks.size)
        assertEquals("B", rows.blocks.single().rows.single().second)
    }

    @Test
    fun `kampusRows - nilai objek ditampilkan sebagai JSON mentah`() {
        val rows = InfoFormat.kampusRows("""[{"DATA":{"a":1,"b":true}}]""")
        val value = rows.blocks.single().rows.single().second
        assertTrue(value.contains("\"a\":1"))
        assertTrue(value.contains("\"b\":true"))
    }

    @Test
    fun `kampusRows - lebih dari maxRecords - dipangkas dengan catatan jumlah`() {
        val json = buildString {
            append('[')
            repeat(30) { i ->
                if (i > 0) append(',')
                append("""{"KODE_MK":"MK$i","NM_MATA_KULIAH":"MK $i"}""")
            }
            append(']')
        }
        val rows = InfoFormat.kampusRows(json, maxRecords = 25)
        assertEquals(25, rows.blocks.size)
        assertEquals(5, rows.skippedRecords)
    }

    @Test
    fun `kampusRows - dataJson kosong, bukan array, atau rusak - hasil kosong tanpa crash`() {
        assertEquals(0, InfoFormat.kampusRows("[]").blocks.size)
        assertEquals(0, InfoFormat.kampusRows("{}").blocks.size)
        assertEquals(0, InfoFormat.kampusRows("bukan-json").blocks.size)
        assertEquals(0, InfoFormat.kampusRows("").blocks.size)
    }
}
