package com.chaddy50.concerttracker.data

import com.chaddy50.concerttracker.data.external.dataTransferObjects.ComposerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.FeaturedPerformerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.PerformanceDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.PerformerDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.SetListEntryDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.VenueDto
import com.chaddy50.concerttracker.data.external.dataTransferObjects.WorkDto
import com.chaddy50.concerttracker.data.enum.PerformanceStatus
import com.chaddy50.concerttracker.data.enum.PerformerType

/** Domain-model fixtures shared across mapper/DAO/repository tests. */
object Fixtures {

    fun venue(id: String = "v1") = VenueDto(id = id, name = "Hall $id", osmId = "osm-$id", osmType = "way")

    fun performer(id: String = "perf1", type: PerformerType = PerformerType.ORCHESTRA) =
        PerformerDto(id = id, name = "Performer $id", type = type, specialty = null, musicbrainzId = "mb-$id")

    fun composer(id: String = "c1") =
        ComposerDto(id = id, name = "Composer $id", sortName = "Sort $id", openOpusId = "oo-$id")

    fun work(id: String = "w1", composers: List<ComposerDto> = listOf(composer())) =
        WorkDto(id = id, title = "Work $id", composers = composers)

    fun setListEntry(
        id: String = "s1",
        order: Int = 1,
        work: WorkDto = work(),
        featured: List<FeaturedPerformerDto> = emptyList(),
        notes: String? = null
    ) = SetListEntryDto(id = id, work = work, order = order, conductor = null, featuredPerformers = featured, notes = notes)

    fun performance(
        id: String = "p1",
        date: String = "2024-06-01T19:00:00Z",
        status: PerformanceStatus = PerformanceStatus.UPCOMING,
        venue: VenueDto = venue(),
        performers: List<PerformerDto> = listOf(performer()),
        conductor: PerformerDto? = null,
        setList: List<SetListEntryDto> = emptyList(),
        notes: String? = null
    ) = PerformanceDto(
        id = id,
        date = date,
        venue = venue,
        performers = performers,
        conductor = conductor,
        status = status,
        setList = setList,
        notes = notes
    )

    /** A richly-populated performance exercising every nested relation, for round-trip tests. */
    fun fullPerformance(id: String = "p1", status: PerformanceStatus = PerformanceStatus.UPCOMING): PerformanceDto {
        val soloist = performer(id = "soloist", type = PerformerType.SOLO)
        val conductor = performer(id = "maestro", type = PerformerType.CONDUCTOR)
        val sharedComposer = composer(id = "shared")
        return performance(
            id = id,
            status = status,
            performers = listOf(performer(id = "orchestra"), conductor),
            conductor = conductor,
            setList = listOf(
                setListEntry(
                    id = "${id}_s1",
                    order = 1,
                    work = work(id = "w1", composers = listOf(sharedComposer, composer(id = "c2"))),
                    featured = listOf(FeaturedPerformerDto(performer = soloist, role = "Piano")),
                    notes = "Brilliant"
                ),
                setListEntry(
                    id = "${id}_s2",
                    order = 2,
                    work = work(id = "w2", composers = listOf(sharedComposer)),
                    featured = emptyList(),
                    notes = null
                )
            )
        )
    }
}
