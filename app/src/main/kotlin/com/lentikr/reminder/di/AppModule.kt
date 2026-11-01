package com.lentikr.reminder.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room3.Room
import com.lentikr.reminder.data.local.ReminderDao
import com.lentikr.reminder.data.local.ReminderDatabase
import com.lentikr.reminder.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): ReminderDatabase =
    Room.databaseBuilder<ReminderDatabase>(context, ReminderDatabase.NAME)
      .setQueryCoroutineContext(Dispatchers.IO)
      .build()

  @Provides fun provideReminderDao(database: ReminderDatabase): ReminderDao = database.reminderDao()

  @Provides
  @Singleton
  fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
      corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
      scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
      produceFile = { context.preferencesDataStoreFile(SettingsRepository.FILE_NAME) },
    )

  /** Injected so tests can pin "today". */
  @Provides fun provideClock(): Clock = Clock.systemDefaultZone()
}
