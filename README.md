# What does this app do?
This is an Android app for keeping track of classical music concerts you've attended or plan to attend.
It lets you record all of the details that matter for a classical performance - the pieces on the program, who composed them, the performers and conductor, and the venue.
<br>
During the performance, you can take listening notes on each piece and refer back to your notes later.

# Features
## Performances at a glance
The home screen surfaces what's relevant right now: a **Next Up** card for your soonest upcoming concert and a **Recent** list of concerts you've been to in the last 30 days. **Upcoming** and **Past** tabs let you browse everything else.

<img src="./screenshots/home_screen.png" width="300">

## Performance details
Data that can be recorded about each performance includes date, time, venue, and performers, along with the full **set list** of pieces that were played. Opening a performance shows the program in order, with each pieces's composer, any featured soloists, and your own notes about each piece.

<img src="./screenshots/performance_details.png" width="300">

## Notes
You can attach your own notes to any work on a set list, capturing what stood out about a particular performance of a piece.

<img src="./screenshots/work_notes.png" width="300">

## Building a performance
Adding or editing a performance walks you through filling out every part of the program. You set the date and time, pick a venue, add the headline performers, and assemble the set list one work at a time.

<img src="./screenshots/edit_performance.png" width="300">

## Searching real data sources
Rather than typing everything by hand, the app looks up performers, composers, works, and venues against established databases, so names and details stay consistent. Searching for a performer queries **MusicBrainz** and shows what each result is - orchestra, ensemble, soloist, chorus, or conductor - before you add it to your own library.

<img src="./screenshots/performer_search.png" width="300">

<br>

Composers and pieces are looked up against **Open Opus**, complete with their opus numbers and category, and you can filter by type (Orchestral, Vocal, Keyboard, Chamber, etc.) to find the right piece quickly. If something isn't in the database, you can create a custom entry inline without leaving the search.

<img src="./screenshots/work_search.png" width="300">

<br>

Venues are searched against **OpenStreetMap** via Nominatim, so a concert hall or opera house is identified by its real address and location.

<img src="./screenshots/venue_search.png" width="300">

# Technical Details
This app was built using [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/compose).
It is the Android client for a [self-hosted backend API (FastAPI + PostgreSQL)](https://github.com/chaddy50/FastAPIConcertTrackerAPI) that stores your library.

## Libraries Used
[Retrofit 2](https://github.com/square/retrofit) for making type-safe HTTP API calls.
<br>
[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for parsing JSON responses.
<br>
[Hilt](https://developer.android.com/training/dependency-injection/hilt-android) for dependency injection.
<br>
[Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for storing app settings (such as the API server URL) locally.
<br>
[Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for navigating between screens.

## External APIs
[MusicBrainz](https://musicbrainz.org/doc/MusicBrainz_API) for looking up performers.
<br>
[Open Opus](https://openopus.org/) for looking up classical composers and their works.
<br>
[OpenStreetMap Nominatim](https://nominatim.org/) for looking up concert venues.
