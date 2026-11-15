package com.example.plume

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale
import com.example.plume.data.repository.AppLanguage
import com.example.plume.ui.PlumeRoot
import com.example.plume.ui.main.MainScreen
import com.example.plume.ui.main.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved locale before setContent so UI language is correct from the start
        val prefs = getSharedPreferences("plume_settings", MODE_PRIVATE)
        val savedLang = prefs.getString("app_lang", AppLanguage.RUSSIAN.code) ?: AppLanguage.RUSSIAN.code
        val localeList = LocaleListCompat.forLanguageTags(savedLang)
        AppCompatDelegate.setApplicationLocales(localeList)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            // Root composable comes from :shared (commonMain) — theming + surface
            // are platform-agnostic, while MainScreen/navigation stay in :app.
            PlumeRoot {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
