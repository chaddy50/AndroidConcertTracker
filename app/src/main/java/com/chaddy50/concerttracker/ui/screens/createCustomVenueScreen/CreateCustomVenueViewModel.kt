package com.chaddy50.concerttracker.ui.screens.createCustomVenueScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaddy50.concerttracker.data.domain.Venue
import com.chaddy50.concerttracker.data.external.api.ApiResult
import com.chaddy50.concerttracker.data.external.api.VenueRequest
import com.chaddy50.concerttracker.data.repository.VenuesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateCustomVenueViewModel @Inject constructor(
    private val venuesRepository: VenuesRepository
) : ViewModel() {

    var name: String by mutableStateOf("")
        private set

    var address: String by mutableStateOf("")
        private set

    var city: String by mutableStateOf("")
        private set

    var country: String by mutableStateOf("")
        private set

    var website: String by mutableStateOf("")
        private set

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var saveError: String? by mutableStateOf(null)
        private set

    var nameError: Boolean by mutableStateOf(false)
        private set

    var addressError: Boolean by mutableStateOf(false)
        private set

    var cityError: Boolean by mutableStateOf(false)
        private set

    var countryError: Boolean by mutableStateOf(false)
        private set

    fun updateName(value: String) { name = value; if (value.isNotBlank()) nameError = false }
    fun updateAddress(value: String) { address = value; if (value.isNotBlank()) addressError = false }
    fun updateCity(value: String) { city = value; if (value.isNotBlank()) cityError = false }
    fun updateCountry(value: String) { country = value; if (value.isNotBlank()) countryError = false }
    fun updateWebsite(value: String) { website = value }

    fun save(onVenueCreated: (Venue) -> Unit) {
        if (isSaving) return

        nameError = name.isBlank()
        addressError = address.isBlank()
        cityError = city.isBlank()
        countryError = country.isBlank()
        if (nameError || addressError || cityError || countryError) return

        viewModelScope.launch {
            isSaving = true
            saveError = null
            val request = VenueRequest(
                osmType = null,
                osmId = null,
                name = name.trim(),
                address = address.trim(),
                city = city.trim(),
                country = country.trim(),
                website = website.trim().ifBlank { null }
            )
            when (val result = venuesRepository.findOrCreateVenue(request)) {
                is ApiResult.Success -> onVenueCreated(result.data)
                is ApiResult.Error -> saveError = result.errorType.toUserMessage()
            }
            isSaving = false
        }
    }
}
