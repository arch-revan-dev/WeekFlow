package com.weekflow.app.di

import androidx.room.Room
import com.weekflow.app.data.local.datastore.UserPreferencesRepository
import com.weekflow.app.data.local.db.WeekFlowDatabase
import com.weekflow.app.data.repository.ScheduleRepositoryImpl
import com.weekflow.app.domain.repository.PreferencesRepository
import com.weekflow.app.domain.repository.ScheduleRepository
import com.weekflow.app.core.notifications.NotificationHelper
import com.weekflow.app.core.notifications.NotificationScheduler
import com.weekflow.app.domain.usecase.CalculateNextLessonDateUseCase
import com.weekflow.app.domain.usecase.CalculateWeekCycleUseCase
import com.weekflow.app.domain.usecase.DetectActiveOrRecentActivityUseCase
import com.weekflow.app.domain.usecase.GetDayScheduleUseCase
import com.weekflow.app.domain.usecase.GetMonthCalendarUseCase
import com.weekflow.app.domain.usecase.ImportExportScheduleUseCase
import com.weekflow.app.presentation.calendar.CalendarViewModel
import com.weekflow.app.presentation.schedule.ScheduleViewModel
import com.weekflow.app.presentation.settings.SettingsViewModel
import com.weekflow.app.presentation.tasks.TasksViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single<WeekFlowDatabase> {
        Room.databaseBuilder(
            androidContext(),
            WeekFlowDatabase::class.java,
            WeekFlowDatabase.DATABASE_NAME
        )
            .addMigrations(
                WeekFlowDatabase.MIGRATION_2_3,
                WeekFlowDatabase.MIGRATION_3_4,
                WeekFlowDatabase.MIGRATION_4_5,
                WeekFlowDatabase.MIGRATION_5_6,
                WeekFlowDatabase.MIGRATION_6_7
            )
            .addCallback(WeekFlowDatabase.DEFAULT_PRESET_CALLBACK)
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    single { get<WeekFlowDatabase>().presetDao() }
    single { get<WeekFlowDatabase>().activityDao() }
    single { get<WeekFlowDatabase>().slotDao() }
    single { get<WeekFlowDatabase>().eventDao() }
    single { get<WeekFlowDatabase>().taskDao() }
    single { get<WeekFlowDatabase>().vacationDao() }

    // Repositories
    single<PreferencesRepository> { UserPreferencesRepository(androidContext()) }
    single<ScheduleRepository> { ScheduleRepositoryImpl(get(), androidContext()) }

    // Notifications
    single { NotificationHelper(androidContext()) }
    single { NotificationScheduler(androidContext(), get(), get(), get()) }

    // Use Cases
    single { CalculateWeekCycleUseCase() }
    single { CalculateNextLessonDateUseCase(get()) }
    single { DetectActiveOrRecentActivityUseCase(get(), get()) }
    single { GetDayScheduleUseCase(get()) }
    single { GetMonthCalendarUseCase(get()) }
    single { ImportExportScheduleUseCase(get()) }

    // ViewModels
    viewModel {
        ScheduleViewModel(
            scheduleRepository = get(),
            preferencesRepository = get(),
            getDayScheduleUseCase = get(),
            calculateWeekCycleUseCase = get(),
            notificationScheduler = get()
        )
    }

    viewModel {
        CalendarViewModel(
            scheduleRepository = get(),
            preferencesRepository = get(),
            getMonthCalendarUseCase = get(),
            getDayScheduleUseCase = get(),
            notificationScheduler = get()
        )
    }

    viewModel {
        TasksViewModel(
            scheduleRepository = get(),
            preferencesRepository = get(),
            notificationScheduler = get()
        )
    }

    viewModel {
        SettingsViewModel(
            preferencesRepository = get(),
            scheduleRepository = get(),
            importExportScheduleUseCase = get(),
            notificationHelper = get(),
            notificationScheduler = get()
        )
    }
}
