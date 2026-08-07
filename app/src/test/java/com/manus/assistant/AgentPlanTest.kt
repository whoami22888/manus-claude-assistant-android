package com.manus.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPlanTest {
    @Test fun `new plan exposes research as the current task`() {
        val plan = AgentPlanner.create("Organize a trip")
        assertEquals("research", plan.currentTask?.id)
        assertEquals(3, plan.tasks.size)
    }

    @Test fun `dependent task cannot finish before prerequisite`() {
        val update = AgentPlanner.create("Organize a trip").complete("draft")
        assertFalse(update.plan.tasks.first { it.id == "draft" }.isFinished)
        assertTrue(update.message.contains("research"))
    }

    @Test fun `completion advances only after prerequisite finishes`() {
        val researchDone = AgentPlanner.create("Organize a trip").complete("research").plan
        val draftDone = researchDone.complete("draft").plan
        assertTrue(draftDone.tasks.first { it.id == "draft" }.isFinished)
        assertEquals("review", draftDone.currentTask?.id)
    }

    @Test fun `reset returns completed tasks to reviewable state`() {
        val completed = AgentPlanner.create("Organize a trip").complete("research").plan
        val reset = completed.reset()
        assertFalse(reset.tasks.first { it.id == "research" }.isFinished)
        assertEquals("research", reset.currentTask?.id)
    }
}
