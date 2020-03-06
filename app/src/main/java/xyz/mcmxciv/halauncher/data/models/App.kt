package xyz.mcmxciv.halauncher.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class App(
    @PrimaryKey
    @ColumnInfo(name = "activity_name")
    val activityName: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "last_update")
    var lastUpdate: Long,
    @ColumnInfo(name = "icon", typeAffinity = ColumnInfo.BLOB)
    var icon: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as App

        if (activityName != other.activityName) return false
        if (packageName != other.packageName) return false
        if (displayName != other.displayName) return false
        if (lastUpdate != other.lastUpdate) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon!!.contentEquals(other.icon!!)) return false
        } else if (other.icon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activityName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + lastUpdate.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        return result
    }
}