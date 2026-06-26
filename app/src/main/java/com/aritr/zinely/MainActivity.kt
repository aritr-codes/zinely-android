package com.aritr.zinely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritr.zinely.editor.ZinelyNavHost
import com.aritr.zinely.ui.theme.ZinelyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity (ADR-030 §1). Hosts the navigation-compose graph; all screens are composable
 * destinations. `@AndroidEntryPoint` lets `hiltViewModel()` resolve the editor's `@HiltViewModel`
 * against the `:data-android` SingletonComponent graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZinelyApp()
        }
    }
}

@Composable
private fun ZinelyApp() {
    ZinelyTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ZinelyNavHost(modifier = Modifier.fillMaxSize().padding(innerPadding))
        }
    }
}
