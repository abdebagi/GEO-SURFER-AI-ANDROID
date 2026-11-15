package com.geosurfer.ai
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.*

data class SatelliteSearchResult(
    val success: Boolean,
    val items: List<SatelliteItem>,
    val message: String
)

class SatelliteDataEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val stacUrl =
        "https://earth-search.aws.element84.com/v1/search"

    private fun createBbox(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Double> {

        require(latitude in -90.0..90.0)
        require(longitude in -180.0..180.0)
        require(radiusKm > 0)

        val latOffset = radiusKm / 111.32

        val lonOffset = radiusKm /
            (111.32 * cos(Math.toRadians(latitude))
                .coerceAtLeast(0.01))

        return listOf(
            longitude - lonOffset,
            latitude - latOffset,
            longitude + lonOffset,
            latitude + latOffset
        )
    }

    fun searchSatelliteImages(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        startDate: String,
        endDate: String,
        maxCloudCover: Double = 30.0
    ): SatelliteSearchResult {

        return try {

            require(maxCloudCover in 0.0..100.0)

            val bbox = createBbox(
                latitude,
                longitude,
                radiusKm
            )

            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)

            require(!end.isBefore(start)) {
                "End date must not precede start date"
            }

            val body = JSONObject().apply {

                put("bbox", org.json.JSONArray(bbox))

                put(
                    "datetime",
                    "${start}T00:00:00Z/${end}T23:59:59Z"
                )

                put(
                    "collections",
                    org.json.JSONArray().put("sentinel-2-l2a")
                )

                put("limit", 100)

                put(
                    "query",
                    JSONObject().put(
                        "eo:cloud_cover",
                        JSONObject()
                            .put("lte", maxCloudCover)
                    )
                )
            }

            val request = Request.Builder()
                .url(stacUrl)
                .post(
                    body.toString().toRequestBody(
    "application/json".toMediaType()
)
                    )
                )
                .build()

            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    throw IOException(
                        "Server error: ${response.code}"
                    )
                }

                val responseBody =
                    response.body?.string()
                        ?: throw IOException("Empty response")

                val json = JSONObject(responseBody)

                val features = json.optJSONArray("features")
                    ?: org.json.JSONArray()

                val items = mutableListOf<SatelliteItem>()

                for (i in 0 until features.length()) {

                    val feature = features.getJSONObject(i)

                    val properties =
                        feature.optJSONObject("properties")
                            ?: JSONObject()

                    val assetsJson =
                        feature.optJSONObject("assets")
                            ?: JSONObject()

                    val assetLinks = mutableMapOf<String, String>()

                    val keys = assetsJson.keys()

                    while (keys.hasNext()) {

                        val key = keys.next()

                        val asset = assetsJson.optJSONObject(key)
                            ?: continue

                        val href = asset.optString("href")

                        if (href.isNotBlank()) {
                            assetLinks[key] = href
                        }
                    }

                 items.add(
    SatelliteItem(
        id = feature.optString("id"),
        date = properties.optString("datetime"),
        cloudCover = properties.optDouble(
            "eo:cloud_cover",
            -1.0
        ),
        assets = assetLinks
    )
) 
