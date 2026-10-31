package icedamericano.tooling.color

data class Vector3(val x: Double, val y: Double, val z: Double) {
    fun dot(other: Vector3): Double = x * other.x + y * other.y + z * other.z
}
