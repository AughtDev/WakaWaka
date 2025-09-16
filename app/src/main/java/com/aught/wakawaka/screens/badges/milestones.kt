package com.aught.wakawaka.screens.badges

import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

data class Milestone(
    val hours: Int,
    val colorHex: String,
    val crown: DrawScope.(Color, Int) -> Unit,
    val badge: Canvas.(Int, Float, Float) -> Unit
)

val MILESTONES: List<Milestone> = listOf(
    Milestone(25, "#B8702E", DrawScope::bronzeCrown, Canvas::drawBronzeCrown),
    Milestone(50, "#C0C0C0", DrawScope::silverCrown, Canvas::drawSilverCrown),
    Milestone(100, "#CFB53B", DrawScope::goldCrown, Canvas::drawGoldCrown),
    Milestone(250, "#00A693", DrawScope::diamondCrown, Canvas::drawDiamondCrown),
    Milestone(500, "#9F7FF5", DrawScope::royalPurpleCrown, Canvas::drawRoyalPurpleCrown),
    Milestone(1000, "#F56058", DrawScope::royalRedCrown, Canvas::drawRoyalRedCrown),
    Milestone(5000, "#2FC87C", DrawScope::royalRedCrown, Canvas::drawRoyalRedCrown),
)

fun getMilestoneIndex(totalHours: Int): Int {
    var milestoneIndex = 0
    while (milestoneIndex < MILESTONES.size && totalHours >= MILESTONES[milestoneIndex].hours) {
        milestoneIndex++
    }
    return milestoneIndex - 1
}
