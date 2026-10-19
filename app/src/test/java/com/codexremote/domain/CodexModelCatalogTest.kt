package com.codexremote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexModelCatalogTest {
    @Test
    fun exposesNumberedGpt56Family() {
        val ids = CodexModelCatalog.models.map { it.id }

        assertTrue(ids.contains("gpt-5.6-sol"))
        assertTrue(ids.contains("gpt-5.6-terra"))
        assertTrue(ids.contains("gpt-5.6-luna"))
    }

    @Test
    fun lunaUsesFourExplicitEffortsPlusDefault() {
        val efforts = CodexModelCatalog.effortsFor("gpt-5.6-luna")

        assertEquals(listOf("", "low", "medium", "high", "xhigh"), efforts.map { it.id })
        assertFalse(efforts.any { it.id == "max" })
    }

    @Test
    fun incompatibleEffortFallsBackToModelDefault() {
        assertEquals("", CodexModelCatalog.normalizeEffort("gpt-5.6-luna", "max"))
        assertEquals("high", CodexModelCatalog.normalizeEffort("gpt-5.6-luna", "high"))
    }

    @Test
    fun dynamicCatalogControlsRenderedEfforts() {
        val models = listOf(
            CodexModelOption(
                id = "server-model",
                label = "Server Model",
                shortLabel = "Server",
                description = "Remote",
                family = "VPS 模型",
                supportedEfforts = listOf("minimal", "high"),
            ),
        )

        assertEquals(
            listOf("", "minimal", "high"),
            CodexModelCatalog.effortsFor("server-model", models).map { it.id },
        )
        assertEquals("", CodexModelCatalog.normalizeEffort("server-model", "max", models))
    }
}
