package icedamericano.tooling.color

data class Matrix3(val row0: Vector3, val row1: Vector3, val row2: Vector3) {
    operator fun times(vector: Vector3): Vector3 =
        Vector3(row0.dot(vector), row1.dot(vector), row2.dot(vector))

    // Clip only after each full dot product; allocate only the final result vector.
    fun transformClamped(vector: Vector3, min: Double, max: Double): Vector3 =
        Vector3(
            row0.dot(vector).coerceIn(min, max),
            row1.dot(vector).coerceIn(min, max),
            row2.dot(vector).coerceIn(min, max),
        )
}
