package com.example.epubreader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.Streamer


import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class EpubViewModel(private val context: Context) : ViewModel() {
    private val readium = Readium(context)
    private var publicationTry: Try<Publication, Exception>? = null
    var isBookLoaded by mutableStateOf(false)
        private set
    private var publication: Publication? = null

    // Download the EPUB file from a URL
    fun downloadEpubFile(urlString: String, callback: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val file = File(context.cacheDir, "book.epub")
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            withContext(Dispatchers.Main) {
                callback(file)
            }
        }
    }

    // Load the publication from the downloaded EPUB file
    fun loadBook(urlString: String) {
        downloadEpubFile(urlString) { file ->
            file?.let {
                viewModelScope.launch(Dispatchers.IO) {
                    // Use the Readium's publicationOpener to open the publication from the file
                    val streamer = Streamer(context)
                    publicationTry = readium.publicationOpener.open(it, streamer)

                    withContext(Dispatchers.Main) {
                        publicationTry?.onSuccess { pub ->
                            publication = pub

                            isBookLoaded = true
                        }?.onFailure { error ->
                            // Handle error (log or show message)
                        }
                    }
                }
            }
        }
    }

    // Return the current publication
    fun getPublication(): Publication? = publication

    // Create an instance of EpubNavigatorFragment
    fun createNavigatorFragment(publication: Publication): EpubNavigatorFragment {
        val epubLayout = publication.metadata.presentation.layout ?: EpubLayout.REFLOWABLE

        return EpubNavigatorFragment(
            publication = publication,
            initialLocator = null,  // You can provide an initial locator if necessary
            readingOrder = publication.readingOrder,
            listener = null,  // Add listener if needed
            paginationListener = null,  // Add pagination listener if needed
            epubLayout = epubLayout,
            defaults = EpubNavigatorFragment.EpubDefaults(),
            configuration = EpubNavigatorFragment.Configuration()  // Default or customized configuration
        )
    }
}
