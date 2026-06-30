package ash.app.journal

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ash.app.journal.ui.JournalViewModel
import ash.app.journal.ui.MainJournalScreen
import ash.app.journal.ui.theme.JourNaLTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setOnExitAnimationListener { splashScreenView ->

            val fadeInAnimator = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                0f, 1f
            )

            val fadeOutAnimator = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f, 0f,
            )

            // Play them together using an AnimatorSet wrapper
            AnimatorSet().apply {
                duration = 300L
                interpolator = AnticipateInterpolator()
                playTogether(fadeInAnimator, fadeOutAnimator)

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }
                })
                start()
            }
        }
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
