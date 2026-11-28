package com.jabulile.zaka.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards ORDER BY added_at DESC, id DESC")
    fun observeAll(): Flow<List<LoyaltyCardEntity>>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id")
    suspend fun find(id: Long): LoyaltyCardEntity?

    @Insert
    suspend fun insert(card: LoyaltyCardEntity): Long

    @Update
    suspend fun update(card: LoyaltyCardEntity)

    @Delete
    suspend fun delete(card: LoyaltyCardEntity)

    @Query("DELETE FROM loyalty_cards WHERE id = :id")
    suspend fun deleteById(id: Long)
}
