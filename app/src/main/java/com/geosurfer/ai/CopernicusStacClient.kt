package com.geosurfer.ai

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.Executors

data class SatelliteItem(
val id: String,
val date: String,
val cloudCover: Double,
val assets: Map<String, String>
)

class CopernicusStacClient {

companion object {
    private const val API_URL =
        "https://stac.dataspace.copernicus.eu/v1/search"

    private const val PAGE_LIMIT = 200
    private const val MAX_PAGES = 5

    val SPECTRAL_BANDS = listOf(
        "B02",
        "B03",
        "B04",
        "B08",
        "B11",
        "B12"
    )
}

private val executor =
    Executors.newSingleThreadExecutor()

private val mainHandler =
    Handler(Looper.getMainLooper())

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

            require(latitude.isFinite() &&
                latitude in -90.0..90.0) {
                "Invalid latitude"
            }

            require(longitude.isFinite() &&
                longitude in -180.0..180.0) {
                "Invalid longitude"
            }

            require(radiusKm.isFinite() &&
                radiusKm > 0.0 &&
                radiusKm <= 500.0) {
                "Radius must be between 0 and 500 km"
            }

            require(maxCloud.isFinite() &&
                maxCloud in 0.0..100.0) {
                "Invalid cloud cover"
            }

            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)

            require(!start.isAfter(end)) {
                "Start date must be before end date"
            }

            val geometry = createCircle(
                latitude,
                longitude,
                radiusKm
            )

            val requestBody = JSONObject().apply {

                put(
                    "collections",
                    JSONArray().put("sentinel-2-l2a")
                )

                put("intersects", geometry)

                put(
                    "datetime",
                    "${startDate}T00:00:00Z/" +
                        "${endDate}T23:59:59Z"
                )

                put("limit", PAGE_LIMIT)

                put(
                    "query",
                    JSONObject().put(
                        "eo:cloud_cover",
                        JSONObject().put(
                            "lte",
                            maxCloud
                        )
                    )
                )
            }

            val collected =
                LinkedHashMap<String, SatelliteItem>()

            var nextUrl = API_URL
            var nextMethod = "POST"
            var nextBody: JSONObject? = requestBody

            var pageCount = 0

            while (
                pageCount < MAX_PAGES &&
                nextUrl.isNotBlank()
            ) {

                val json = fetchPage(
                    url = nextUrl,
                    method = nextMethod,
                    body = nextBody
                )

                val features =
                    json.optJSONArray("features")
                        ?: JSONArray()

                for (i in 0 until features.length()) {

                    val feature =
                        features.optJSONObject(i)
                            ?: continue

                    val properties =
                        feature.optJSONObject("properties")
                            ?: continue

                    val assetsObject =
                        feature.optJSONObject("assets")
                            ?: JSONObject()

                    val assetLinks =
                        mutableMapOf<String, String>()

                    val keys = assetsObject.keys()

                    while (keys.hasNext()) {

                        val key = keys.next()

                        val asset =
                            assetsObject.optJSONObject(key)

                        val href =
                            asset?.optString("href", "")
                                ?: ""

                        if (href.isNotBlank()) {
                            assetLinks[key] = href
                        }
                    }

                    val id = feature.optString("id", "")

                    if (id.isBlank()) continue

                    collected[id] = SatelliteItem(
                        id = id,
                        date = properties.optString(
                            "datetime",
                            ""
                        ),
                        cloudCover = properties.optDouble(
                            "eo:cloud_cover",
                            -1.0
                        ),
                        assets = assetLinks
                    )
                }

                pageCount++

                val links =
                    json.optJSONArray("links")

                val nextLink = findNextLink(links)

                if (nextLink == null) {
                    nextUrl = ""
                } else {

                    nextUrl = nextLink.optString(
                        "href",
                        ""
                    )

                    nextMethod = nextLink.optString(
                        "method",
                        "GET"
                    ).uppercase()

                    nextBody =
                        nextLink.optJSONObject("body")
                            ?: if (nextMethod == "POST") {
                                requestBody
                            } else {
                                null
                            }
                }
            }

            val results = collected.values
                .sortedWith(
                    compareBy<SatelliteItem> {
                        if (it.cloudCover < 0.0) {
                            Double.MAX_VALUE
                        } else {
                            it.cloudCover
                        }
                    }.thenByDescending {
                        it.date
                    }
                )

            mainHandler.post {
                onSuccess(results)
            }

        } catch (e: Exception) {

            mainHandler.post {
                onError(
                    e.message
                        ?: "Unknown Copernicus error"
                )
            }
        }
    }
}

private fun fetchPage(
    url: String,
    method: String,
    body: JSONObject?
): JSONObject {

    val connection =
        URL(url).openConnection() as HttpURLConnection

    try {

        connection.apply {

            requestMethod = method

            connectTimeout = 30000
            readTimeout = 60000

            setRequestProperty(
                "Accept",
                "application/geo+json, application/json"
            )

            if (method == "POST") {

                doOutput = true

                setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
            }
        }

        if (method == "POST") {

            connection.outputStream.use {
                it.write(
                    (body ?: JSONObject())
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }
        }

        val status = connection.responseCode

        val stream =
            if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val response = stream
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: ""

        if (status !in 200..299) {

            throw Exception(
                "Copernicus HTTP $status: $response"
            )
        }

        return JSONObject(response)

    } finally {

        connection.disconnect()
    }
}

private fun findNextLink(
    links: JSONArray?
): JSONObject? {

    if (links == null) return null

    for (i in 0 until links.length()) {

        val link = links.optJSONObject(i)
            ?: continue

        if (link.optString("rel") == "next") {
            return link
        }
    }

    return null
}

private fun createCircle(
    latitude: Double,
    longitude: Double,
    radiusKm: Double
): JSONObject {

    val coordinates = JSONArray()

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
            JSONArray()
                .put(lon)
                .put(lat)
        )
    }

    return JSONObject().apply {

        put("type", "Polygon")

        put(
            "coordinates",
            JSONArray().put(coordinates)
        )
    }
}

}
