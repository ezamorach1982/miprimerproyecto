package com.funtv.player.data.prefs

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackPositionManagerTest {

    private lateinit var manager: PlaybackPositionManager

    @Before
    fun setUp() {
        manager = PlaybackPositionManager(ApplicationProvider.getApplicationContext())
    }

    private fun entry(
        url: String = "http://example.com/movie.mp4",
        positionMs: Long = 60_000L,
        durationMs: Long = 600_000L,
        updatedAt: Long = 1_000L,
        type: FavoriteType? = FavoriteType.VOD
    ) = ContinueWatchingEntry(
        url = url,
        title = "Película de prueba",
        positionMs = positionMs,
        durationMs = durationMs,
        updatedAt = updatedAt,
        type = type
    )

    @Test
    fun `getPosition returns 0 for an unknown url`() {
        assertEquals(0L, manager.getPosition("http://example.com/unknown.mp4"))
    }

    @Test
    fun `savePosition can be read back, including the content type`() {
        val saved = entry()
        manager.savePosition(saved)

        assertEquals(saved.positionMs, manager.getPosition(saved.url))
        val continueWatching = manager.getContinueWatching()
        assertEquals(1, continueWatching.size)
        assertEquals(FavoriteType.VOD, continueWatching.first().type)
    }

    @Test
    fun `entries saved before the type field existed deserialize with a null type`() {
        // Simula una entrada guardada por una versión anterior de la app, sin el campo "type".
        val prefs = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("funtv_playback_positions", android.content.Context.MODE_PRIVATE)
        val legacyJson = """{"http://example.com/old.mp4":{"url":"http://example.com/old.mp4","title":"Vieja","positionMs":1000,"durationMs":2000,"updatedAt":1}}"""
        prefs.edit().putString("entries", legacyJson).apply()

        val legacyManager = PlaybackPositionManager(ApplicationProvider.getApplicationContext())
        val entries = legacyManager.getContinueWatching()

        assertEquals(1, entries.size)
        assertNull(entries.first().type)
    }

    @Test
    fun `getProgress is clamped between 0 and 1`() {
        manager.savePosition(entry(positionMs = 900_000L, durationMs = 600_000L))
        assertEquals(1f, manager.getProgress("http://example.com/movie.mp4"))
    }

    @Test
    fun `clearPosition removes a single entry without affecting others`() {
        manager.savePosition(entry(url = "http://example.com/a.mp4"))
        manager.savePosition(entry(url = "http://example.com/b.mp4"))

        manager.clearPosition("http://example.com/a.mp4")

        assertEquals(0L, manager.getPosition("http://example.com/a.mp4"))
        assertTrue(manager.getPosition("http://example.com/b.mp4") > 0L)
    }

    @Test
    fun `getContinueWatching orders by most recently updated and respects the limit`() {
        manager.savePosition(entry(url = "http://example.com/a.mp4", updatedAt = 1L))
        manager.savePosition(entry(url = "http://example.com/b.mp4", updatedAt = 2L))
        manager.savePosition(entry(url = "http://example.com/c.mp4", updatedAt = 3L))

        val limited = manager.getContinueWatching(limit = 2)

        assertEquals(2, limited.size)
        assertEquals("http://example.com/c.mp4", limited[0].url)
        assertEquals("http://example.com/b.mp4", limited[1].url)
    }

    @Test
    fun `clearAll wipes every saved position`() {
        manager.savePosition(entry(url = "http://example.com/a.mp4"))
        manager.clearAll()

        assertTrue(manager.getContinueWatching().isEmpty())
    }
}
