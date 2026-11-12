package com.weekflow.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weekflow.app.data.local.db.dao.ActivityDao
import com.weekflow.app.data.local.db.dao.EventDao
import com.weekflow.app.data.local.db.dao.PresetDao
import com.weekflow.app.data.local.db.dao.SlotDao
import com.weekflow.app.data.local.db.dao.TaskDao
import com.weekflow.app.data.local.db.dao.VacationDao
import com.weekflow.app.data.local.db.entity.ActivityEntity
import com.weekflow.app.data.local.db.entity.EventEntity
import com.weekflow.app.data.local.db.entity.PresetEntity
import com.weekflow.app.data.local.db.entity.SlotEntity
import com.weekflow.app.data.local.db.entity.TaskEntity
import com.weekflow.app.data.local.db.entity.VacationEntity

@Database(
    entities = [
        PresetEntity::class,
        ActivityEntity::class,
        SlotEntity::class,
        EventEntity::class,
        TaskEntity::class,
        VacationEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class WeekFlowDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun activityDao(): ActivityDao
    abstract fun slotDao(): SlotDao
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
    abstract fun vacationDao(): VacationDao

    companion object {
        const val DATABASE_NAME = "weekflow_database.db"

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `slots` ADD COLUMN `grade` TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks_new` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `dueDateIso` TEXT,
                        `linkedSlotId` TEXT,
                        `linkedActivityId` TEXT,
                        `isCompleted` INTEGER NOT NULL,
                        `note` TEXT,
                        `priority` TEXT NOT NULL,
                        `subtasksJson` TEXT NOT NULL DEFAULT '[]',
                        `presetId` TEXT NOT NULL DEFAULT 'default',
                        `attachmentsJson` TEXT NOT NULL DEFAULT '[]',
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`linkedActivityId`) REFERENCES `activities`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`linkedSlotId`) REFERENCES `slots`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `tasks_new` (`id`, `title`, `dueDateIso`, `linkedSlotId`, `linkedActivityId`, `isCompleted`, `note`, `priority`, `subtasksJson`, `presetId`, `attachmentsJson`)
                    SELECT `id`, `title`, `dueDateIso`, `linkedSlotId`, `linkedActivityId`, `isCompleted`, `note`, `priority`, `subtasksJson`, `presetId`, `attachmentsJson`
                    FROM `tasks`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueDateIso` ON `tasks` (`dueDateIso`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_linkedActivityId` ON `tasks` (`linkedActivityId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_linkedSlotId` ON `tasks` (`linkedSlotId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_presetId` ON `tasks` (`presetId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `grade` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `attachmentsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `slots` ADD COLUMN `sessionItemsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `presets` ADD COLUMN `appProfile` TEXT NOT NULL DEFAULT 'STUDY'")
                db.execSQL("UPDATE `presets` SET `appProfile` = 'HEALTH' WHERE `iconEmoji` = '🏋️' OR `name` LIKE '%Упр%' OR `name` LIKE '%Спорт%'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `presets` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `iconEmoji` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `autoStartDateIso` TEXT,
                        `autoEndDateIso` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `presets` (`id`, `name`, `iconEmoji`, `colorHex`, `autoStartDateIso`, `autoEndDateIso`, `createdAt`)
                    VALUES ('default', 'Основной', '🎓', '#007AFF', NULL, NULL, 0)
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE `activities` ADD COLUMN `presetId` TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activities_presetId` ON `activities` (`presetId`)")

                db.execSQL("ALTER TABLE `slots` ADD COLUMN `presetId` TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_slots_presetId` ON `slots` (`presetId`)")

                db.execSQL("ALTER TABLE `events` ADD COLUMN `presetId` TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_presetId` ON `events` (`presetId`)")

                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `subtasksJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `presetId` TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_presetId` ON `tasks` (`presetId`)")
            }
        }

        val DEFAULT_PRESET_CALLBACK = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `presets` (`id`, `name`, `iconEmoji`, `colorHex`, `autoStartDateIso`, `autoEndDateIso`, `createdAt`)
                        VALUES ('default', 'Основной', '🎓', '#007AFF', NULL, NULL, 0)
                        """.trimIndent()
                    )
                } catch (_: Exception) {}
            }
        }
    }
}
