package com.aryariap.forfh.ui.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InfoCardModels — model tampil per jenis info kampus (V1.1 Task 9: kartu per jenis,
 * bukan dump mentah). Builder murni: dataJson baris mentah → model berdesain per jenis,
 * toleran field hilang/null, label dari map penuh (fallback humanize, TIDAK pernah key
 * mentah). Semua nilai hanya dari data (R-17); tanpa em dash (R-02).
 */
class InfoCardModelsTest {

    // ---------- status badge ----------

    @Test
    fun `statusTone - Aktif positif, lainnya netral`() {
        assertEquals(StatusTone.POSITIVE, InfoCardModels.statusTone("Aktif"))
        assertEquals(StatusTone.POSITIVE, InfoCardModels.statusTone("AKTIF"))
        assertEquals(StatusTone.POSITIVE, InfoCardModels.statusTone(" Aktif "))
        assertEquals(StatusTone.POSITIVE, InfoCardModels.statusTone("Aktif (Yudisium)"))
        assertEquals(StatusTone.NEUTRAL, InfoCardModels.statusTone("Cuti"))
        assertEquals(StatusTone.NEUTRAL, InfoCardModels.statusTone("Nonaktif"))
        assertEquals(StatusTone.NEUTRAL, InfoCardModels.statusTone("Lulus"))
    }

    @Test
    fun `statusTone - kosong atau null netral, bukan positif palsu`() {
        assertEquals(StatusTone.NEUTRAL, InfoCardModels.statusTone(null))
        assertEquals(StatusTone.NEUTRAL, InfoCardModels.statusTone(""))
    }

    // ---------- status_mhs ----------

    @Test
    fun `status_mhs - kartu identitas dari baris UPPERCASE_SNAKE`() {
        val model = InfoCardModels.buildInfoCardModel(
            "status_mhs",
            """
            [{"NIM_MHS":"626103051310","NM_PENGGUNA":"Arya Rizky","NM_PROGRAM_STUDI":"Ilmu Hukum",
              "JENJANG":"S1","FAKULTAS":"Fakultas Hukum","ANGKATAN":"2026",
              "STATUS_AKADEMIK":"Aktif","JK":"L","AGAMA":"Islam"}]
            """.trimIndent(),
        ) as IdentityCard
        assertFalse(model.isEmpty)
        assertEquals("626103051310", model.nim)
        assertEquals("Arya Rizky", model.nama)
        assertEquals("Aktif", model.status)
        assertEquals("Ilmu Hukum", model.prodi)
        assertEquals("S1", model.jenjang)
        assertEquals("Fakultas Hukum", model.fakultas)
        assertEquals("2026", model.angkatan)
        assertEquals("L", model.jk)
        assertEquals("Islam", model.agama)
        assertTrue(model.extras.isEmpty())
    }

    @Test
    fun `status_mhs - varian field device asli (THN_ANGKATAN_MHS, NM_JENJANG, NM_STATUS_PENGGUNA)`() {
        val model = InfoCardModels.buildInfoCardModel(
            "status_mhs",
            """
            [{"NIM":"626103051310","THN_ANGKATAN_MHS":"2026","NM_JENJANG":"S1",
              "NM_PROGRAM_STUDI":"Ilmu Hukum","NM_STATUS_PENGGUNA":"Aktif"}]
            """.trimIndent(),
        ) as IdentityCard
        assertEquals("626103051310", model.nim)
        assertEquals("2026", model.angkatan)
        assertEquals("S1", model.jenjang)
        assertEquals("Ilmu Hukum", model.prodi)
        assertEquals("Aktif", model.status)
        assertNull(model.nama)
        assertNull(model.fakultas)
        assertNull(model.jk)
        assertNull(model.agama)
        assertTrue(model.extras.isEmpty())
    }

    @Test
    fun `status_mhs - field hilang ditoleransi, sisa field jadi extras berlabel`() {
        val model = InfoCardModels.buildInfoCardModel(
            "status_mhs",
            """[{"NIM_MHS":"123","SKS_LULUS":"120","NM_IBU_MHS":"Siti"}]""",
        ) as IdentityCard
        assertFalse(model.isEmpty)
        assertEquals("123", model.nim)
        assertNull(model.nama)
        assertEquals(listOf("SKS Lulus" to "120", "Nama Ibu" to "Siti"), model.extras)
    }

    @Test
    fun `status_mhs - lebih dari satu baris - generic (data penuh, tidak dibuang)`() {
        val model = InfoCardModels.buildInfoCardModel(
            "status_mhs",
            """[{"NIM_MHS":"1"},{"NIM_MHS":"2"}]""",
        )
        assertTrue(model is GenericRowModel)
    }

