package app.trainer.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import app.trainer.app.PendingInvite
import app.trainer.app.PendingPasswordReset
import app.trainer.app.ui.AppGate
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val pendingInvite: PendingInvite by inject()
    private val pendingPasswordReset: PendingPasswordReset by inject()

    private val notificationsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationsPermissionIfNeeded()
        rememberLink(intent)
        setContent { AppGate() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        rememberLink(intent)
    }

    private fun rememberLink(intent: Intent?) {
        val link = intent?.data?.toString() ?: return
        pendingInvite.remember(link)
        pendingPasswordReset.remember(link)
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
