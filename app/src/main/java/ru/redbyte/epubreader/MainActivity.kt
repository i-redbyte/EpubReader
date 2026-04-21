package ru.redbyte.epubreader

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.redbyte.epubreader.R
import ru.redbyte.epubreader.ui.reader.LocalReaderViewModelFactory
import ru.redbyte.epubreader.ui.reader.ReaderScreen
import ru.redbyte.epubreader.ui.theme.EpubReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val lightScrim = ContextCompat.getColor(this, R.color.bar_light_scrim)
        val darkScrim = ContextCompat.getColor(this, R.color.bar_dark_scrim)
        val barStyle = SystemBarStyle.light(lightScrim, darkScrim)
        enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle,
        )
        val readerViewModelFactory =
            (application as EpubReaderApplication).appComponent.readerViewModelFactory()
        setContent {
            CompositionLocalProvider(LocalReaderViewModelFactory provides readerViewModelFactory) {
                EpubReaderTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        EpubReaderNavHost()
                    }
                }
            }
        }
    }
}

@Composable
private fun EpubReaderNavHost() {
    val navController = rememberNavController()
    val startRoute = stringResource(R.string.nav_route_reader)
    NavHost(navController = navController, startDestination = startRoute) {
        composable(startRoute) {
            ReaderScreen()
        }
    }
}
