package com.example

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
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          ReaderAppNavigation(readerViewModel)
        }
      }
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      if (readerViewModel.onVolumeKeyDown()) {
        return true
      }
    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      if (readerViewModel.onVolumeKeyUp()) {
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }
}

@Composable
fun ReaderAppNavigation(readerViewModel: ReaderViewModel = viewModel()) {
  val navController = rememberNavController()
  val libraryViewModel: LibraryViewModel = viewModel()

  NavHost(navController = navController, startDestination = "library") {
    composable("library") {
      LibraryScreen(
        viewModel = libraryViewModel,
        onBookClick = { bookId ->
          navController.navigate("reader/$bookId")
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
