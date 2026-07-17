package com.appcustodian.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.appcustodian.app.ui.screens.AppListScreen
import com.appcustodian.app.ui.theme.AppCustodianTheme
import com.appcustodian.app.util.PermissionUtils
import com.appcustodian.app.ui.AppListViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppCustodianTheme {
                AppListScreen(
                    viewModel = viewModel,
                    onRequestForceStop = { packageName ->
                        startActivity(PermissionUtils.appInfoIntent(this, packageName))
                    },
                    onOpenApp = { packageName ->
                        viewModel.getLaunchIntent(packageName)?.let { startActivity(it) }
                    },
                    onOpenAccessibilitySettings = {
                        startActivity(PermissionUtils.accessibilitySettingsIntent())
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check whenever we come back from Settings (Accessibility toggle or App Info screen).
        viewModel.refreshPermissionState()
        viewModel.loadApps()
    }
}