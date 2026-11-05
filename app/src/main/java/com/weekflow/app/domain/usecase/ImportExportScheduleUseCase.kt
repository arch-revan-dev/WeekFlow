package com.weekflow.app.domain.usecase

import com.weekflow.app.data.serialization.JsonImportExportManager
import com.weekflow.app.data.serialization.ValidationResult
import com.weekflow.app.domain.model.CleanSchedulePayload
import com.weekflow.app.domain.model.SchedulePreset
import com.weekflow.app.domain.repository.ScheduleRepository

enum class ImportMode {
    REPLACE,
    MERGE
}

class ImportExportScheduleUseCase(
    private val scheduleRepository: ScheduleRepository
) {

    fun validateInputJson(rawJson: String): ValidationResult {
        return JsonImportExportManager.validateAndSanitize(rawJson)
    }

    suspend fun executeImport(
        payload: CleanSchedulePayload,
        mode: ImportMode,
        presetId: String = SchedulePreset.DEFAULT_ID
    ) {
        when (mode) {
            ImportMode.REPLACE -> scheduleRepository.replaceAllSchedule(payload, presetId)
            ImportMode.MERGE -> scheduleRepository.mergeSchedule(payload, presetId)
        }
    }

    suspend fun exportCleanJson(presetId: String = SchedulePreset.DEFAULT_ID): String {
        val payload = scheduleRepository.exportCleanSchedule(presetId)
        return JsonImportExportManager.exportToJson(payload)
    }
}
