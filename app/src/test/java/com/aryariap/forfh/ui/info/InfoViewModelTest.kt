package com.aryariap.forfh.ui.info

import com.aryariap.forfh.data.db.KampusInfoDao
import com.aryariap.forfh.data.db.KampusInfoEntity
import com.aryariap.forfh.data.db.KampusInfoSnapshot
import com.aryariap.forfh.data.db.KampusMetaEntity
import com.aryariap.forfh.data.db.PresensiRecapEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * InfoViewModel — layar Info (Task 8). Pola NextUpViewModelTest: Dispatchers.setMain dengan
 * StandardTestDispatcher, DAO fake berbasis MutableStateFlow (Room Flow bisa di-emit ulang),
 * background dispatcher yang sama di-inject ke ViewModel → semua deterministik tanpa Robolectric.
 */
class InfoViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- fakes ----------

    private class FakeKampusInfoDao(
        initialPresensi: List<PresensiRecapEntity> = emptyList(),
        initialInfo: List<KampusInfoEntity> = emptyList(),
        initialMeta: KampusMetaEntity? = null,
    ) : KampusInfoDao {
        val presensi = MutableStateFlow(initialPresensi)
        val info = MutableStateFlow(initialInfo)
        val meta = MutableStateFlow(initialMeta)
        override fun getPresensiRecap(): Flow<List<PresensiRecapEntity>> = presensi
        override fun getKampusInfo(): Flow<List<KampusInfoEntity>> = info
        override fun getMeta(): Flow<KampusMetaEntity?> = meta
        override suspend fun getMetaOnce(): KampusMetaEntity? = meta.value
        override suspend fun clearPresensiRecap() { presensi.value = emptyList() }
        override suspend fun clearKampusInfo() { info.value = emptyList() }
        override suspend fun clearKampusMeta() { meta.value = null }
        override suspend fun insertPresensiRecap(items: List<PresensiRecapEntity>) { presensi.value = items }
        override suspend fun insertKampusInfo(items: List<KampusInfoEntity>) { info.value = items }
        override suspend fun insertMeta(meta: KampusMetaEntity) { this.meta.value = meta }
        override suspend fun saveSnapshot(snapshot: KampusInfoSnapshot) {
            insertMeta(KampusMetaEntity(1, snapshot.connected, snapshot.lastSyncAt))
            if (snapshot.connected) {
                clearPresensiRecap()
                insertPresensiRecap(snapshot.presensi)
                clearKampusInfo()
                insertKampusInfo(snapshot.info)
            } else {
                clearPresensiRecap()
                clearKampusInfo()
            }
        }
    }

    private class FakeInfoContainer(
        override val kampusInfoDao: KampusInfoDao,
        override val syncActivity: Flow<SyncActivity> = MutableStateFlow(SyncActivity.IDLE),
        override val lastSyncStatus: Flow<String> = MutableStateFlow(""),
        var enqueueCalls: Int = 0,
    ) : InfoContainer {
        override val enqueueSync: () -> Unit = { enqueueCalls++ }
    }

    private fun vm(container: InfoContainer) = InfoViewModel(container, background = dispatcher)

    private fun presensi(kode: String, nama: String, tm: Int, hadir: Int, persen: Int) =
        PresensiRecapEntity(kode, nama, tm, hadir, persen)

    private val statusMhsJson = """
        [{"NIM_MHS":"626103051310","NM_PENGGUNA":"Arya Rizky","NM_PROGRAM_STUDI":"Ilmu Hukum",
          "JENJANG":"S1","FAKULTAS":"Fakultas Hukum","ANGKATAN":"2026",
          "STATUS_AKADEMIK":"Aktif","JK":"L","AGAMA":"Islam"}]
    """.trimIndent()

    // ---------- tests ----------

    @Test
    fun `state dari dao - presensi, kartu info, dan meta terpetakan`() = runTest(dispatcher) {
        val dao = FakeKampusInfoDao(
            initialPresensi = listOf(
                presensi("FHK25601032", "Hak Asasi Manusia", 14, 13, 93),
                presensi("FHK25601033", "Hukum Acara Pidana", 14, 7, 50),
            ),
            initialInfo = listOf(
                KampusInfoEntity("status_mhs", statusMhsJson, "2026-08-17T04:05:06.123Z"),
            ),
            initialMeta = KampusMetaEntity(1, connected = true, lastSyncAt = "2026-08-17T04:05:06.123Z"),
        )
        val viewModel = vm(FakeInfoContainer(dao))

        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(2, s.presensi.size)
        assertEquals("Hak Asasi Manusia", s.presensi[0].nama)
        assertEquals("FHK25601032", s.presensi[0].kode)
        assertEquals(14, s.presensi[0].tm)
        assertEquals(13, s.presensi[0].hadir)
        assertEquals(93, s.presensi[0].persen)

        assertEquals(1, s.cards.size)
        val card = s.cards.single()
        assertEquals("status_mhs", card.jenis)
        assertEquals("Status Mahasiswa", card.title)
        val identity = card.model as IdentityCard
        assertEquals("626103051310", identity.nim)
        assertEquals("Arya Rizky", identity.nama)
        assertEquals("Aktif", identity.status)
        assertEquals("17 Agu 2026, 11:05", card.updatedAt) // UTC 04:05 → WIB 11:05

        assertEquals(true, s.connected)
        assertEquals("2026-08-17T04:05:06.123Z", s.kampusLastSyncAt)
    }

    @Test
    fun `dao kosong - state kosong tanpa meta`() = runTest(dispatcher) {
        val viewModel = vm(FakeInfoContainer(FakeKampusInfoDao()))

        advanceUntilIdle()

        val s = viewModel.state.value
        assertTrue(s.loaded)
        assertTrue(s.presensi.isEmpty())
        assertTrue(s.cards.isEmpty())
        assertNull(s.connected)
        assertNull(s.kampusLastSyncAt)
    }

    @Test
    fun `loaded - false sebelum emisi Room pertama, true setelahnya (guard frame pertama)`() = runTest(dispatcher) {
        val dao = FakeKampusInfoDao(initialMeta = KampusMetaEntity(1, connected = false, lastSyncAt = null))
        val viewModel = vm(FakeInfoContainer(dao))

        // Collector belum jalan di test scheduler → belum ada emisi → JANGAN render state
        // terminal (user yang terputus tidak boleh sempat melihat "Belum ada data").
        assertFalse(viewModel.state.value.loaded)

        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertEquals(false, viewModel.state.value.connected)
    }

    @Test
    fun `dao di-update - state ikut berubah (Room flow)`() = runTest(dispatcher) {
        val dao = FakeKampusInfoDao()
        val viewModel = vm(FakeInfoContainer(dao))

        advanceUntilIdle()
        assertTrue(viewModel.state.value.presensi.isEmpty())

        dao.presensi.value = listOf(presensi("FHK25601032", "Hak Asasi Manusia", 14, 13, 93))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.presensi.size)
        assertEquals("Hak Asasi Manusia", viewModel.state.value.presensi.single().nama)
    }

    @Test
    fun `syncActivity - IDLE, QUEUED saat menunggu jaringan, RUNNING saat berjalan`() = runTest(dispatcher) {
        val activity = MutableStateFlow(SyncActivity.IDLE)
        val viewModel = vm(FakeInfoContainer(FakeKampusInfoDao(), syncActivity = activity))

        advanceUntilIdle()
        assertEquals(SyncActivity.IDLE, viewModel.state.value.syncActivity)

        activity.value = SyncActivity.QUEUED
        advanceUntilIdle()
        assertEquals(SyncActivity.QUEUED, viewModel.state.value.syncActivity)

        activity.value = SyncActivity.RUNNING
        advanceUntilIdle()
        assertEquals(SyncActivity.RUNNING, viewModel.state.value.syncActivity)

        activity.value = SyncActivity.IDLE
        advanceUntilIdle()
        assertEquals(SyncActivity.IDLE, viewModel.state.value.syncActivity)
    }

    @Test
    fun `error - true saat status sync terakhir error, false saat ok`() = runTest(dispatcher) {
        val status = MutableStateFlow("")
        val viewModel = vm(FakeInfoContainer(FakeKampusInfoDao(), lastSyncStatus = status))

        advanceUntilIdle()
        assertFalse(viewModel.state.value.error)

        status.value = "error"
        advanceUntilIdle()
        assertTrue(viewModel.state.value.error)
        assertEquals("error", viewModel.state.value.lastSyncStatus)

        status.value = "ok"
        advanceUntilIdle()
        assertFalse(viewModel.state.value.error)
    }

    @Test
    fun `syncNow - memanggil enqueueSync (pola PengaturanViewModel)`() = runTest(dispatcher) {
        val container = FakeInfoContainer(FakeKampusInfoDao())
        val viewModel = vm(container)

        viewModel.syncNow()
        viewModel.syncNow()

        assertEquals(2, container.enqueueCalls)
    }

    @Test
    fun `connected false - state memberitahu layar utk state putus`() = runTest(dispatcher) {
        val dao = FakeKampusInfoDao(
            initialMeta = KampusMetaEntity(1, connected = false, lastSyncAt = "2026-08-17T04:05:06.123Z"),
        )
        val viewModel = vm(FakeInfoContainer(dao))

        advanceUntilIdle()

        assertFalse(viewModel.state.value.connected!!)
        assertEquals("2026-08-17T04:05:06.123Z", viewModel.state.value.kampusLastSyncAt)
    }
}
