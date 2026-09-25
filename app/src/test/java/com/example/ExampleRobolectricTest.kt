package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Platform
import com.example.data.model.VideoQuality
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Social Video Saver", appName)
    }

    @Test
    fun `platform detection from urls`() {
        assertEquals(Platform.YOUTUBE, Platform.fromUrl("https://youtube.com/shorts/xyz123"))
        assertEquals(Platform.INSTAGRAM, Platform.fromUrl("https://instagram.com/reel/C12345/"))
        assertEquals(Platform.FACEBOOK, Platform.fromUrl("https://fb.watch/video123"))
        assertEquals(Platform.TIKTOK, Platform.fromUrl("https://tiktok.com/@user/video/999"))
        assertEquals(Platform.SNAPCHAT, Platform.fromUrl("https://story.snapchat.com/s/123"))
        assertEquals(Platform.DIRECT_MEDIA, Platform.fromUrl("https://example.com/video.mp4"))
    }

    @Test
    fun `format utils verification`() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
        assertTrue(FormatUtils.formatBytes(15_000_000L).contains("MB"))
        assertTrue(FormatUtils.formatSpeed(2_500_000L).contains("MB/s"))
        assertEquals("4s remaining", FormatUtils.formatEta(4))
    }
}
