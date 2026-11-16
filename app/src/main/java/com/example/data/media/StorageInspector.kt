package com.example.data.media

import android.os.Environment
import android.os.StatFs
import com.example.data.model.StorageSnapshot

class StorageInspector {
    fun getSnapshot(): StorageSnapshot {
        return runCatching {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val total = stat.totalBytes
            val available = stat.availableBytes
            StorageSnapshot(totalBytes = total, availableBytes = available)
        }.getOrElse {
            StorageSnapshot(totalBytes = 0L, availableBytes = 0L)
        }
    }

    fun getAvailableBytes(): Long = getSnapshot().availableBytes
}
