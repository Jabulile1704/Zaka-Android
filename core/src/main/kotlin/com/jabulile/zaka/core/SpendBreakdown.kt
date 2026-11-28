package com.jabulile.zaka.core

/**
 * One wedge of the month's spend. A null [category] is the merged remainder:
 * sixteen categories would be sixteen unreadable slices, so the small ones are
 * added together.
 */
data class SpendSlice(
    val category: ExpenseCategory?,
    val amount: Double,
    val mergedCount: Int = 0,
) {
    val id: String get() = category?.raw ?: "merged"

    val label: String get() = category?.label ?: "+$mergedCount more"

    val isMerged: Boolean get() = category == null

    /** This slice's share of [total], and never a division by zero. */
    fun share(total: Double): Double = if (total > 0) amount / total else 0.0
}

/** What was drawn of each slice at some point through the reveal animation. */
data class Sweep(
    val drawn: List<Double>,
    val remainder: Double,
)

object SpendBreakdown {
    /** Sums every expense per category, dropping nothing. */
    fun totals(expenses: List<Expense>): Map<ExpenseCategory, Double> {
        val running = LinkedHashMap<ExpenseCategory, Double>()
        for (expense in expenses) {
            running[expense.category] = (running[expense.category] ?: 0.0) + expense.amount
        }
        return running
    }

    /**
     * Ranks the categories by spend and keeps at most [limit] slices, merging
     * whatever is left over into one. Equal amounts are ordered by label, so
     * the chart does not reshuffle itself between runs.
     */
    fun slices(
        totals: Map<ExpenseCategory, Double>,
        limit: Int = 6,
    ): List<SpendSlice> {
        if (limit <= 0) return emptyList()

        val ranked = totals.entries
            .filter { it.value > 0 }
            .sortedWith(
                compareByDescending<Map.Entry<ExpenseCategory, Double>> { it.value }
                    .thenBy { it.key.label },
            )
            .map { SpendSlice(it.key, it.value) }

        if (ranked.size <= limit) return ranked

        val kept = ranked.take(limit - 1)
        val merged = ranked.drop(limit - 1)

        return kept + SpendSlice(
            category = null,
            amount = merged.sumOf { it.amount },
            mergedCount = merged.size,
        )
    }

    /**
     * The donut part-way through its reveal. [progress] is clamped to 0...1 and
     * the remainder keeps the circle whole at every step.
     */
    fun sweep(slices: List<SpendSlice>, progress: Double): Sweep {
        val clamped = progress.coerceIn(0.0, 1.0)
        val total = slices.sumOf { it.amount }
        val drawn = slices.map { it.amount * clamped }
        return Sweep(drawn, total - drawn.sum())
    }
}
