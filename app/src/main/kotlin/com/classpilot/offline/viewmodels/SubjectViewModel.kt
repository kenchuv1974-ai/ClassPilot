package com.classpilot.offline.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classpilot.offline.data.repository.AttendanceRepository
import com.classpilot.offline.data.repository.SubjectRepository
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.domain.model.SubjectType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    val subjects: StateFlow<List<Subject>> = subjectRepository.allSubjectsWithStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSubject(
        name: String,
        code: String = "",
        faculty: String = "",
        classroom: String = "",
        type: SubjectType = SubjectType.THEORY,
        threshold: Float = 75f,
        colorHex: String = "#7C4DFF",
        notes: String = "",
        initialAttended: Int = 0,
        initialConducted: Int = 0
    ) {
        viewModelScope.launch {
            val newId = subjectRepository.addSubject(
                Subject(
                    name = name,
                    code = code,
                    facultyName = faculty,
                    classroom = classroom,
                    type = type,
                    attendanceThreshold = threshold,
                    colorHex = colorHex,
                    notes = notes
                )
            )
            if (initialConducted > 0) {
                attendanceRepository.setInitialAttendance(newId, initialAttended, initialConducted)
            }
        }
    }

    fun updateSubject(
        subject: Subject,
        attended: Int? = null,
        conducted: Int? = null
    ) {
        viewModelScope.launch {
            subjectRepository.updateSubject(subject)
            if (attended != null && conducted != null) {
                attendanceRepository.setInitialAttendance(subject.id, attended, conducted)
            }
        }
    }

    fun quickMarkSubject(subjectId: Long, isPresent: Boolean) {
        viewModelScope.launch {
            attendanceRepository.quickMarkSubject(subjectId, isPresent)
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(id)
        }
    }

    suspend fun getSubjectById(id: Long): Subject? = subjectRepository.getSubjectById(id)
}

