package com.example

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import com.example.util.AdManager
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
  private val readerViewModel: ReaderViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      PDFBoxResourceLoader.init(applicationContext)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    // Initialize Google AdMob Mobile Ads SDK
    AdManager.initialize(applicationContext)

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          ReaderAppNavigation(readerViewModel)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Trigger App Open Ad if available
    AdManager.showAppOpenAdIfAvailable(this)
  }
}

@Composable
fun ReaderAppNavigation(readerViewModel: ReaderViewModel = viewModel()) {
  val context = LocalContext.current
  val activity = context as? Activity
  val navController = rememberNavController()
  val libraryViewModel: LibraryViewModel = viewModel()

  NavHost(navController = navController, startDestination = "library") {
    composable("library") {
      LibraryScreen(
        viewModel = libraryViewModel,
        onBookClick = { bookId ->
          if (activity != null) {
            AdManager.showInterstitialAd(activity) {
              navController.navigate("reader/$bookId")
            }
          } else {
            navController.navigate("reader/$bookId")
          }
        }
      )
    }
    composable(
      route = "reader/{bookId}",
      arguments = listOf(navArgument("bookId") { type = NavType.LongType })
    ) { backStackEntry ->
      val bookId = backStackEntry.arguments?.getLong("bookId") ?: -1L
      ReaderScreen(
        bookId = bookId,
        viewModel = readerViewModel,
        onBack = {
          navController.popBackStack()
        }
      )
    }
  }
}
