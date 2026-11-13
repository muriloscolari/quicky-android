package me.muriloscolari.quicky.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateSource(
    val label: String,
    val hostLabel: String,
    val releasesApiUrl: String
) {
    FORGEJO(
        label = "Forgejo",
        hostLabel = "git.muriloscolari.me",
        releasesApiUrl = "https://git.muriloscolari.me/api/v1/repos/murilo/quicky-android/releases/latest"
    ),
    GITLAB(
        label = "GitLab",
        hostLabel = "gitlab.com",
        releasesApiUrl = "https://gitlab.com/api/v4/projects/muriloscolari%2Fquicky-android/releases"
    )
}

data class ReleaseInfo(
    val tagName: String,
    val versionClean: String,
    val notes: String,
    val apkDownloadUrl: String,
    val source: UpdateSource
)

object AppUpdater {

    fun cleanVersion(version: String): String =
        version.trim().removePrefix("v").removePrefix("V")

    /**
     * Returns true if [remoteVersion] is strictly newer than [currentVersion].
     */
    fun isNewerVersion(currentVersion: String, remoteVersion: String): Boolean {
        val currentParts = cleanVersion(currentVersion).split(".", "-").mapNotNull { it.toIntOrNull() }
        val remoteParts = cleanVersion(remoteVersion).split(".", "-").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun parseForgejoReleaseJson(jsonText: String): ReleaseInfo? {
        return runCatching {
            val obj = JSONObject(jsonText)
            val tag = obj.optString("tag_name", "")
            if (tag.isBlank()) return null
            val body = obj.optString("body", "")
            val assets = obj.optJSONArray("assets") ?: JSONArray()
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                    apkUrl = url
                    break
                }
            }
            if (apkUrl.isBlank()) return null
            ReleaseInfo(
                tagName = tag,
                versionClean = cleanVersion(tag),
                notes = body,
                apkDownloadUrl = apkUrl,
                source = UpdateSource.FORGEJO
            )
        }.getOrNull()
    }

    fun parseGitLabReleasesJson(jsonText: String): ReleaseInfo? {
        return runCatching {
            val arr = if (jsonText.trimStart().startsWith("[")) {
                JSONArray(jsonText)
            } else {
                JSONArray().put(JSONObject(jsonText))
            }
            if (arr.length() == 0) return null
            val obj = arr.getJSONObject(0)
            val tag = obj.optString("tag_name", "")
            if (tag.isBlank()) return null
            val description = obj.optString("description", "")
            val assets = obj.optJSONObject("assets")
            val links = assets?.optJSONArray("links") ?: JSONArray()
            var apkUrl = ""
            for (i in 0 until links.length()) {
                val link = links.getJSONObject(i)
                val name = link.optString("name", "")
                val directUrl = link.optString("direct_asset_url", "").ifBlank {
                    link.optString("url", "")
                }
                if ((name.endsWith(".apk", ignoreCase = true) || directUrl.endsWith(".apk", ignoreCase = true)) && directUrl.isNotBlank()) {
                    apkUrl = directUrl
                    break
                }
            }
            if (apkUrl.isBlank()) return null
            ReleaseInfo(
                tagName = tag,
                versionClean = cleanVersion(tag),
                notes = description,
                apkDownloadUrl = apkUrl,
                source = UpdateSource.GITLAB
            )
        }.getOrNull()
    }

    suspend fun checkLatestRelease(source: UpdateSource): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(source.releasesApiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code ao consultar ${source.label}")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = when (source) {
                    UpdateSource.FORGEJO -> parseForgejoReleaseJson(body)
                    UpdateSource.GITLAB -> parseGitLabReleasesJson(body)
                }
                parsed ?: throw IllegalStateException("Nenhum APK encontrado na última release do ${source.label}")
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(updatesDir, "quicky-${release.versionClean}.apk")

            val conn = (URL(release.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 10_000
                readTimeout = 20_000
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("Erro HTTP $code ao baixar APK")
                }
                val total = conn.contentLength.coerceAtLeast(1)
                var downloaded = 0
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 1) {
                                onProgress(((downloaded * 100L) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }

            withContext(Dispatchers.Main) {
                promptInstallApk(context, targetFile)
            }
            targetFile
        }
    }

    fun promptInstallApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
