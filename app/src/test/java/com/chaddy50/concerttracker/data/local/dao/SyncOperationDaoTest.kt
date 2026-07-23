package com.chaddy50.concerttracker.data.local.dao

import com.chaddy50.concerttracker.data.enum.SyncEntityType
import com.chaddy50.concerttracker.data.enum.SyncOperationType
import com.chaddy50.concerttracker.data.local.ConcertTrackerDatabase
import com.chaddy50.concerttracker.data.local.entity.SyncOperationEntity
import com.chaddy50.concerttracker.data.local.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncOperationDaoTest {

    private lateinit var db: ConcertTrackerDatabase
    private lateinit var dao: SyncOperationDao

    private fun op(
        entityType: SyncEntityType = SyncEntityType.PERFORMANCE,
        syncOperationType: SyncOperationType = SyncOperationType.CREATE,
        entityLocalId: String = "e1",
        payloadJson: String? = """{"k":"v"}""",
    ) = SyncOperationEntity(
        entityType = entityType.name,
        operationType = syncOperationType.name,
        entityId = entityLocalId,
        payloadJson = payloadJson,
        createdAt = "2026-01-01T00:00:00Z"
    )

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.syncOperationDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `enqueue autogenerates a positive id and defaults attemptCount and lastError`() = runTest {
        val id = dao.enqueue(op())
        assertTrue(id > 0)
        val stored = dao.getAllOrdered().single()
        assertEquals(0, stored.attemptCount)
        assertNull(stored.lastError)
    }

    @Test
    fun `payloadJson round-trips verbatim and null for DELETE`() = runTest {
        val nested = """{"a":{"b":"c \"quoted\" ünïcode"}}"""
        dao.enqueue(op(payloadJson = nested))
        dao.enqueue(op(syncOperationType = SyncOperationType.DELETE, payloadJson = null))
        val rows = dao.getAllOrdered()
        assertEquals(nested, rows[0].payloadJson)
        assertNull(rows[1].payloadJson)
    }

    @Test
    fun `getAllOrdered returns ops in ascending-id insertion order`() = runTest {
        dao.enqueue(op(entityLocalId = "a"))
        dao.enqueue(op(entityLocalId = "b"))
        dao.enqueue(op(entityLocalId = "c"))
        assertEquals(listOf("a", "b", "c"), dao.getAllOrdered().map { it.entityId })
    }

    @Test
    fun `observePendingCount tracks enqueue and delete`() = runTest {
        assertEquals(0, dao.observePendingCount().first())
        val id = dao.enqueue(op())
        dao.enqueue(op(entityLocalId = "e2"))
        assertEquals(2, dao.observePendingCount().first())
        dao.delete(id)
        assertEquals(1, dao.observePendingCount().first())
    }

    @Test
    fun `incrementAttempt and markFailed update only the target and do not delete`() = runTest {
        val id = dao.enqueue(op())
        val other = dao.enqueue(op(entityLocalId = "e2"))
        dao.incrementAttempt(id)
        dao.incrementAttempt(id)
        dao.markFailed(id, "boom")
        val rows = dao.getAllOrdered().associateBy { it.id }
        assertEquals(2, rows.getValue(id).attemptCount)
        assertEquals("boom", rows.getValue(id).lastError)
        assertEquals(0, rows.getValue(other).attemptCount)
        assertNull(rows.getValue(other).lastError)
        assertEquals(2, dao.getAllOrdered().size) // markFailed keeps the row
    }

    @Test
    fun `getById returns the matching row and null for an unknown id`() = runTest {
        val id = dao.enqueue(op(entityLocalId = "target"))
        dao.incrementAttempt(id)
        dao.markFailed(id, "boom")

        val found = dao.getById(id)
        assertEquals("target", found?.entityId)
        assertEquals(1, found?.attemptCount)
        assertEquals("boom", found?.lastError)
        assertNull(dao.getById(id + 999))
    }

    @Test
    fun `resetFailures zeroes attemptCount and clears lastError only on failed rows`() = runTest {
        val failed = dao.enqueue(op(entityLocalId = "failed"))
        dao.incrementAttempt(failed)
        dao.incrementAttempt(failed)
        dao.markFailed(failed, "boom")
        val clean = dao.enqueue(op(entityLocalId = "clean"))

        dao.resetFailures()

        val rows = dao.getAllOrdered().associateBy { it.id }
        assertEquals(0, rows.getValue(failed).attemptCount)
        assertNull(rows.getValue(failed).lastError)
        assertEquals(0, rows.getValue(clean).attemptCount)
        assertNull(rows.getValue(clean).lastError)
        assertEquals(2, dao.getAllOrdered().size) // resetFailures never deletes
    }

    @Test
    fun `deleteForEntity removes all ops for a local id`() = runTest {
        dao.enqueue(op(entityLocalId = "x"))
        dao.enqueue(op(entityLocalId = "x", syncOperationType = SyncOperationType.UPDATE))
        dao.enqueue(op(entityLocalId = "y"))
        dao.deleteForEntity("x")
        assertEquals(listOf("y"), dao.getAllOrdered().map { it.entityId })
    }
}
