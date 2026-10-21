package com.classpilot.offline.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classpilot.offline.data.repository.AttendanceRepository
import com.classpilot.offline.data.repository.SubjectRepository
import com.classpilot.offline.data.database.entities.AttendanceRecordEntity
import com.classpilot.offline.domain.model.AttendanceStatus
import com.classpilot.offline.domain.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = subjectRepository.allSubjectsWithStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getAttendanceHistory(subjectId: Long): Flow<List<AttendanceRecordEntity>> =
        attendanceRepository.getAttendanceForSubject(subjectId)

    fun markAttendance(
        subjectId: Long,
        date: LocalDate,
        status: AttendanceStatus,
        startMinute: Int = 0,
        endMinute: Int = 0,
        timetableEntryId: Long? = null
    ) {
        viewModelScope.launch {
            attendanceRepository.markAttendance(
                subjectId = subjectId,
                date = date,
                status = status,
                startMinute = startMinute,
                endMinute = endMinute,
                timetableEntryId = timetableEntryId
            )
        }
    }

    fun updateRecord(record: AttendanceRecordEntity, newStatus: String) {
        viewModelScope.launch {
            attendanceRepository.updateRecord(record.copy(status = newStatus))
        }
    }

    fun deleteRecord(record: AttendanceRecordEntity) {
        viewModelScope.launch {
            attendanceRepository.deleteRecord(record)
        }
    }

    suspend fun getSubjectById(id: Long): Subject? = subjectRepository.getSubjectById(id)
}

// ── Attendance Calculator logic ───────────────────────────────────────────────

data class AttendanceCalculation(
    val subjectName: String,
    val conducted: Int,
    val present: Int,
    val threshold: Float,
    val currentPercentage: Float,
    val canMiss: Int,
    val needToAttend: Int,
    val percentageIfMissNext: Float,
    val percentageIfAttendNext5: Float,
    val isAboveThreshold: Boolean
)

fun calculateAttendance(subject: Subject): AttendanceCalculation {
    val conducted = subject.conductedClasses
    val present = subject.presentClasses
    val threshold = subject.attendanceThreshold

    val current = if (conducted == 0) 0f else (present * 100f / conducted)

    val canMiss = if (conducted == 0) 0 else {
        val maxTotal = (present * 100.0 / threshold).toInt()
        maxOf(0, maxTotal - conducted)
    }

    val needToAttend = if (current >= threshold) 0 else {
        val t = threshold / 100.0
        val denom = 1.0 - t
        if (denom <= 0) Int.MAX_VALUE
        else Math.ceil((t * conducted - present) / denom).toInt().coerceAtLeast(0)
    }

    val percentageIfMissNext = if (conducted + 1 == 0) 0f
        else (present * 100f / (conducted + 1))

    val percentageIfAttendNext5 = if (conducted + 5 == 0) 0f
        else ((present + 5) * 100f / (conducted + 5))

    return AttendanceCalculation(
        subjectName = subject.name,
        conducted = conducted,
        present = present,
        threshold = threshold,
        currentPercentage = current,
        canMiss = canMiss,
        needToAttend = needToAttend,
        percentageIfMissNext = percentageIfMissNext,
        percentageIfAttendNext5 = percentageIfAttendNext5,
        isAboveThreshold = current >= threshold
    )
}
