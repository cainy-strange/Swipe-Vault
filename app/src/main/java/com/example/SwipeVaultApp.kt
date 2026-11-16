package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.local.SwipeVaultDatabase
import com.example.data.media.MediaStoreRepository
import com.example.data.media.StorageInspector
import com.example.data.repository.SwipeRepository

class AppContainer(application: Application) {
    val database: SwipeVaultDatabase by lazy {
        SwipeVaultDatabase.getInstance(application)
    }
    val mediaStoreRepository: MediaStoreRepository by lazy {
        MediaStoreRepository(application)
    }
    val storageInspector: StorageInspector by lazy {
        StorageInspector()
    }
    val swipeRepository: SwipeRepository by lazy {
        SwipeRepository(
            mediaStoreRepository = mediaStoreRepository,
            dao = database.swipeVaultDao(),
            storageInspector = storageInspector
        )
    }
}

class SwipeVaultApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
