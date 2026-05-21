package com.aught.wakawaka.data

import android.graphics.Canvas
import com.aught.wakawaka.screens.badges.drawBronzeCrown
import com.aught.wakawaka.screens.badges.drawDiamondCrown
import com.aught.wakawaka.screens.badges.drawGoldCrown
import com.aught.wakawaka.screens.badges.drawRoyalPurpleCrown
import com.aught.wakawaka.screens.badges.drawSilverCrown

enum class CompletionTier {
    None, Bronze, Silver, Gold, Diamond, RoyalPurple
}

object CompletionTierConfig {
    // Cutoffs sorted ascending by HHMM. The tier wins when the completion HHMM is <= cutoff.
    // Past the last cutoff -> CompletionTier.None.
    val cutoffs: List<Pair<Int, CompletionTier>> = listOf(
        930 to CompletionTier.RoyalPurple,
        1100 to CompletionTier.Diamond,
        1300 to CompletionTier.Gold,
        1600 to CompletionTier.Silver,
        1900 to CompletionTier.Bronze,
    )

    // Hex colors reused from the milestone badge palette in screens/badges/milestones.kt.
    val colors: Map<CompletionTier, String> = mapOf(
        CompletionTier.Bronze to "#B8702E",
        CompletionTier.Silver to "#C0C0C0",
        CompletionTier.Gold to "#CFB53B",
        CompletionTier.Diamond to "#00A693",
        CompletionTier.RoyalPurple to "#9F7FF5",
    )

    val canvasCrown: Map<CompletionTier, Canvas.(Int, Float, Float) -> Unit> = mapOf(
        CompletionTier.Bronze to Canvas::drawBronzeCrown,
        CompletionTier.Silver to Canvas::drawSilverCrown,
        CompletionTier.Gold to Canvas::drawGoldCrown,
        CompletionTier.Diamond to Canvas::drawDiamondCrown,
        CompletionTier.RoyalPurple to Canvas::drawRoyalPurpleCrown,
    )

    const val COMPLETION_HISTORY_CONSIDERED = 21

    // Number of seconds past each cutoff at which the scheduled tier-boundary fetch fires.
    // Small buffer to ensure we capture state slightly after the boundary rather than slightly before.
    const val TIER_FETCH_BUFFER_SECONDS = 30

    fun tierForHHMM(hhmm: Int): CompletionTier {
        for ((cutoff, tier) in cutoffs) {
            if (hhmm <= cutoff) return tier
        }
        return CompletionTier.None
    }

    fun cutoffHHMMs(): List<Int> = cutoffs.map { it.first }

    fun hhmmToMinutes(hhmm: Int): Int = (hhmm / 100) * 60 + (hhmm % 100)

    fun minutesToHHMM(minutes: Int): Int {
        val clamped = minutes.coerceIn(0, 24 * 60)
        return (clamped / 60) * 100 + (clamped % 60)
    }
}
