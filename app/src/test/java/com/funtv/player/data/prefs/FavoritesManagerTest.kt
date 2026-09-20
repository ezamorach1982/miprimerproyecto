package com.funtv.player.data.prefs

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritesManagerTest {

    private lateinit var manager: FavoritesManager

    @Before
    fun setUp() {
        manager = FavoritesManager(ApplicationProvider.getApplicationContext())
    }

    private fun sampleEntry(id: Int = 1): FavoriteEntry = FavoriteEntry(
        key = FavoritesManager.keyFor(FavoriteType.VOD, id),
        type = FavoriteType.VOD,
        id = id,
        title = "Película de prueba"
    )

    @Test
    fun `content not marked as favorite by default`() {
        assertFalse(manager.isFavorite(FavoritesManager.keyFor(FavoriteType.VOD, 1)))
    }

    @Test
    fun `toggle marks and unmarks a favorite`() {
        val entry = sampleEntry()

        val nowFavorite = manager.toggle(entry)
        assertTrue(nowFavorite)
        assertTrue(manager.isFavorite(entry.key))

        val nowNotFavorite = manager.toggle(entry)
        assertFalse(nowNotFavorite)
        assertFalse(manager.isFavorite(entry.key))
    }

    @Test
    fun `getFavorites returns only what was added, newest first`() {
        manager.toggle(sampleEntry(1))
        manager.toggle(sampleEntry(2))

        val favorites = manager.getFavorites()
        assertEquals(2, favorites.size)
        assertEquals(sampleEntry(2).key, favorites.first().key)
    }

    @Test
    fun `clearAll removes every favorite`() {
        manager.toggle(sampleEntry(1))
        manager.toggle(sampleEntry(2))

        manager.clearAll()

        assertTrue(manager.getFavorites().isEmpty())
    }

    @Test
    fun `keyFor is stable and distinguishes types with the same id`() {
        val vodKey = FavoritesManager.keyFor(FavoriteType.VOD, 42)
        val seriesKey = FavoritesManager.keyFor(FavoriteType.SERIES, 42)

        assertEquals(vodKey, FavoritesManager.keyFor(FavoriteType.VOD, 42))
        assertTrue(vodKey != seriesKey)
    }
}
