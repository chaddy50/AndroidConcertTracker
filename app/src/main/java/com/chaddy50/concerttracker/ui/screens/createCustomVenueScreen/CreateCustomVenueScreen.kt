package com.chaddy50.concerttracker.ui.screens.createCustomVenueScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chaddy50.concerttracker.data.domain.Venue

@Composable
fun CreateCustomVenueScreen(
    onVenueCreated: (Venue) -> Unit,
    onCancel: () -> Unit,
    viewModel: CreateCustomVenueViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CreateCustomVenueForm(
            name = viewModel.name,
            address = viewModel.address,
            city = viewModel.city,
            country = viewModel.country,
            website = viewModel.website,
            nameError = viewModel.nameError,
            addressError = viewModel.addressError,
            cityError = viewModel.cityError,
            countryError = viewModel.countryError,
            onNameChange = viewModel::updateName,
            onAddressChange = viewModel::updateAddress,
            onCityChange = viewModel::updateCity,
            onCountryChange = viewModel::updateCountry,
            onWebsiteChange = viewModel::updateWebsite,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !viewModel.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { viewModel.save(onVenueCreated) },
                enabled = !viewModel.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
        if (viewModel.saveError != null) {
            Text(
                text = viewModel.saveError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
