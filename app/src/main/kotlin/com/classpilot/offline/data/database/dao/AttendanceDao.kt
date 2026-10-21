package com.classpilot.offline.data.database.dao

import androidx.room.*
import com.classpilot.offline.data.database.entities.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY dateEpochDay DESC, startMinute DESC")
    fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE dateEpochDay = :dateEpochDay ORDER BY startMinute ASC")
    fun getAttendanceForDate(dateEpochDay: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE dateEpochDay = :dateEpochDay ORDER BY startMinute ASC")
    suspend fun getAttendanceForDateSync(dateEpochDay: Long): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND dateEpochDay = :dateEpochDay")
    suspend fun getAttendanceForSubjectAndDate(subjectId: Long, dateEpochDay: Long): List<AttendanceRecordEntity>

    @Query("""
        SELECT COUNT(*) FROM attendance_records 
        WHERE subjectId = :subjectId AND status = 'PRESENT'
    """)
    fun getPresentCount(subjectId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM attendance_records 
        WHERE subjectId = :subjectId AND status = 'ABSENT'
    """)
    fun getAbsentCount(subjectId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM attendance_records 
        WHERE subjectId = :subjectId AND (status = 'PRESENT' OR status = 'ABSENT' OR status = 'EXTRA')
    """)
    fun getConductedCount(subjectId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM attendance_records 
        WHERE subjectId = :subjectId AND (status = 'PRESENT' OR status = 'ABSENT' OR status = 'EXTRA')
    """)
    suspend fun getConductedCountSync(subjectId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM attendance_records 
        WHERE subjectId = :subjectId AND status = 'PRESENT'
    """)
    suspend fun getPresentCountSync(subjectId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Update
    suspend fun updateRecord(record: AttendanceRecordEntity)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteRecordsForSubject(subjectId: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()

    @Query("SELECT * FROM attendance_records ORDER BY dateEpochDay DESC")
    suspend fun getAllRecordsSync(): List<AttendanceRecordEntity>
}
