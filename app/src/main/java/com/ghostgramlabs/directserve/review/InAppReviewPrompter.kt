package com.ghostgramlabs.directserve.review

import android.content.Context

/**
 * Decides when to show Google Play's in-app review sheet: only right after a share that actually
 * worked (someone connected or data was sent), never on the first one, and at most once every
 * [MIN_DAYS_BETWEEN_PROMPTS] days. Play applies its own quota on top and may silently skip it.
 */
class InAppReviewPrompter(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Records a finished share and returns true when this is a good moment to ask for a review. */
    fun onShareFinished(wasUseful: Boolean, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!wasUseful) return false
        val successfulShares = prefs.getInt(KEY_SUCCESSFUL_SHARES, 0) + 1
        prefs.edit().putInt(KEY_SUCCESSFUL_SHARES, successfulShares).apply()
        if (successfulShares < MIN_SUCCESSFUL_SHARES) return false
        val lastPromptMs = prefs.getLong(KEY_LAST_PROMPT_MS, 0L)
        if (lastPromptMs != 0L && nowMs - lastPromptMs < MIN_DAYS_BETWEEN_PROMPTS * DAY_MS) return false
        prefs.edit().putLong(KEY_LAST_PROMPT_MS, nowMs).apply()
        return true
    }

    private companion object {
        const val PREFS_NAME = "directserve_review"
        const val KEY_SUCCESSFUL_SHARES = "successful_shares"
        const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
        const val MIN_SUCCESSFUL_SHARES = 2
        const val MIN_DAYS_BETWEEN_PROMPTS = 120L
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
