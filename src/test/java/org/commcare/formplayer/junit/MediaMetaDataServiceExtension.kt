package org.commcare.formplayer.junit

import org.commcare.formplayer.exceptions.MediaMetaDataNotFoundException
import org.commcare.formplayer.objects.MediaMetadataRecord
import org.commcare.formplayer.services.MediaMetaDataService
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.test.context.junit.jupiter.SpringExtension

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
            metadataMap[mediaMetadataRecord.fileId] = mediaMetadataRecord
            mediaMetadataRecord
        }.`when`(mediaMetaDataService).saveMediaMetaData(
            ArgumentMatchers.any(
                MediaMetadataRecord::class.java
            )
        )

        Mockito.`when`(mediaMetaDataService.findByFileId(ArgumentMatchers.anyString()))
            .thenAnswer(
                Answer { invocation ->
                    val fileId = invocation.arguments[0] as String
                    if (metadataMap.containsKey(fileId)) {
                        return@Answer metadataMap[fileId]
                    }
                    throw MediaMetaDataNotFoundException(fileId)
                }
            )

        Mockito.`when`(mediaMetaDataService.deleteByFileId(ArgumentMatchers.anyString()))
            .thenAnswer(
                Answer { invocation ->
                    val key = invocation.arguments[0] as String
                    metadataMap.remove(key)
                }

            )

        Mockito.`when`(mediaMetaDataService.findAllWithNullFormSession())
            .thenAnswer(
                Answer { invocation ->
                    var mediaRecordList = arrayListOf<MediaMetadataRecord>()
                    for (record in metadataMap) {
                        if (record.value.formSession == null) {
                            mediaRecordList.add(record.value)
                        }
                    }

                    return@Answer mediaRecordList
                }
            )
    }
}
