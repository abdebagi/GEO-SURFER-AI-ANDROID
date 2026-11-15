package com.geosurfer.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CopernicusS3DownloadManager(
    context: Context
) {

    companion object {

        private const val ENDPOINT =
            "https://eodata.dataspace.copernicus.eu"

        private const val REGION = "default"

        private const val SERVICE = "s3"

        private const val ALGORITHM =
            "AWS4-HMAC-SHA256"
    }

    private val appContext =
        context.applicationContext

    private val executor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()


    fun downloadObject(
        s3Path: String,
        accessKey: String,
        secretKey: String,
        onProgress: (Long, Long) -> Unit,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {

        executor.execute {

            var temporaryFile: File? = null

            try {

                require(accessKey.isNotBlank()) {
                    "S3 Access Key is required"
                }

                require(secretKey.isNotBlank()) {
                    "S3 Secret Key is required"
                }

                val uri = URI(s3Path)

                require(uri.scheme == "s3") {
                    "Invalid S3 URI"
                }

                val bucket =
                    uri.host ?: throw Exception(
                        "Invalid S3 bucket"
                    )

                val objectKey =
                    uri.path.removePrefix("/")

                require(objectKey.isNotBlank()) {
                    "S3 object path is empty"
                }

                require(
                    bucket.matches(
                        Regex("[a-zA-Z0-9.-]+")
                    )
                ) {
                    "Invalid bucket name"
                }

                val url = buildS3Url(
                    bucket,
                    objectKey
                )

                val authorization =
                    createAuthorization(
                        url,
                        accessKey,
                        secretKey
                    )

                val request =
                    Request.Builder()
                        .url(url)
                        .get()
                        .header(
                            "Host",
                            url.host
                        )
                        .header(
                            "x-amz-date",
                            authorization.first
                        )
                        .header(
                            "x-amz-content-sha256",
                            authorization.second
                        )
                        .header(
                            "Authorization",
                            authorization.third
                        )
                        .build()

                client.newCall(request)
                    .execute()
                    .use { response ->

                        if (!response.isSuccessful) {

                            throw Exception(
                                "S3 HTTP ${response.code}: " +
                                response.message
                            )
                        }

                        val body =
                            response.body
                                ?: throw Exception(
                                    "Empty S3 response"
                                )

                        val total =
                            body.contentLength()

                        val directory =
                            File(
                                appContext.filesDir,
                                "SatelliteData"
                            )

                        if (!directory.exists()) {
                            directory.mkdirs()
                        }

                        val fileName =
                            objectKey.substringAfterLast("/")

                        require(
                            fileName.isNotBlank() &&
                            fileName != "." &&
                            fileName != ".."
                        ) {
                            "Invalid destination filename"
                        }

                        val destination =
                            File(directory, fileName)

                        temporaryFile =
                            File(
                                directory,
                                "$fileName.download"
                            )

                        var downloaded = 0L

                        body.byteStream().use { input ->

                            FileOutputStream(
                                temporaryFile
                            ).use { output ->

                                val buffer =
                                    ByteArray(64 * 1024)

                                while (true) {

                                    val count =
                                        input.read(buffer)

                                    if (count == -1) {
                                        break
                                    }

                                    output.write(
                                        buffer,
                                        0,
                                        count
                                    )

                                    downloaded += count

                                    mainHandler.post {

                                        onProgress(
                                            downloaded,
                                            total
                                        )
                                    }
                                }

                                output.fd.sync()
                            }
                        }

                        require(
                            temporaryFile.length() > 0
                        ) {
                            "Downloaded file is empty"
                        }

                        if (destination.exists()) {
                            destination.delete()
                        }

                        require(
                            temporaryFile.renameTo(
                                destination
                            )
                        ) {
                            "Unable to save downloaded file"
                        }

                        mainHandler.post {
                            onSuccess(destination)
                        }
                    }

            } catch (e: Exception) {

                temporaryFile?.delete()

                mainHandler.post {

                    onError(
                        e.message
                            ?: "Unknown S3 download error"
                    )
                }
            }
        }
    }


    private fun buildS3Url(
        bucket: String,
        objectKey: String
    ): HttpUrl {

        val builder =
            HttpUrl.parse(ENDPOINT)!!.newBuilder()

        builder.addPathSegment(bucket)

        objectKey.split("/").forEach { segment ->

            if (segment.isNotEmpty()) {
                builder.addPathSegment(segment)
            }
        }

        return builder.build()
    }


    private fun createAuthorization(
        url: HttpUrl,
        accessKey: String,
        secretKey: String
    ): Triple<String, String, String> {

        val timestamp =
            SimpleDateFormat(
                "yyyyMMdd'T'HHmmss'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

        val date =
            timestamp.substring(0, 8)

        val payloadHash =
            sha256("")

        val canonicalHeaders =
            "host:${url.host}\n" +
            "x-amz-content-sha256:$payloadHash\n" +
            "x-amz-date:$timestamp\n"

        val signedHeaders =
            "host;x-amz-content-sha256;x-amz-date"

        val canonicalRequest =
            "GET\n" +
            url.encodedPath + "\n" +
            "\n" +
            canonicalHeaders + "\n" +
            signedHeaders + "\n" +
            payloadHash

        val scope =
            "$date/$REGION/$SERVICE/aws4_request"

        val stringToSign =
            "$ALGORITHM\n" +
            "$timestamp\n" +
            "$scope\n" +
            sha256(canonicalRequest)

        val signingKey =
            getSignatureKey(
                secretKey,
                date
            )

        val signature =
            hmacSha256(
                signingKey,
                stringToSign
            ).joinToString("") {
                "%02x".format(it)
            }

        val authorization =
            "$ALGORITHM " +
            "Credential=$accessKey/$scope, " +
            "SignedHeaders=$signedHeaders, " +
            "Signature=$signature"

        return Triple(
            timestamp,
            payloadHash,
            authorization
        )
    }


    private fun getSignatureKey(
        secretKey: String,
        date: String
    ): ByteArray {

        val kDate =
            hmacSha256(
                ("AWS4" + secretKey)
                    .toByteArray(),
                date
            )

        val kRegion =
            hmacSha256(
                kDate,
                REGION
            )

        val kService =
            hmacSha256(
                kRegion,
                SERVICE
            )

        return hmacSha256(
            kService,
            "aws4_request"
        )
    }


    private fun hmacSha256(
        key: ByteArray,
        data: String
    ): ByteArray {

        val mac =
            Mac.getInstance("HmacSHA256")

        mac.init(
            SecretKeySpec(
                key,
                "HmacSHA256"
            )
        )

        return mac.doFinal(
            data.toByteArray(Charsets.UTF_8)
        )
    }


    private fun sha256(
        value: String
    ): String {

        return MessageDigest
            .getInstance("SHA-256")
            .digest(
                value.toByteArray(Charsets.UTF_8)
            )
            .joinToString("") {
                "%02x".format(it)
            }
    }
}
