# Code Structure
The main entry point of the app is `MainActivity.kt`.

Beyond that, this app is broken up into five main packages.
## data
Holds all of the objects that represent data in the app, such as the performances, performers, set list entries, venues, etc.

Data is split into three layers to support an offline-first workflow that syncs back to the server when network is available.

Changes made by the user are first saved to the local Room database, but are then written through to the server if the device is connected to the internet. If it's not, then those changes sit in an outbox until internet connection is re-established.

These three data layers take the form of these sub-packages:
1. **external** - contains the object structures that are expected by external APIs, including the server's API, Open Opus, MusicBrainz, and Nominatim. It also contains the Retrofit service interfaces required to call those external services for searching for works, performers, and venues.
2. **local** - contains the object structures that are needed for the local Room database.
3. **domain** - contains the simplified object structures that are used by the UI.

The **enum** package contains enums that are common to all three data layers.

The **repository** package contains the public-facing accessors for data, mainly through the local Room database.

The **sync** package contains the outbox and WorkManager logic that pushes local changes to the server.
## dependencyInjection
Contains all the wiring for dependency injection that's used throughout the app for things like the local Room database, the external network APIs, and a data store that's used to store app settings.
## navigation
Contains all of the logic for how the app navigates between screens.

Each route is a strongly-typed data class to ensure that there's always proper type checking when passing data between routes.

**topBarActions** handles dynamically changing what's available in the app's top bar depending on which screen the user is on.
## ui
**composables** contains any composables that are used on multiple screens throughout the app.

**screens** contains the UI for each screen in the app, along with any composables that are only used on that screen.

**theme** contains the wiring for the Material3 theming used by the app.
## util
Contains some common utility functions that are used throughout the app.