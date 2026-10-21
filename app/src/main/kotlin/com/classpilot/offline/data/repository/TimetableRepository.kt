package com.classpilot.offline.data.repository

import com.classpilot.offline.data.database.dao.TimetableDao
import com.classpilot.offline.data.database.entities.TimetableEntryEntity
import com.classpilot.offline.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepository @Inject constructor(
    private val timetableDao: TimetableDao,
    private val subjectRepository: SubjectRepository
) {

    fun getAllEntries(): Flow<List<TimetableEntryEntity>> = timetableDao.getAllEntries()

    fun getEntriesForDay(dayOfWeek: DayOfWeek): Flow<List<TimetableEntryEntity>> =
        timetableDao.getEntriesForDay(dayOfWeek.value)

    /** Returns the full ClassPeriod list for a given day of week with subject info */
    fun getPeriodsForDay(dayOfWeek: DayOfWeek): Flow<List<ClassPeriod>> =
        combine(
            timetableDao.getEntriesForDay(dayOfWeek.value),
            subjectRepository.allSubjectsWithStats
        ) { entries, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            entries.mapNotNull { entry ->
                subjectMap[entry.subjectId]?.let { subject ->
                    entry.toClassPeriod(subject)
                }
            }.sortedBy { it.startMinuteOfDay }
        }

    suspend fun getPeriodsForDaySync(dayOfWeek: DayOfWeek): List<TimetableEntryEntity> =
        timetableDao.getEntriesForDaySync(dayOfWeek.value)

    suspend fun addEntry(entry: TimetableEntryEntity): Long = timetableDao.insertEntry(entry)

    suspend fun updateEntry(entry: TimetableEntryEntity) = timetableDao.updateEntry(entry)

    suspend fun deleteEntry(id: Long) = timetableDao.deactivateEntry(id)

    suspend fun getEntryById(id: Long): TimetableEntryEntity? = timetableDao.getEntryById(id)

    suspend fun checkOverlap(
        dayOfWeek: DayOfWeek,
        startMinute: Int,
        endMinute: Int,
        excludeId: Long = -1L
    ): List<TimetableEntryEntity> =
        timetableDao.getOverlappingEntries(dayOfWeek.value, startMinute, endMinute, excludeId)

    suspend fun deleteAllEntries() = timetableDao.deleteAllEntries()

    suspend fun insertEntries(entries: List<TimetableEntryEntity>) =
        timetableDao.insertEntries(entries)
}

// ── Mapping helpers ──────────────────────────────────────────────────────────

fun TimetableEntryEntity.toClassPeriod(subject: Subject) = ClassPeriod(
    id = id,
    subject = subject,
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    startTime = LocalTime.of(startMinute / 60, startMinute % 60),
    endTime = LocalTime.of(endMinute / 60, endMinute % 60),
    room = room.ifBlank { subject.classroom },
    faculty = faculty.ifBlank { subject.facultyName },
    classType = ClassType.values().firstOrNull { it.name == classType } ?: ClassType.THEORY,
    isRepeatingWeekly = isRepeatingWeekly,
    notificationEnabled = notificationEnabled,
    reminderMinutes = reminderMinutes.split(",").mapNotNull { it.trim().toIntOrNull() }
)

fun ClassPeriod.toEntity() = TimetableEntryEntity(
    id = id,
    subjectId = subject.id,
    dayOfWeek = dayOfWeek.value,
    startMinute = startMinuteOfDay,
    endMinute = endMinuteOfDay,
    room = room,
    faculty = faculty,
    classType = classType.name,
    isRepeatingWeekly = isRepeatingWeekly,
    notificationEnabled = notificationEnabled,
    reminderMinutes = reminderMinutes.joinToString(",")
)