    // ---------- peserta_mk ----------

    @Test
    fun `peserta_mk - baris lowercase device asli (nm_mata_kuliah, kuota, peserta)`() {
        val model = InfoCardModels.buildInfoCardModel(
            "peserta_mk",
            """
            [{"nm_mata_kuliah":"Ilmu Negara","kuota":"65","nama_kelas":"219","peserta":"65",
              "hari":"Senin","ruang":"Ruang Kelas AP - LG02"}]
            """.trimIndent(),
        ) as CourseListModel
        assertEquals(1, model.courses.size)
        val row = model.courses.single()
        assertEquals("Ilmu Negara", row.nama)
        assertEquals("219", row.kelas)
        assertEquals("Senin", row.hari)
        assertEquals("Ruang Kelas AP - LG02", row.ruang)
        assertNull(row.kode)
        assertNull(row.sks)
        assertEquals(listOf("Kuota" to "65", "Peserta" to "65"), row.extras)
    }

    @Test
    fun `peserta_mk - baris UPPERCASE dengan kode, SKS, dosen`() {
        val model = InfoCardModels.buildInfoCardModel(
            "peserta_mk",
            """
            [{"KODE_MK":"FHK25601032","NM_MATA_KULIAH":"Hak Asasi Manusia","NAMA_KELAS":"A-1",
              "SKS":"3","NM_DOSEN":"Dr. X","HARI":"Senin","RUANG":"R 2.1"}]
            """.trimIndent(),
        ) as CourseListModel
        val row = model.courses.single()
        assertEquals("FHK25601032", row.kode)
        assertEquals("Hak Asasi Manusia", row.nama)
        assertEquals("A-1", row.kelas)
        assertEquals("3", row.sks)
        assertEquals("Dr. X", row.dosen)
        assertEquals("Senin", row.hari)
        assertEquals("R 2.1", row.ruang)
    }

    @Test
    fun `peserta_mk - beberapa baris dipertahankan urutannya`() {
        val model = InfoCardModels.buildInfoCardModel(
            "peserta_mk",
            """
            [{"NM_MATA_KULIAH":"Ilmu Negara"},{"NM_MATA_KULIAH":"Hak Asasi Manusia"}]
            """.trimIndent(),
        ) as CourseListModel
        assertEquals(2, model.courses.size)
        assertEquals("Ilmu Negara", model.courses[0].nama)
        assertEquals("Hak Asasi Manusia", model.courses[1].nama)
    }

    // ---------- hist_her ----------

    @Test
    fun `hist_her - nilai huruf, skor, SKS, no ujian, periode`() {
        val model = InfoCardModels.buildInfoCardModel(
            "hist_her",
            """
            [{"NM_MATA_KULIAH":"Hak Asasi Manusia","NILAI":"87","NILAI_HURUF":"A","SKS":"3",
              "NO_UJIAN":"2026/1/12345","NM_SEMESTER":"2026/2027 Ganjil"}]
            """.trimIndent(),
        ) as HerListModel
        val row = model.rows.single()
        assertEquals("Hak Asasi Manusia", row.nama)
        assertEquals("87", row.nilai)
        assertEquals("A", row.nilaiHuruf)
        assertEquals("3", row.sks)
        assertEquals("2026/1/12345", row.noUjian)
        assertEquals("2026/2027 Ganjil", row.periode)
        // fokus nilai = huruf saat ada (huruf lebih informatif dari skor)
        assertEquals("A", row.grade)
    }

    @Test
    fun `hist_her - baris tanpa nilai huruf - skor numerik tetap tampil sebagai fokus`() {
        val model = InfoCardModels.buildInfoCardModel(
            "hist_her",
            """
            [{"NM_MATA_KULIAH":"Hak Asasi Manusia","NILAI":"87","SKS":"3"}]
            """.trimIndent(),
        ) as HerListModel
        val row = model.rows.single()
        assertNull(row.nilaiHuruf)
        assertEquals("87", row.nilai)
        assertEquals("87", row.grade)
    }

    @Test
    fun `hist_her - tanpa nilai sama sekali - grade null, baris tetap punya nama`() {
        val model = InfoCardModels.buildInfoCardModel(
            "hist_her",
            """[{"NM_MATA_KULIAH":"Hak Asasi Manusia"}]""",
        ) as HerListModel
        val row = model.rows.single()
        assertNull(row.grade)
        assertEquals("Hak Asasi Manusia", row.nama)
    }

    // ---------- pembayaran ----------

