package app.trainer.android

import app.trainer.feature.progress.presentation.photos.mvi.CompareSide
import app.trainer.feature.progress.presentation.photos.mvi.PhotoCompareState
import app.trainer.feature.progress.presentation.photos.mvi.PhotoShot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

private const val JUNE_PHOTO = "photo-june"
private const val JULY_PHOTO = "photo-july"
private const val AUGUST_PHOTO = "photo-august"

class PhotoCompareStateTest {

    @Test
    fun `picking a shot fills the selected side`() {
        val state = threeShots(selectedSide = CompareSide.Before)

        val picked = state.withShotPicked(JULY_PHOTO)

        assertEquals(JULY_PHOTO, picked.beforePhotoId)
        assertEquals(AUGUST_PHOTO, picked.afterPhotoId)
    }

    @Test
    fun `a shot taken from the other side swaps the two`() {
        val state = threeShots(selectedSide = CompareSide.Before)

        val picked = state.withShotPicked(AUGUST_PHOTO)

        assertEquals(AUGUST_PHOTO, picked.beforePhotoId)
        assertEquals(JUNE_PHOTO, picked.afterPhotoId, "снимок «до» должен уехать на другую сторону")
    }

    @Test
    fun `the same shot never ends up on both sides`() {
        val state = threeShots(selectedSide = CompareSide.After)

        val picked = state.withShotPicked(JUNE_PHOTO)

        assertFalse(picked.beforePhotoId == picked.afterPhotoId, "сравнивать снимок с самим собой нечего")
    }

    @Test
    fun `a single shot is not enough to compare`() {
        val state = threeShots(selectedSide = CompareSide.After)
            .copy(shots = persistentListOf(shot(JUNE_PHOTO)))

        assertFalse(state.canCompare)
        assertTrue(threeShots(selectedSide = CompareSide.After).canCompare)
    }

    private fun threeShots(selectedSide: CompareSide): PhotoCompareState = PhotoCompareState.initial().copy(
        shots = persistentListOf(shot(JUNE_PHOTO), shot(JULY_PHOTO), shot(AUGUST_PHOTO)),
        beforePhotoId = JUNE_PHOTO,
        afterPhotoId = AUGUST_PHOTO,
        selectedSide = selectedSide,
        isLoading = false,
    )

    private fun shot(photoId: String): PhotoShot = PhotoShot(
        photoId = photoId,
        url = "https://example.invalid/$photoId.jpg",
        dateIso = "2026-06-03",
        dateLabel = "3 июня",
    )
}
