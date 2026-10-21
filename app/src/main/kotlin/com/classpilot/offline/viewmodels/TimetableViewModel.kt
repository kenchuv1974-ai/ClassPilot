package com.classpilot.offline.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classpilot.offline.data.database.entities.TimetableEntryEntity
import com.classpilot.offline.data.repository.SubjectRepository
import com.classpilot.offline.data.repository.TimetableRepository
import com.classpilot.offline.domain.model.*
import com.classpilot.offline.notifications.NotificationScheduler
import com.classpilot.offline.data.preferences.PreferencesManager
import com.classpilot.offline.scheduling.ReminderRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class TimetableUiState(
    val isLoading: Boolean = true,
    val todayPeriods: List<ClassPeriod> = emptyList(),
    val tomorrowPeriods: List<ClassPeriod> = emptyList(),
    val weeklyPeriods: Map<DayOfWeek, List<ClassPeriod>> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    val tomorrow: LocalDate = LocalDate.now().plusDays(1),
    val overlapWarning: String? = null
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository,
    private val scheduler: NotificationScheduler,
    private val prefsManager: PreferencesManager,
    private val rescheduler: ReminderRescheduler
) : ViewModel() {

    private val today = LocalDate.now()
    private val tomorrow = today.plusDays(1)

    val uiState: StateFlow<TimetableUiState> = combine(
        timetableRepository.getPeriodsForDay(today.dayOfWeek),
        timetableRepository.getPeriodsForDay(tomorrow.dayOfWeek)
    ) { todayPeriods, tomorrowPeriods ->
        TimetableUiState(
            isLoading = false,
            todayPeriods = todayPeriods,
            tomorrowPeriods = tomorrowPeriods,
            today = today,
            tomorrow = tomorrow
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TimetableUiState()
    )

    fun getPeriodsForDay(day: DayOfWeek): Flow<List<ClassPeriod>> =
        timetableRepository.getPeriodsForDay(day)

    suspend fun addOrUpdateEntry(
        entry: TimetableEntryEntity,
        allowOverlap: Boolean = false
    ): Result<Unit> {
        return try {
            if (!allowOverlap) {
                val overlapping = timetableRepository.checkOverlap(
                    DayOfWeek.of(entry.dayOfWeek),
                    entry.startMinute,
                    entry.endMinute,
                    if (entry.id > 0) entry.id else -1L
                )
                if (overlapping.isNotEmpty()) {
                    return Result.failure(Exception("Time overlaps with an existing class"))
                }
            }
            if (entry.id > 0L) {
                timetableRepository.updateEntry(entry)
                scheduler.cancelRemindersForEntry(entry.id, entry.reminderMinutes)
            } else {
                timetableRepository.addEntry(entry)
            }
            rescheduler.rescheduleAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteEntry(entry: TimetableEntryEntity) {
        viewModelScope.launch {
            scheduler.cancelRemindersForEntry(entry.id, entry.reminderMinutes)
            timetableRepository.deleteEntry(entry.id)
        }
    }

    suspend fun getEntryById(id: Long): TimetableEntryEntity? =
        timetableRepository.getEntryById(id)
}
