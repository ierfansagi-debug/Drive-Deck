package com.example

import com.example.data.WeatherRepository
import com.example.model.SpeedUnit
import com.example.model.TelemetryState
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadunitLogicTest {

    @Test
    fun `telemetry converts mps to mph and kmh accurately`() {
        val telemetry = TelemetryState(speedMps = 26.8224f) // 60 mph / ~96.5 kmh
        assertEquals(60, telemetry.formattedSpeed(SpeedUnit.MPH))
        assertEquals(96, telemetry.formattedSpeed(SpeedUnit.KMH))
    }

    @Test
    fun `telemetry formats trip distance in miles and kilometers`() {
        val telemetry = TelemetryState(tripDistanceMeters = 16093.44) // 10 miles / 16.09 km
        assertEquals("10.0 mi", telemetry.formattedTripDistance(SpeedUnit.MPH))
        assertEquals("16.1 km", telemetry.formattedTripDistance(SpeedUnit.KMH))
    }

    @Test
    fun `wmo weather codes map to human readable descriptions`() {
        assertEquals("Clear Sky", WeatherRepository.describeWmoCode(0))
        assertEquals("Rain", WeatherRepository.describeWmoCode(63))
        assertEquals("Thunderstorm", WeatherRepository.describeWmoCode(95))
    }
}
