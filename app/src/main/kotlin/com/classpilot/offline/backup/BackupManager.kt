package com.classpilot.offline.backup

import android.content.Context
import android.net.Uri
import com.classpilot.offline.data.database.entities.AttendanceRecordEntity
import com.classpilot.offline.data.database.entities.CalendarEventEntity
import com.classpilot.offline.data.database.entities.SubjectEntity
import com.classpilot.offline.data.database.entities.TimetableEntryEntity
import com.classpilot.offline.data.repository.AttendanceRepository
import com.classpilot.offline.data.repository.SubjectRepository
import com.classpilot.offline.data.repository.TimetableRepository
import com.classpilot.offline.data.database.dao.CalendarEventDao
import com.classpilot.offline.data.database.dao.SubjectDao
import com.classpilot.offline.data.database.dao.TimetableDao
import com.classpilot.offline.data.database.dao.AttendanceDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupData(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val subjects: List<SubjectEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val calendarEvents: List<CalendarEventEntity> = emptyList()
)

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Failure(val error: String) : BackupResult()
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subjectDao: SubjectDao,
    private val timetableDao: TimetableDao,
    private val attendanceDao: AttendanceDao,
    private val calendarEventDao: CalendarEventDao
) {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createBackup(uri: Uri): BackupResult {
        return try {
            val backup = BackupData(
                subjects = subjectDao.getAllSubjects().first(),
                timetableEntries = timetableDao.getAllEntries().first(),
                attendanceRecords = attendanceDao.getAllRecordsSync(),
                calendarEvents = calendarEventDao.getAllEventsSync()
            )
            val jsonStr = json.encodeToString(backup)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonStr.toByteArray(Charsets.UTF_8))
            } ?: return BackupResult.Failure("Could not open output stream")
            BackupResult.Success("Backup created with ${backup.subjects.size} subjects, ${backup.attendanceRecords.size} attendance records.")
        } catch (e: Exception) {
            BackupResult.Failure("Backup failed: ${e.localizedMessage}")
        }
    }

    suspend fun restoreBackup(uri: Uri): BackupResult {
        return try {
            val jsonStr = context.contentResolver.openInputStream(uri)?.use { ins ->
                BufferedReader(InputStreamReader(ins, Charsets.UTF_8)).readText()
            } ?: return BackupResult.Failure("Could not open input stream")

            val backup = runCatching { json.decodeFromString<BackupData>(jsonStr) }
                .getOrElse { return BackupResult.Failure("Invalid backup file: ${it.localizedMessage}") }

            if (backup.version > 1) {
                return BackupResult.Failure("Backup version ${backup.version} is not supported. Please update the app.")
            }

            // Clear existing data
            subjectDao.deleteAllSubjects()
            timetableDao.deleteAllEntries()
            attendanceDao.deleteAllRecords()
            calendarEventDao.deleteAllEvents()

            // Insert restored data with id=0 so Room auto-generates new IDs
            // Actually we keep original IDs for FK integrity
            backup.subjects.forEach { subjectDao.insertSubject(it) }
            backup.timetableEntries.forEach { timetableDao.insertEntry(it) }
            backup.attendanceRecords.forEach { attendanceDao.insertRecord(it) }
            backup.calendarEvents.forEach { calendarEventDao.insertEvent(it) }

            BackupResult.Success("Restored ${backup.subjects.size} subjects and ${backup.attendanceRecords.size} attendance records.")
        } catch (e: Exception) {
            BackupResult.Failure("Restore failed: ${e.localizedMessage}")
        }
    }

    suspend fun clearAllData(): BackupResult {
        return try {
            subjectDao.deleteAllSubjects()
            timetableDao.deleteAllEntries()
            attendanceDao.deleteAllRecords()
            calendarEventDao.deleteAllEvents()
            BackupResult.Success("All data cleared successfully.")
        } catch (e: Exception) {
            BackupResult.Failure("Clear failed: ${e.localizedMessage}")
        }
    }
}
