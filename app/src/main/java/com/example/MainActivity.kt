package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.SQPlusTheme
import com.example.ui.viewmodel.SqPlusViewModel
import com.example.ui.viewmodel.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SqPlusViewModel = viewModel()
            val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()

            val isDark = when (themePreference) {
                ThemePreference.SYSTEM -> systemInDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            SQPlusTheme(darkTheme = isDark) {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}
