package com.weekflow.app.data

import com.weekflow.app.data.serialization.DemoScheduleProvider
import com.weekflow.app.data.serialization.JsonImportExportManager
import com.weekflow.app.data.serialization.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonImportExportManagerTest {

    @Test
    fun `extractJsonPayload correctly extracts json from markdown codeblocks and surrounding text`() {
        val markdownInput = """
            Конечно, вот ваше расписание в формате JSON:
            ```json
            {
               "activities": [],
               "slots": [],
               "events": [],
               "tasks": []
            }
            ```
            Надеюсь, это поможет!
        """.trimIndent()

        val extracted = JsonImportExportManager.extractJsonPayload(markdownInput)
        assertTrue(extracted.startsWith("{"))
        assertTrue(extracted.endsWith("}"))
    }

    @Test
    fun `validateAndSanitize succeeds on DemoScheduleProvider payload`() {
        val demoPayload = DemoScheduleProvider.getDemoPayload()
        val jsonString = JsonImportExportManager.exportToJson(demoPayload)

        val result = JsonImportExportManager.validateAndSanitize(jsonString)
        assertTrue("Expected ValidationResult.Success", result is ValidationResult.Success)

        val success = result as ValidationResult.Success
        assertEquals(demoPayload.activities.size, success.activitiesCount)
        assertEquals(demoPayload.slots.size, success.slotsCount)
        assertEquals(demoPayload.events.size, success.eventsCount)
        assertEquals(demoPayload.tasks.size, success.tasksCount)
    }

    @Test
    fun `validateAndSanitize returns error on invalid json syntax`() {
        val invalidJson = "{ this is not a json }"
        val result = JsonImportExportManager.validateAndSanitize(invalidJson)
        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `validateAndSanitize creates fallback activity if slot references unknown activity`() {
        val rawJson = """
            {
              "activities": [],
              "slots": [
                {
                  "id": "slot_1",
                  "activityTypeId": "unknown_act",
                  "dayOfWeek": 1,
                  "startTime": "09:00",
                  "endTime": "10:35"
                }
              ],
              "events": [],
              "tasks": []
            }
        """.trimIndent()

        val result = JsonImportExportManager.validateAndSanitize(rawJson)
        assertTrue(result is ValidationResult.Success)

        val success = result as ValidationResult.Success
        assertEquals(1, success.slotsCount)
        assertEquals(1, success.activitiesCount) // Created fallback
        assertTrue(success.warnings.isNotEmpty())
    }
}
