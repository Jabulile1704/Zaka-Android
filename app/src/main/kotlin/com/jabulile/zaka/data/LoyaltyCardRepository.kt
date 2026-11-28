package com.jabulile.zaka.data

import com.jabulile.zaka.core.LoyaltyCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoyaltyCardRepository(
    private val dao: LoyaltyCardDao,
) {
    fun observeAll(): Flow<List<LoyaltyCard>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun find(id: Long): LoyaltyCard? = dao.find(id)?.toDomain()

    suspend fun save(card: LoyaltyCard): Long =
        if (card.id == 0L) {
            dao.insert(card.toEntity())
        } else {
            dao.update(card.toEntity())
            card.id
        }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
