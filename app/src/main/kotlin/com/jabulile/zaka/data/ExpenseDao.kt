package com.jabulile.zaka.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Reads return `Flow`, writes suspend.
 *
 * SwiftData re-ran a `@Query` view whenever the store changed; a Room `Flow`
 * is the same idea with the wiring visible. Every ordering is `date DESC, id
 * DESC` so two expenses logged in the same millisecond still come back in a
 * stable order — without the tiebreak the list can reshuffle on recomposition.
 */
@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE date >= :from AND date < :until
        ORDER BY date DESC, id DESC
        """,
    )
    fun observeBetween(from: Long, until: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :from AND date < :until")
    fun observeTotalBetween(from: Long, until: Long): Flow<Double>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun find(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
