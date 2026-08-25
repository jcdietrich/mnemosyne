package com.mnemosyne.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

@Entity
data class Memory(
    @Id var id: Long = 0,
    var transcript: String = "",
    var timestampUtcMs: Long = 0,
    var latitudeDeg: Double = 0.0,
    var longitudeDeg: Double = 0.0,
    var locationName: String? = null,
    @HnswIndex(dimensions = 100)
    var embeddingVector: FloatArray = FloatArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Memory

        if (id != other.id) return false
        if (transcript != other.transcript) return false
        if (timestampUtcMs != other.timestampUtcMs) return false
        if (latitudeDeg != other.latitudeDeg) return false
        if (longitudeDeg != other.longitudeDeg) return false
        if (locationName != other.locationName) return false
        if (!embeddingVector.contentEquals(other.embeddingVector)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + transcript.hashCode()
        result = 31 * result + timestampUtcMs.hashCode()
        result = 31 * result + latitudeDeg.hashCode()
        result = 31 * result + longitudeDeg.hashCode()
        result = 31 * result + locationName.hashCode()
        result = 31 * result + embeddingVector.contentHashCode()
        return result
    }
}
