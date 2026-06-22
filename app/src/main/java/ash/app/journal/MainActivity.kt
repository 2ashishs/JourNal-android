package ash.app.journal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ash.app.journal.ui.JournalViewModel
import ash.app.journal.ui.MainJournalScreen
import ash.app.journal.ui.theme.JourNaLTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract instances from custom Application scope container
        val appContainer = application as JournalApplication
        val repository = appContainer.repository

        // Factory container pattern to instantiate Custom ViewModel Parameter signatures cleanly
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return JournalViewModel(repository) as T
            }
        }

        val viewModel = ViewModelProvider(this, viewModelFactory)[JournalViewModel::class.java]

        setContent {
            JourNaLTheme {
                MainJournalScreen(viewModel = viewModel)
            }
        }
    }

}
