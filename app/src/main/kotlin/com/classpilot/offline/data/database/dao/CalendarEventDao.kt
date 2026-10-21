package com.classpilot.offline.data.database.dao

import androidx.room.*
import com.classpilot.offline.data.database.entities.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Query("SELECT * FROM calendar_events ORDER BY dateEpochDay ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE dateEpochDay = :dateEpochDay ORDER BY startMinute ASC")
    fun getEventsForDate(dateEpochDay: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay, startMinute")
    fun getEventsInRange(from: Long, to: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: Long): CalendarEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()

    @Query("SELECT * FROM calendar_events ORDER BY dateEpochDay ASC")
    suspend fun getAllEventsSync(): List<CalendarEventEntity>
}
