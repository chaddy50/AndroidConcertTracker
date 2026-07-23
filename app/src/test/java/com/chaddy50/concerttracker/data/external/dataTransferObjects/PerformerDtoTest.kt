package com.chaddy50.concerttracker.data.external.dataTransferObjects

import com.chaddy50.concerttracker.data.enum.PerformerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerformerDtoTest {

    private fun dto(specialty: String?) =
        PerformerDto(id = "pe1", name = "Yo-Yo Ma", type = PerformerType.SOLO, specialty = specialty)

    @Test
    fun `toDomain normalizes a blank specialty to null`() {
        assertNull(dto("").toDomain().specialty)
        assertNull(dto("   ").toDomain().specialty)
        assertNull(dto(null).toDomain().specialty)
    }

    @Test
    fun `toDomain preserves a real specialty`() {
        assertEquals("Cellist", dto("Cellist").toDomain().specialty)
    }

    @Test
    fun `toRow normalizes a blank specialty to null`() {
        assertNull(dto("").toRow().specialty)
        assertNull(dto(null).toRow().specialty)
    }

    @Test
    fun `toRow preserves a real specialty`() {
        assertEquals("Cellist", dto("Cellist").toRow().specialty)
    }
}
