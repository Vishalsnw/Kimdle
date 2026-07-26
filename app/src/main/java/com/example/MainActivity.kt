package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          KindleAppNavigation()
        }
      }
    }
  }
}

@Composable
fun KindleAppNavigation() {
  val navController = rememberNavController()
  val libraryViewModel: LibraryViewModel = viewModel()
  val readerViewModel: ReaderViewModel = viewModel()

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
