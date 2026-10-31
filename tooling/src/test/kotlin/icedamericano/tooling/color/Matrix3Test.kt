package icedamericano.tooling.color

import kotlin.test.Test
import kotlin.test.assertEquals

class Matrix3Test {
    @Test
    fun multiplicationUsesRowsAndDoesNotClamp() {
        val matrix =
            Matrix3(
                row0 = Vector3(1.0, 2.0, 3.0),
                row1 = Vector3(-4.0, 5.0, 6.0),
                row2 = Vector3(7.0, 8.0, -9.0),
            )
        assertEquals(Vector3(14.0, 24.0, -4.0), matrix * Vector3(1.0, 2.0, 3.0))
    }

    @Test
    fun clampedTransformClipsCompletedSumsNotIndividualTerms() {
        val matrix =
            Matrix3(
                row0 = Vector3(2.0, -1.5, 0.0),
                row1 = Vector3(-2.0, 0.5, 0.0),
                row2 = Vector3(0.0, 1.0, 1.0),
            )
        assertEquals(
            Vector3(0.5, 0.0, 1.0),
            matrix.transformClamped(Vector3(1.0, 1.0, 1.0), 0.0, 1.0),
        )
    }
}
