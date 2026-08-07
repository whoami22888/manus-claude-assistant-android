package com.manus.assistant

import android.content.Context

/** Stores a single local plan; plans are never sent to a provider by this class. */
class AgentPlanStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AgentPlan? = preferences.getString(KEY_PLAN, null)?.let(::agentPlanFromJson)
    fun save(plan: AgentPlan) { preferences.edit().putString(KEY_PLAN, plan.toJson()).apply() }
    fun clear() { preferences.edit().remove(KEY_PLAN).apply() }

    private companion object {
        const val PREFS_NAME = "agent_plan"
        const val KEY_PLAN = "current_plan"
    }
}
