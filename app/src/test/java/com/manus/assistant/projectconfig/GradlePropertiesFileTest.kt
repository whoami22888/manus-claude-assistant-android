package com.manus.assistant.projectconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Properties

/**
 * Tests for the project-wide `gradle.properties` file added in this change.
 */
class GradlePropertiesFileTest {

    private lateinit var propertiesFile: File
    private lateinit var properties: Properties

    @Before
    fun setUp() {
        propertiesFile = File(findRepoRoot(), "gradle.properties")
        assertTrue("gradle.properties should exist at ${propertiesFile.path}", propertiesFile.isFile)
        properties = Properties().apply { propertiesFile.inputStream().use { load(it) } }
    }

    @Test
    fun `jvm args configure heap size and file encoding`() {
        assertEquals("-Xmx2048m -Dfile.encoding=UTF-8", properties.getProperty("org.gradle.jvmargs"))
    }

    @Test
    fun `AndroidX support is enabled`() {
        assertEquals("true", properties.getProperty("android.useAndroidX"))
    }

    @Test
    fun `Jetifier is disabled`() {
        assertEquals("false", properties.getProperty("android.enableJetifier"))
    }

    @Test
    fun `file declares exactly the expected set of properties`() {
        val expectedKeys = setOf(
            "org.gradle.jvmargs",
            "android.useAndroidX",
            "android.enableJetifier"
        )
        assertEquals(expectedKeys, properties.stringPropertyNames())
    }

    @Test
    fun `raw source has no duplicate property keys`() {
        val keys = propertiesFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .map { it.substringBefore("=").trim() }

        assertEquals("Duplicate keys should not appear in gradle.properties", keys.size, keys.toSet().size)
    }
}
