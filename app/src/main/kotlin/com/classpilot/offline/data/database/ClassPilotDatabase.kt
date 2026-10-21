package com.classpilot.offline.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.classpilot.offline.data.database.dao.AttendanceDao
import com.classpilot.offline.data.database.dao.CalendarEventDao
import com.classpilot.offline.data.database.dao.SubjectDao
import com.classpilot.offline.data.database.dao.TimetableDao
import com.classpilot.offline.data.database.entities.AttendanceRecordEntity
import com.classpilot.offline.data.database.entities.CalendarEventEntity
import com.classpilot.offline.data.database.entities.SubjectEntity
import com.classpilot.offline.data.database.entities.TimetableEntryEntity

@Database(
    entities = [
        SubjectEntity::class,
        TimetableEntryEntity::class,
        AttendanceRecordEntity::class,
        CalendarEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ClassPilotDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun calendarEventDao(): CalendarEventDao

    companion object {
        const val DATABASE_NAME = "classpilot.db"
    }
}
