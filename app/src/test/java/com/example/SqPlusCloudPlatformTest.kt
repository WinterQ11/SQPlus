package com.example

import com.example.data.model.ChannelEntity
import com.example.data.model.FileEntity
import com.example.data.repository.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SqPlusCloudPlatformTest {

    @Test
    fun testCleanLibraryStartup_noPreaddedDemoFilesOrChannels() {
        // Verify SeedData default lists are completely empty
        assertTrue("SeedData default channels must be empty", SeedData.defaultChannels.isEmpty())
        assertTrue("SeedData default files must be empty", SeedData.defaultFiles.isEmpty())

        // Verify demo channel ID detection
        assertTrue(SeedData.isDemoChannel("chan_exec_01"))
        assertTrue(SeedData.isDemoChannel("chan_mobile_02"))
        assertFalse(SeedData.isDemoChannel("chan_real_user_01"))

        // Verify demo file ID detection
        assertTrue(SeedData.isDemoFile("file_exec_01"))
        assertFalse(SeedData.isDemoFile("cloud_file_xyz123"))
    }

    @Test
    fun testChannelCreation_andFileAssociation() {
        val channelId = "chan_" + UUID.randomUUID().toString().take(8)
        val channel = ChannelEntity(
            id = channelId,
            name = "Android Software Releases",
            description = "Production signed APKs and binaries",
            category = "Software",
            iconName = "android",
            fileCount = 1,
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals("Android Software Releases", channel.name)
        assertEquals("Software", channel.category)

        val file = FileEntity(
            id = "file_" + UUID.randomUUID().toString().take(8),
            channelId = channelId,
            name = "sqplus-client-release.apk",
            description = "Official client application binary",
            fileType = "APK",
            fileSizeBytes = 28_000_000L,
            uploadTimestamp = System.currentTimeMillis(),
            downloadCount = 0,
            category = "Software",
            downloadUrl = "https://sqplus-1b0e9.supabase.co/storage/v1/object/public/sqplus-documents/channels/chan_1/file_1/sqplus-client-release.apk",
            isPublished = true
        )

        assertEquals(channelId, file.channelId)
        assertEquals("sqplus-client-release.apk", file.name)
        assertEquals("APK", file.fileType)
        assertTrue(file.downloadUrl.isNotBlank())
        assertTrue(file.isPublished)
    }

    @Test
    fun testPublishStatusWorkflow_normalUsersVsAdmin() {
        val publishedDoc = FileEntity(
            id = "doc_pub_1",
            channelId = "ch_docs",
            name = "Quarterly_Financials.pdf",
            description = "Audited figures",
            fileType = "PDF",
            fileSizeBytes = 1024L * 1024L,
            isPublished = true
        )

        val draftDoc = FileEntity(
            id = "doc_draft_1",
            channelId = "ch_docs",
            name = "Internal_Memo_Draft.docx",
            description = "Unpublished draft",
            fileType = "DOCX",
            fileSizeBytes = 512L * 1024L,
            isPublished = false
        )

        val allDocs = listOf(publishedDoc, draftDoc)

        // Filter for normal users (only published content)
        val normalUserDocs = allDocs.filter { it.isPublished }
        assertEquals(1, normalUserDocs.size)
        assertEquals("Quarterly_Financials.pdf", normalUserDocs.first().name)
        assertFalse(normalUserDocs.any { it.name == "Internal_Memo_Draft.docx" })

        // Admin sees all documents
        val adminDocs = allDocs
        assertEquals(2, adminDocs.size)

        // Admin publishes draft
        val newlyPublishedDoc = draftDoc.copy(isPublished = true)
        val updatedDocs = listOf(publishedDoc, newlyPublishedDoc)
        val updatedNormalUserDocs = updatedDocs.filter { it.isPublished }
        assertEquals(2, updatedNormalUserDocs.size)
    }

    @Test
    fun testFileMove_betweenChannels() {
        val file = FileEntity(
            id = "file_test_move",
            channelId = "ch_source",
            name = "release_notes.pdf",
            description = "Notes",
            fileType = "PDF",
            fileSizeBytes = 1024L,
            category = "Reports",
            isPublished = true
        )

        val movedFile = file.copy(channelId = "ch_destination")
        assertEquals("ch_destination", movedFile.channelId)
        assertEquals(file.id, movedFile.id)
        assertEquals(file.name, movedFile.name)
        assertTrue(movedFile.isPublished)
    }

    @Test
    fun testSupportedFileTypes_andIntegrity() {
        val validTypes = listOf("APK", "PDF", "ZIP", "DOCX", "PPTX", "XLSX", "MP4")
        for (type in validTypes) {
            val file = FileEntity(
                id = "test_$type",
                channelId = "chan_all",
                name = "test_file.$type",
                description = "Valid payload",
                fileType = type,
                fileSizeBytes = 4096L,
                category = "General",
                isPublished = true
            )
            assertEquals(type, file.fileType)
            assertTrue(file.isPublished)
        }
    }
}
