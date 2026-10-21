package com.classpilot.offline.data.database.dao

import androidx.room.*
import com.classpilot.offline.data.database.entities.TimetableEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Query("SELECT * FROM timetable_entries WHERE isActive = 1 ORDER BY dayOfWeek ASC, startMinute ASC")
    fun getAllEntries(): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND isActive = 1 ORDER BY startMinute ASC")
    fun getEntriesForDay(dayOfWeek: Int): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek AND isActive = 1 ORDER BY startMinute ASC")
    suspend fun getEntriesForDaySync(dayOfWeek: Int): List<TimetableEntryEntity>

    @Query("SELECT * FROM timetable_entries WHERE subjectId = :subjectId AND isActive = 1 ORDER BY dayOfWeek, startMinute")
    fun getEntriesForSubject(subjectId: Long): Flow<List<TimetableEntryEntity>>

    @Query("SELECT * FROM timetable_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): TimetableEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TimetableEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TimetableEntryEntity>)

    @Update
    suspend fun updateEntry(entry: TimetableEntryEntity)

    @Query("UPDATE timetable_entries SET isActive = 0 WHERE id = :id")
    suspend fun deactivateEntry(id: Long)

    @Delete
    suspend fun deleteEntry(entry: TimetableEntryEntity)

    @Query("DELETE FROM timetable_entries WHERE subjectId = :subjectId")
    suspend fun deleteEntriesForSubject(subjectId: Long)

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteAllEntries()

    /** Check for overlapping entries on same day (for validation) */
    @Query("""
        SELECT * FROM timetable_entries 
        WHERE dayOfWeek = :day 
        AND isActive = 1
        AND id != :excludeId
        AND startMinute < :endMinute 
        AND endMinute > :startMinute
    """)
    suspend fun getOverlappingEntries(
        day: Int,
        startMinute: Int,
        endMinute: Int,
        excludeId: Long = -1L
    ): List<TimetableEntryEntity>
}
