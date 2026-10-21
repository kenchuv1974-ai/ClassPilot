package com.classpilot.offline.data.repository

import com.classpilot.offline.data.database.dao.AttendanceDao
import com.classpilot.offline.data.database.entities.AttendanceRecordEntity
import com.classpilot.offline.domain.model.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao
) {

    fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getAttendanceForSubject(subjectId)

    fun getAttendanceForDate(date: LocalDate): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getAttendanceForDate(date.toEpochDay())

    suspend fun markAttendance(
        subjectId: Long,
        date: LocalDate,
        status: AttendanceStatus,
        startMinute: Int = 0,
        endMinute: Int = 0,
        timetableEntryId: Long? = null,
        notes: String = "",
        isExtraClass: Boolean = false
    ): Long {
        val record = AttendanceRecordEntity(
            subjectId = subjectId,
            dateEpochDay = date.toEpochDay(),
            status = status.name,
            timetableEntryId = timetableEntryId,
            startMinute = startMinute,
            endMinute = endMinute,
            notes = notes,
            isExtraClass = isExtraClass,
            isCancelled = status == AttendanceStatus.CANCELLED
        )
        return attendanceDao.insertRecord(record)
    }

    suspend fun updateRecord(record: AttendanceRecordEntity) =
        attendanceDao.updateRecord(record)

    suspend fun deleteRecord(record: AttendanceRecordEntity) =
        attendanceDao.deleteRecord(record)

    suspend fun getForSubjectAndDate(subjectId: Long, date: LocalDate): List<AttendanceRecordEntity> =
        attendanceDao.getAttendanceForSubjectAndDate(subjectId, date.toEpochDay())

    suspend fun getAllRecordsSync(): List<AttendanceRecordEntity> =
        attendanceDao.getAllRecordsSync()

    suspend fun insertRecords(records: List<AttendanceRecordEntity>) =
        attendanceDao.insertRecords(records)

    suspend fun setInitialAttendance(subjectId: Long, attended: Int, totalConducted: Int) {
        val safeAttended = attended.coerceAtLeast(0)
        val safeConducted = maxOf(safeAttended, totalConducted)
        val absentCount = safeConducted - safeAttended
        
        // Remove existing initial records for this subject if re-setting
        attendanceDao.deleteRecordsForSubject(subjectId)
        
        val todayEpoch = LocalDate.now().toEpochDay()
        val records = mutableListOf<AttendanceRecordEntity>()
        
        repeat(safeAttended) { i ->
            records.add(
                AttendanceRecordEntity(
                    subjectId = subjectId,
                    dateEpochDay = todayEpoch - 1,
                    status = AttendanceStatus.PRESENT.name,
                    notes = "Initial / previously attended",
                    markedAt = System.currentTimeMillis() - (i * 1000L)
                )
            )
        }
        repeat(absentCount) { i ->
            records.add(
                AttendanceRecordEntity(
                    subjectId = subjectId,
                    dateEpochDay = todayEpoch - 1,
                    status = AttendanceStatus.ABSENT.name,
                    notes = "Initial / previously absent",
                    markedAt = System.currentTimeMillis() - ((safeAttended + i) * 1000L)
                )
            )
        }
        if (records.isNotEmpty()) {
            attendanceDao.insertRecords(records)
        }
    }

    suspend fun quickMarkSubject(subjectId: Long, isPresent: Boolean) {
        val status = if (isPresent) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
        markAttendance(
            subjectId = subjectId,
            date = LocalDate.now(),
            status = status,
            notes = if (isPresent) "Quick mark present" else "Quick mark absent"
        )
    }

    suspend fun deleteAllRecords() = attendanceDao.deleteAllRecords()
}

