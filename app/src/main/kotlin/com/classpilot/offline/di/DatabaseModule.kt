package com.classpilot.offline.di

import android.content.Context
import androidx.room.Room
import com.classpilot.offline.data.database.ClassPilotDatabase
import com.classpilot.offline.data.database.dao.AttendanceDao
import com.classpilot.offline.data.database.dao.CalendarEventDao
import com.classpilot.offline.data.database.dao.SubjectDao
import com.classpilot.offline.data.database.dao.TimetableDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ClassPilotDatabase =
        Room.databaseBuilder(
            context,
            ClassPilotDatabase::class.java,
            ClassPilotDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideSubjectDao(db: ClassPilotDatabase): SubjectDao = db.subjectDao()

    @Provides
    fun provideTimetableDao(db: ClassPilotDatabase): TimetableDao = db.timetableDao()

    @Provides
    fun provideAttendanceDao(db: ClassPilotDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    fun provideCalendarEventDao(db: ClassPilotDatabase): CalendarEventDao = db.calendarEventDao()
}
