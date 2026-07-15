package com.chaddy50.concerttracker.ui.screens.createCustomVenueScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chaddy50.concerttracker.R
import com.chaddy50.concerttracker.ui.composables.RequiredTextField
import com.chaddy50.concerttracker.ui.theme.ConcertTrackerTheme

@Composable
fun CreateCustomVenueForm(
    name: String,
    address: String,
    city: String,
    country: String,
    website: String,
    nameError: Boolean,
    addressError: Boolean,
    cityError: Boolean,
    countryError: Boolean,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RequiredTextField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(R.string.custom_venue_name_label),
            isError = nameError,
            modifier = Modifier.fillMaxWidth()
        )

        RequiredTextField(
            value = address,
            onValueChange = onAddressChange,
            label = stringResource(R.string.custom_venue_address_label),
            isError = addressError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        RequiredTextField(
            value = city,
            onValueChange = onCityChange,
            label = stringResource(R.string.custom_venue_city_label),
            isError = cityError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        RequiredTextField(
            value = country,
            onValueChange = onCountryChange,
            label = stringResource(R.string.custom_venue_country_label),
            isError = countryError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = website,
            onValueChange = onWebsiteChange,
            label = { Text(stringResource(R.string.custom_venue_website_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
    }
}

// region Previews
@Preview(showBackground = true)
@Composable
fun CreateCustomVenueFormPreview() {
    ConcertTrackerTheme {
        CreateCustomVenueForm(
            name = "The Blue Note",
            address = "131 W 3rd St, New York, NY 10012",
            city = "New York",
            country = "United States",
            website = "https://bluenotejazz.com",
            nameError = false,
            addressError = false,
            cityError = false,
            countryError = false,
            onNameChange = {},
            onAddressChange = {},
            onCityChange = {},
            onCountryChange = {},
            onWebsiteChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateCustomVenueFormEmptyPreview() {
    ConcertTrackerTheme {
        CreateCustomVenueForm(
            name = "",
            address = "",
            city = "",
            country = "",
            website = "",
            nameError = true,
            addressError = true,
            cityError = true,
            countryError = true,
            onNameChange = {},
            onAddressChange = {},
            onCityChange = {},
            onCountryChange = {},
            onWebsiteChange = {}
        )
    }
}
// endregion
