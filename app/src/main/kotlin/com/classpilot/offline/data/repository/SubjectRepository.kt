package com.classpilot.offline.data.repository

import com.classpilot.offline.data.database.dao.AttendanceDao
import com.classpilot.offline.data.database.dao.SubjectDao
import com.classpilot.offline.data.database.entities.SubjectEntity
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.domain.model.SubjectType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao,
    private val attendanceDao: AttendanceDao
) {

    /** All subjects with their real-time attendance counts */
    @OptIn(ExperimentalCoroutinesApi::class)
    val allSubjectsWithStats: Flow<List<Subject>> = subjectDao.getAllSubjects()
        .flatMapLatest { entities ->
            if (entities.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val flows: List<Flow<Subject>> = entities.map { entity ->
                    combine(
                        attendanceDao.getConductedCount(entity.id),
                        attendanceDao.getPresentCount(entity.id),
                        attendanceDao.getAbsentCount(entity.id)
                    ) { conducted, present, absent ->
                        entity.toDomain(conducted, present, absent)
                    }
                }
                combine(flows) { it.toList() }
            }
        }

    suspend fun getSubjectById(id: Long): Subject? =
        subjectDao.getSubjectById(id)?.let { entity ->
            val conducted = attendanceDao.getConductedCountSync(entity.id)
            val present = attendanceDao.getPresentCountSync(entity.id)
            entity.toDomain(conducted, present, conducted - present)
        }

    suspend fun addSubject(subject: Subject): Long =
        subjectDao.insertSubject(subject.toEntity())

    suspend fun updateSubject(subject: Subject) =
        subjectDao.updateSubject(subject.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteSubject(id: Long) =
        subjectDao.softDeleteSubject(id)

    suspend fun hardDeleteSubject(subject: Subject) =
        subjectDao.hardDeleteSubject(subject.toEntity())
}

// ── Mapping helpers ──────────────────────────────────────────────────────────

fun SubjectEntity.toDomain(conducted: Int = 0, present: Int = 0, absent: Int = 0) = Subject(
    id = id,
    name = name,
    code = code,
    facultyName = facultyName,
    classroom = classroom,
    type = SubjectType.values().firstOrNull { it.name == type } ?: SubjectType.THEORY,
    attendanceThreshold = attendanceThreshold,
    colorHex = colorHex,
    notes = notes,
    separateLabTracking = separateLabTracking,
    isActive = isActive,
    conductedClasses = conducted,
    presentClasses = present,
    absentClasses = absent
)

fun Subject.toEntity() = SubjectEntity(
    id = id,
    name = name,
    code = code,
    facultyName = facultyName,
    classroom = classroom,
    type = type.name,
    attendanceThreshold = attendanceThreshold,
    colorHex = colorHex,
    notes = notes,
    separateLabTracking = separateLabTracking,
    isActive = isActive
)
