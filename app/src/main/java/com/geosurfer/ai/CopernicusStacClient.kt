package com.geosurfer.ai

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

data class SatelliteItem(
    val id: String,
    val date: String,
    val cloudCover: Double
)

class CopernicusStacClient {

    companion object {
        private const val API_URL =
            "https://stac.opensearch.dataspace.copernicus.eu/v1/search"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun searchSentinel2(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        startDate: String,
        endDate: String,
        maxCloud: Double,
        onSuccess: (List<SatelliteItem>) -> Unit,
        onError: (String) -> Unit
    ) {

        executor.execute {

            try {

                require(latitude in -90.0..90.0)
                require(longitude in -180.0..180.0)
                require(radiusKm > 0.0)
                require(maxCloud in 0.0..100.0)

                val geometry = createCircle(
                    latitude,
                    longitude,
                    radiusKm
                )

                val body = JSONObject().apply {

                    put(
                        "collections",
                        org.json.JSONArray().put("sentinel-2-l2a")
                    )

                    put("intersects", geometry)

                    put(
                        "datetime",
                        "${startDate}T00:00:00Z/" +
                        "${endDate}T23:59:59Z"
                    )

                    put("limit", 50)

                    put(
                        "query",
                        JSONObject().put(
                            "eo:cloud_cover",
                            JSONObject().put("lte", maxCloud)
                        )
                    )
                }

                val connection =
                    URL(API_URL).openConnection()
                        as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    connectTimeout = 30000
                    readTimeout = 60000
                    doOutput = true

                    setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    setRequestProperty(
                        "Accept",
                        "application/geo+json"
                    )
                }

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val status = connection.responseCode

                val responseStream =
                    if (status in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val response = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                connection.disconnect()

                if (status !in 200..299) {
                    throw Exception(
                        "Copernicus HTTP $status: $response"
                    )
                }

                val json = JSONObject(response)
                val features = json.optJSONArray("features")

                val results = mutableListOf<SatelliteItem>()

                if (features != null) {

                    for (i in 0 until features.length()) {

                        val feature = features.getJSONObject(i)
                        val properties =
                            feature.optJSONObject("properties")
                                ?: continue

                        results.add(
                            SatelliteItem(
                                id = feature.optString("id"),
                                date = properties.optString("datetime"),
                                cloudCover = properties.optDouble(
                                    "eo:cloud_cover",
                                    -1.0
                                )
                            )
                        )
                    }
                }

                mainHandler.post {
                    onSuccess(results)
                }

            } catch (e: Exception) {

                mainHandler.post {
                    onError(
                        e.message ?: "Unknown connection error"
                    )
                }
            }
        }
    }

    private fun createCircle(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): JSONObject {

        val coordinates = org.json.JSONArray()

        val latOffset = radiusKm / 111.32

        val cosLat = Math.cos(
            Math.toRadians(latitude)
        ).coerceAtLeast(0.01)

        val lonOffset = radiusKm / (111.32 * cosLat)

        for (i in 0..36) {

            val angle = Math.toRadians(i * 10.0)

            val lon = longitude +
                lonOffset * Math.cos(angle)

            val lat = latitude +
                latOffset * Math.sin(angle)

            coordinates.put(
                org.json.JSONArray()
                    .put(lon)
                    .put(lat)
            )
        }

        return JSONObject().apply {
            put("type", "Polygon")

            put(
                "coordinates",
                org.json.JSONArray().put(coordinates)
            )
        }
    }
}
