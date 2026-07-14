package com.chaddy50.concerttracker.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.data.repository.ServerUrlValidationError

/** Maps a [ServerUrlValidationError] to its user-facing message. Shared by the startup prompt and Settings. */
@Composable
fun ServerUrlValidationError.message(): String = stringResource(
    when (this) {
        ServerUrlValidationError.INVALID_FORMAT -> R.string.server_url_validation_invalid_format
        ServerUrlValidationError.UNREACHABLE -> R.string.server_url_validation_unreachable
        ServerUrlValidationError.SERVER_ERROR -> R.string.server_url_validation_server_error
        ServerUrlValidationError.UNKNOWN -> R.string.server_url_validation_unknown
    }
)
