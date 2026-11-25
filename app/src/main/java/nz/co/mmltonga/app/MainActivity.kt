package nz.co.mmltonga.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import nz.co.mmltonga.app.ui.MmlNavHost
import nz.co.mmltonga.app.ui.theme.MmlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as MmlApp).repo
        setContent {
            MmlTheme { MmlNavHost(repo) }
        }
    }
}
