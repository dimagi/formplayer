package org.commcare.formplayer.junit

import org.aspectj.lang.annotation.Before
import org.commcare.formplayer.exceptions.FormNotFoundException
import org.commcare.formplayer.objects.MediaMetadataRecord
import org.commcare.formplayer.services.MediaMetaDataService
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.cache.CacheManager
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

/**
 * Junit extension that configures the mock MediaMetaDataService
 */
class MediaMetaDataServiceExtension : BeforeAllCallback, BeforeEachCallback {
    private lateinit var mediaMetaDataService: MediaMetaDataService
    private val metadataMap: MutableMap<String, MediaMetadataRecord> = HashMap()

    override fun beforeAll(context: ExtensionContext) {
        mediaMetaDataService = SpringExtension.getApplicationContext(context).getBean(MediaMetaDataService::class.java)
    }

    override fun beforeEach(context: ExtensionContext?) {
        metadataMap.clear()

        Mockito.doAnswer { invocation ->
            val mediaMetadataRecord = invocation.arguments[0] as MediaMetadataRecord
            metadataMap[mediaMetadataRecord.id] = mediaMetadataRecord
            mediaMetadataRecord
        }.`when`(mediaMetaDataService).saveMediaMetaData(
            ArgumentMatchers.any(
                MediaMetadataRecord::class.java
            )
        )
    }
}