package com.example.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.model.WeatherState
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeatherDto?
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "is_day") val isDay: Int? = 1
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m,is_day"
    ): OpenMeteoResponse
}

class WeatherRepository(private val context: Context) {

    private val api: OpenMeteoApi by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherState {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCurrentWeather(latitude = latitude, longitude = longitude)
                val current = response.current
                    ?: return@withContext WeatherState.Error("Weather data unavailable")

                val cityName = resolveCityName(latitude, longitude)
                val updatedLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                WeatherState.Ready(
                    tempCelsius = current.temperature2m,
                    weatherCode = current.weatherCode,
                    windSpeedKmh = current.windSpeed10m,
                    humidityPercent = current.relativeHumidity2m ?: 50,
                    cityName = cityName,
                    isDay = (current.isDay ?: 1) == 1,
                    updatedTimeLabel = updatedLabel
                )
            } catch (e: Exception) {
                WeatherState.Error(e.localizedMessage ?: "Network unavailable")
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun resolveCityName(latitude: Double, longitude: Double): String {
        val fallback = String.format(Locale.US, "%.2f°, %.2f°", latitude, longitude)
        if (!Geocoder.isPresent()) return fallback
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                        if (cont.isActive) {
                            cont.resume(city ?: fallback)
                        }
                    }
                }
            } else {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = addresses?.firstOrNull()
                addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    companion object {
        fun describeWmoCode(code: Int): String {
            return when (code) {
                0 -> "Clear Sky"
                1 -> "Mainly Clear"
                2 -> "Partly Cloudy"
                3 -> "Overcast"
                45, 48 -> "Foggy"
                51, 53, 55 -> "Drizzle"
                56, 57 -> "Freezing Drizzle"
                61, 63, 65 -> "Rain"
                66, 67 -> "Freezing Rain"
                71, 73, 75, 77 -> "Snow"
                80, 81, 82 -> "Rain Showers"
                85, 86 -> "Snow Showers"
                95, 96, 99 -> "Thunderstorm"
                else -> "Fair"
            }
        }
    }
}
