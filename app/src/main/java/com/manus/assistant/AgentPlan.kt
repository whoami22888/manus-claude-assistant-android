package com.manus.assistant

import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only plan model adapted from MetaGPT's MIT-licensed Task/Plan data
 * shape: task id, dependencies, instruction, result, success and completion.
 * It deliberately contains no executable command, connector, or credential.
 */
data class AgentPlanTask(
    val id: String,
    val dependencies: List<String>,
    val instruction: String,
    val result: String = "",
    val isSuccessful: Boolean = false,
    val isFinished: Boolean = false
) {
    fun complete(result: String): AgentPlanTask = copy(
        result = result,
        isSuccessful = true,
        isFinished = true
    )

    fun reset(): AgentPlanTask = copy(result = "", isSuccessful = false, isFinished = false)
}

data class AgentPlan(val goal: String, val tasks: List<AgentPlanTask>) {
    val currentTask: AgentPlanTask?
        get() = tasks.firstOrNull { !it.isFinished && it.dependencies.all { dependency ->
            tasks.firstOrNull { it.id == dependency }?.isFinished == true
        } }

    fun complete(taskId: String, result: String = "Reviewed locally"): PlanUpdate {
        val task = tasks.firstOrNull { it.id == taskId }
            ?: return PlanUpdate(this, "No task named $taskId exists in this plan.")
        if (task.isFinished) return PlanUpdate(this, "Task $taskId is already complete.")
        val blockedBy = task.dependencies.filter { dependency ->
            tasks.firstOrNull { it.id == dependency }?.isFinished != true
        }
        if (blockedBy.isNotEmpty()) {
            return PlanUpdate(this, "Task $taskId is waiting for: ${blockedBy.joinToString(", ")}.")
        }
        val updated = copy(tasks = tasks.map { if (it.id == taskId) task.complete(result) else it })
        return PlanUpdate(updated, "Task $taskId marked complete. ${updated.progressMessage()}")
    }

    fun reset(): AgentPlan = copy(tasks = tasks.map { it.reset() })

    fun summary(): String = buildString {
        append("Plan: ").append(goal).append('\n')
        tasks.forEach { task ->
            append(if (task.isFinished) "✓" else "○")
                .append(" ").append(task.id).append(": ").append(task.instruction)
            if (task.dependencies.isNotEmpty()) append(" (after ").append(task.dependencies.joinToString(", ")).append(")")
            append('\n')
        }
        append(progressMessage())
    }.trim()

    fun progressMessage(): String = "${tasks.count { it.isFinished }}/${tasks.size} tasks complete" +
        (currentTask?.let { ". Next: ${it.id}" } ?: ". Plan complete.")
}

data class PlanUpdate(val plan: AgentPlan, val message: String)

object AgentPlanner {
    fun create(goal: String): AgentPlan {
        val cleanGoal = goal.trim().take(280)
        require(cleanGoal.isNotBlank()) { "A plan needs a goal." }
        return AgentPlan(
            goal = cleanGoal,
            tasks = listOf(
                AgentPlanTask("research", emptyList(), "Research options and constraints for: $cleanGoal"),
                AgentPlanTask("draft", listOf("research"), "Draft a safe, reviewable approach."),
                AgentPlanTask("review", listOf("draft"), "Review the approach and request approval before any consequential action.")
            )
        )
    }
}

internal fun AgentPlan.toJson(): String = JSONObject().apply {
    put("goal", goal)
    put("tasks", JSONArray().apply {
        tasks.forEach { task -> put(JSONObject().apply {
            put("id", task.id)
            put("dependencies", JSONArray(task.dependencies))
            put("instruction", task.instruction)
            put("result", task.result)
            put("isSuccessful", task.isSuccessful)
            put("isFinished", task.isFinished)
        }) }
    })
}.toString()

internal fun agentPlanFromJson(json: String): AgentPlan? = runCatching {
    val root = JSONObject(json)
    val tasks = root.getJSONArray("tasks")
    AgentPlan(root.getString("goal"), List(tasks.length()) { index ->
        val item = tasks.getJSONObject(index)
        val dependencies = item.getJSONArray("dependencies")
        AgentPlanTask(
            id = item.getString("id"),
            dependencies = List(dependencies.length()) { dependencies.getString(it) },
            instruction = item.getString("instruction"),
            result = item.optString("result"),
            isSuccessful = item.optBoolean("isSuccessful"),
            isFinished = item.optBoolean("isFinished")
        )
    })
}.getOrNull()