    @Test
    fun `pembayaran - kegiatan, nominal mentah, tanggal bayar, status, semester`() {
        val model = InfoCardModels.buildInfoCardModel(
            "pembayaran",
            """
            [{"TAHUN_AJARAN":"2026/2027","NM_SEMESTER":"Ganjil","NAMA_STATUS":"Lunas",
              "NOMINAL_BAYAR":"1500000","TGL_BAYAR":"2026-08-15","KEGIATAN":"SPP Semester Ganjil"}]
            """.trimIndent(),
        ) as PaymentListModel
        val row = model.rows.single()
        assertEquals("SPP Semester Ganjil", row.kegiatan)
        assertEquals("2026/2027 Ganjil", row.semester)
        assertEquals("1500000", row.nominal)
        assertEquals("2026-08-15", row.tglBayar)
        assertEquals("Lunas", row.status)
        assertEquals("Rp 1.500.000", InfoFormat.formatRupiah(row.nominal))
    }

    // ---------- kalender_akademik ----------

    @Test
    fun `kalender_akademik - kegiatan + tanggal mulai dan selesai`() {
        val model = InfoCardModels.buildInfoCardModel(
            "kalender_akademik",
            """
            [{"NM_KEGIATAN":"Perkuliahan","TGL_MULAI_JSF":"2026-09-01","TGL_SELESAI_JSF":"2026-12-19"}]
            """.trimIndent(),
        ) as CalendarListModel
        val row = model.rows.single()
        assertEquals("Perkuliahan", row.kegiatan)
        assertEquals("2026-09-01", row.mulai)
        assertEquals("2026-12-19", row.selesai)
    }

    // ---------- summary: masa_studi / sks_aktif / penyerahan_ktm ----------

    @Test
    fun `masa_studi - fakta kunci di headline, sisa field tetap tampil`() {
        val model = InfoCardModels.buildInfoCardModel(
            "masa_studi",
            """
            [{"LAMA_STUDI":"4 Tahun","TGL_MULAI":"2026-09-01","TGL_SELESAI":"2030-08-31",
              "BERLAKU":"2026/2027 Ganjil"}]
            """.trimIndent(),
        ) as SummaryCardModel
        assertEquals(
            listOf("Lama Studi" to "4 Tahun", "Tanggal Mulai" to "2026-09-01", "Tanggal Selesai" to "2030-08-31"),
            model.headline,
        )
        assertEquals(listOf("Berlaku" to "2026/2027 Ganjil"), model.rows)
    }

    @Test
    fun `sks_aktif - SKS tempuh dan jumlah MK di headline`() {
        val model = InfoCardModels.buildInfoCardModel(
            "sks_aktif",
            """[{"SKS_TEMPUH":"20","JUM_MK":"8"}]""",
        ) as SummaryCardModel
        assertEquals(listOf("SKS Tempuh" to "20", "Jumlah MK" to "8"), model.headline)
        assertTrue(model.rows.isEmpty())
    }

    @Test
    fun `penyerahan_ktm - tanggal dan status di headline, label duplikat di-dedupe`() {
        val model = InfoCardModels.buildInfoCardModel(
            "penyerahan_ktm",
            """[{"TGL_VERIFIKASI_PENDIDIKAN":"2026-08-01","TGL":"2026-08-10","STATUS":"Sudah Diambil"}]""",
        ) as SummaryCardModel
        assertEquals(
            listOf("Tgl Verifikasi" to "2026-08-01", "Tanggal" to "2026-08-10", "Status" to "Sudah Diambil"),
            model.headline,
        )
    }

    @Test
    fun `summary - lebih dari satu baris - generic (data penuh)`() {
        val model = InfoCardModels.buildInfoCardModel(
            "sks_aktif",
            """[{"SKS_TEMPUH":"20"},{"SKS_TEMPUH":"21"}]""",
        )
        assertTrue(model is GenericRowModel)
    }

    // ---------- dosen_wali ----------

    @Test
    fun `dosen_wali - nama + fakta lain sebagai baris berlabel`() {
        val model = InfoCardModels.buildInfoCardModel(
            "dosen_wali",
            """
            [{"NM_DOSEN":"Dr. X","NIDN_DOSEN":"0012345678","NIP_DOSEN":"197001012000001001",
              "EMAIL":"x@fh.unair.ac.id"}]
            """.trimIndent(),
        ) as DosenWaliModel
        val d = model.dosen.single()
        assertEquals("Dr. X", d.nama)
        assertEquals(
            listOf("NIDN" to "0012345678", "NIP" to "197001012000001001", "Email" to "x@fh.unair.ac.id"),
            d.facts,
        )
    }

    // ---------- instruksi_tugas ----------

