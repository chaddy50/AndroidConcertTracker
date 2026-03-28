package com.chaddy50.concerttracker

import com.chaddy50.concerttracker.data.entity.Composer
import com.chaddy50.concerttracker.data.entity.Performance
import com.chaddy50.concerttracker.data.entity.Performer
import com.chaddy50.concerttracker.data.entity.SetListEntry
import com.chaddy50.concerttracker.data.entity.SetListEntryPerformer
import com.chaddy50.concerttracker.data.entity.Venue
import com.chaddy50.concerttracker.data.entity.Work
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType

object TestData {
    val conductor = Performer(id = "conductor-1", name = "Simon Rattle", type = PerformerType.CONDUCTOR)
    val orchestra = Performer(id = "orchestra-1", name = "London Symphony Orchestra", type = PerformerType.ORCHESTRA)
    val soloist = Performer(id = "soloist-1", name = "Martha Argerich", type = PerformerType.SOLO)
    val venue = Venue(id = "venue-1", name = "Royal Albert Hall", osmId = "123456")
    val composer = Composer(id = "composer-1", name = "Ludwig van Beethoven")
    val work = Work(id = "work-1", title = "Symphony No. 5 in C minor", composers = listOf(composer))

    val setListEntry = SetListEntry(
        id = "entry-1",
        work = work,
        order = 1,
        conductor = null,
        featuredPerformers = listOf(SetListEntryPerformer(performer = soloist, role = "Piano")),
        notes = "Beautifully played."
    )

    val performance = Performance(
        id = "perf-1",
        date = "2024-11-15T19:30:00.000Z",
        venue = venue,
        performers = listOf(orchestra),
        conductor = conductor,
        status = PerformanceStatus.ATTENDED,
        setList = listOf(setListEntry)
    )

    val performanceWithoutConductor = performance.copy(id = "perf-2", conductor = null)
}
