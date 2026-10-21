package com.classpilot.offline.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classpilot.offline.data.preferences.AppPreferences
import com.classpilot.offline.data.preferences.PreferencesManager
import com.classpilot.offline.data.repository.AttendanceRepository
import com.classpilot.offline.data.repository.SubjectRepository
import com.classpilot.offline.data.repository.TimetableRepository
import com.classpilot.offline.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val studentName: String = "",
    val today: LocalDate = LocalDate.now(),
    val currentTime: LocalTime = LocalTime.now(),
    val todayPeriods: List<ClassPeriod> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val nextClass: ClassPeriod? = null,
    val minutesToNextClass: Long = -1L,
    val lowAttendanceSubjects: List<Subject> = emptyList(),
    val greeting: String = "Good morning"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository,
    private val prefsManager: PreferencesManager
) : ViewModel() {

    private val _now = MutableStateFlow(LocalDateTime.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        timetableRepository.getPeriodsForDay(LocalDate.now().dayOfWeek),
        subjectRepository.allSubjectsWithStats,
        prefsManager.appPreferences,
        _now
    ) { periods, subjects, prefs, now ->
        val currentMin = now.hour * 60 + now.minute
        val upcomingPeriods = periods.filter { it.endMinuteOfDay > currentMin }
        val nextClass = upcomingPeriods.firstOrNull()
        val minsToNext = nextClass?.let {
            (it.startMinuteOfDay - currentMin).toLong().coerceAtLeast(0L)
        } ?: -1L

        DashboardUiState(
            isLoading = false,
            studentName = prefs.studentName,
            today = now.toLocalDate(),
            currentTime = now.toLocalTime(),
            todayPeriods = periods,
            subjects = subjects,
            nextClass = nextClass,
            minutesToNextClass = minsToNext,
            lowAttendanceSubjects = subjects.filter { it.isBelowThreshold },
            greeting = greetingFor(now.hour)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardUiState()
    )

    fun refreshTime() {
        _now.value = LocalDateTime.now()
    }

    private fun greetingFor(hour: Int) = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
