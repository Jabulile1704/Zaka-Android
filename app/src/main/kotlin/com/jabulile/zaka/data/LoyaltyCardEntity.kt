package com.jabulile.zaka.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A loyalty card as the database holds it. */
@Entity(tableName = "loyalty_cards")
data class LoyaltyCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val number: String,
    @ColumnInfo(name = "symbology")
    val symbologyRaw: String,
    @ColumnInfo(name = "tint_hex")
    val tintHex: String,
    @ColumnInfo(name = "added_at")
    val addedAtEpochMillis: Long,
)
