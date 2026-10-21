package com.classpilot.offline.data.database.dao

import androidx.room.*
import com.classpilot.offline.data.database.entities.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects WHERE isActive = 1 ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun getSubjectByIdFlow(id: Long): Flow<SubjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteSubject(id: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun hardDeleteSubject(subject: SubjectEntity)

    @Query("SELECT COUNT(*) FROM subjects WHERE isActive = 1")
    suspend fun getActiveSubjectCount(): Int

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}
