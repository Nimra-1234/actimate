package com.example.actimate.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * A utility class to handle runtime permission requests in the app
 */
class PermissionHandler {
    companion object {
        // Define all required permissions for the app
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Physical activity permission is only needed on Android 10 (API 29) and above
        val ACTIVITY_RECOGNITION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACTIVITY_RECOGNITION
        } else {
            "android.permission.ACTIVITY_RECOGNITION" // For backward compatibility
        }

        /**
         * Check if all required permissions are granted
         */
        fun allPermissionsGranted(context: Context): Boolean {
            return REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Check if a specific permission is granted
         */
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Request all required permissions
         */
        fun requestPermissions(activity: Activity, requestCode: Int) {
            ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                requestCode
            )
        }

        /**
         * Show rationale dialog when permission was denied
         */
        fun showPermissionRationaleDialog(context: Context, onRequestAgain: () -> Unit, onCancel: () -> Unit) {
            AlertDialog.Builder(context)
                .setTitle("Permissions Required")
                .setMessage("This app needs activity recognition and location permissions to track your activities accurately. Without these permissions, the app cannot function properly.")
                .setPositiveButton("Request Again") { _, _ ->
                    onRequestAgain()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    onCancel()
                }
                .setCancelable(false)
                .create()
                .show()
        }

        /**
         * Show dialog when permission was permanently denied
         */
        fun showSettingsDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle("Permissions Required")
                .setMessage("Some required permissions have been permanently denied. Please enable them in the app settings to use this app.")
                .setPositiveButton("Go to Settings") { _, _ ->
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .create()
                .show()
        }
    }
}

/**
 * Extension function for Activity to easily request all required permissions
 */
fun AppCompatActivity.requestAppPermissions(requestCode: Int = 100, onGranted: () -> Unit = {}) {
    if (PermissionHandler.allPermissionsGranted(this)) {
        onGranted()
    } else {
        PermissionHandler.requestPermissions(this, requestCode)
    }
}

/**
 * Extension function for Fragment to easily request all required permissions
 */
fun Fragment.requestAppPermissions(requestCode: Int = 100, onGranted: () -> Unit = {}) {
    context?.let { ctx ->
        if (PermissionHandler.allPermissionsGranted(ctx)) {
            onGranted()
        } else {
            activity?.let { PermissionHandler.requestPermissions(it, requestCode) }
        }
    }
}