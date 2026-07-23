package ash.app.journal

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
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
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.theme.JourNaLTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    lateinit var viewModel: JournalViewModel

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
        val linkRepository = appContainer.linkMetadataRepository

        // Factory container pattern to instantiate Custom ViewModel Parameter signatures cleanly
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return JournalViewModel(repository, linkRepository) as T
            }
        }

        viewModel = ViewModelProvider(this, viewModelFactory)[JournalViewModel::class.java]

        // Handle incoming intent data stream if launched via systemic share actions
        handleSharedIntent(intent)

        setContent {
            JourNaLTheme {
                MainJournalScreen(viewModel = viewModel)
            }
        }
    }

    // Capture case patterns where the app instance is already open in the background memory register
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type != null) {
            if (intent.type?.startsWith("text/") == true) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    // Hand off straight to the details field in the draft state!
                    viewModel.stageSharedTextIntoDraft(sharedText)
                    intent.removeExtra(Intent.EXTRA_TEXT)
                    intent.action = null
                }
            } else {
                // Retrieve the incoming content stream pointer target safely
                val streamUri =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }

                streamUri?.let { uri ->
                    val mimeType = intent.type ?: ""
                    processAndStageSharedMedia(uri, mimeType)
                }
            }
        }
    }

    private fun processAndStageSharedMedia(uri: Uri, mimeType: String) {
        try {
            // 1. Identify destination matching schema structure tags
            val (prefix, suffix, mediaType) = when {
                mimeType.startsWith("image/") -> Triple("shared_img_", ".jpg", EntryMediaType.PHOTO)
                mimeType.startsWith("video/") -> Triple("shared_vid_", ".mp4", EntryMediaType.VIDEO)
                mimeType.startsWith("audio/") -> Triple("shared_aud_", ".m4a", EntryMediaType.AUDIO)
                else -> return // Bail safely if unmapped format anomalies occur
            }

            // 2. Stream and write the bytes out to a safe app caching directory layout branch
            val targetDir = File(cacheDir, "journal_shared").apply { mkdirs() }
            val tempFile = File.createTempFile(prefix, suffix, targetDir)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            // 3. Hand off the freshly saved local file absolute path directly to your draft engine!
            viewModel.stageSharedMediaIntoDraft(tempFile.absolutePath, mediaType)
            // 4. Clean the incoming intent footprint so it doesn't re-trigger on device orientation updates
            intent.removeExtra(Intent.EXTRA_STREAM)
            intent.action = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