    @Test
    fun `instruksi_tugas - blok kursus dengan section, teks instruksi, dan assignments`() {
        val model = InfoCardModels.buildInfoCardModel(
            "instruksi_tugas",
            """
            [{"courseId":"123","shortname":"FHK25601032",
              "fullname":"2026Ganjil - FHK25601032 - Hak Asasi Manusia",
              "sections":[{"sectionId":"1","sectionName":"Umum",
                "summary":"Perkuliahan dimulai 1 September 2026.\nRuang akan diumumkan kemudian.",
                "assignments":["UTS HAM"]}]}]
            """.trimIndent(),
        ) as InstructionBlockModel
        val course = model.courses.single()
        assertEquals("FHK25601032", course.kode)
        assertEquals("2026Ganjil - FHK25601032 - Hak Asasi Manusia", course.nama)
        val section = course.sections.single()
        assertEquals("Umum", section.nama)
        assertEquals("Perkuliahan dimulai 1 September 2026.\nRuang akan diumumkan kemudian.", section.teks)
        assertEquals(listOf("UTS HAM"), section.assignments)
    }

    @Test
    fun `instruksi_tugas - assignments nama aktivitas terbaca dari web (fix review)`() {
        val model = InfoCardModels.buildInfoCardModel(
            "instruksi_tugas",
            """
            [{"shortname":"FHK25601032","fullname":"Kursus HAM",
              "sections":[{"sectionName":"Tugas","summary":"Instruksi lengkap",
                "assignments":["Pengumpulan Tugas Resume Buku PIH Prof Peter Bab I","Assessment HAM"]}]}]
            """.trimIndent(),
        ) as InstructionBlockModel
        assertEquals(
            listOf("Pengumpulan Tugas Resume Buku PIH Prof Peter Bab I", "Assessment HAM"),
            model.courses.single().sections.single().assignments,
        )
    }

    @Test
    fun `instruksi_tugas - section dengan assignments tapi tanpa teks tetap tampil`() {
        val model = InfoCardModels.buildInfoCardModel(
            "instruksi_tugas",
            """
            [{"shortname":"A","fullname":"Kursus A",
              "sections":[{"sectionName":"Tugas","summary":"","assignments":["UTS","UAS"]}]}]
            """.trimIndent(),
        ) as InstructionBlockModel
        val section = model.courses.single().sections.single()
        assertNull(section.teks)
        assertEquals(listOf("UTS", "UAS"), section.assignments)
    }

    @Test
    fun `instruksi_tugas - section tanpa teks dan tanpa assignments dilewati`() {
        val model = InfoCardModels.buildInfoCardModel(
            "instruksi_tugas",
            """
            [{"shortname":"A","fullname":"Kursus A",
              "sections":[{"sectionName":"Kosong","summary":""},{"sectionName":"Isi","summary":"Teks"}]},
             {"shortname":"B","fullname":"Kursus B","sections":[]}]
            """.trimIndent(),
        ) as InstructionBlockModel
        assertEquals(1, model.courses.size)
        assertEquals(1, model.courses.single().sections.size)
        assertEquals("Isi", model.courses.single().sections.single().nama)
    }

    // ---------- fallback generic ----------

    @Test
    fun `jenis tak dikenal - kartu generic label map penuh + humanize, bukan key mentah`() {
        val model = InfoCardModels.buildInfoCardModel(
            "jenis_baru",
            """[{"NM_IBU_MHS":"Siti","ASING_BARU":"?"}]""",
        ) as GenericRowModel
        val map = model.rows.blocks.single().rows.toMap()
        assertEquals("Siti", map["Nama Ibu"])
        assertEquals("?", map["Asing Baru"])
    }

    // ---------- kosong / rusak ----------

    @Test
    fun `dataJson kosong - model kosong tanpa crash`() {
        assertTrue(InfoCardModels.buildInfoCardModel("status_mhs", "[]").isEmpty)
        assertTrue(InfoCardModels.buildInfoCardModel("peserta_mk", "[]").isEmpty)
        assertTrue(InfoCardModels.buildInfoCardModel("instruksi_tugas", "[]").isEmpty)
        assertTrue(InfoCardModels.buildInfoCardModel("jenis_x", "[]").isEmpty)
    }

    @Test
    fun `dataJson bukan array atau rusak - model kosong tanpa crash`() {
        assertTrue(InfoCardModels.buildInfoCardModel("status_mhs", "{}").isEmpty)
        assertTrue(InfoCardModels.buildInfoCardModel("status_mhs", "bukan-json").isEmpty)
        assertTrue(InfoCardModels.buildInfoCardModel("status_mhs", "").isEmpty)
    }

    @Test
    fun `baris non-object dilewati`() {
        val model = InfoCardModels.buildInfoCardModel(
            "peserta_mk",
            """[null,"teks",5,{"NM_MATA_KULIAH":"Ilmu Negara"}]""",
        ) as CourseListModel
        assertEquals(1, model.courses.size)
    }
}
