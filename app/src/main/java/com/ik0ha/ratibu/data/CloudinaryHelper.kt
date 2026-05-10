package com.ik0ha.ratibu.data

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.ik0ha.ratibu.BuildConfig

object CloudinaryHelper {
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME
            )
            MediaManager.init(context, config)
            isInitialized = true
        }
    }

    fun uploadImage(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Use unsigned upload since only cloud_name is provided
        // NOTE: Ensure you have an unsigned upload preset named 'ml_default' or change it here
        MediaManager.get().upload(uri)
            .unsigned("ml_default")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String ?: ""
                    onSuccess(url)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}
