package com.learnlock.di

import android.content.Context
import com.learnlock.data.AppDatabase
import com.learnlock.data.dao.ConceptDao
import com.learnlock.data.dao.HistoryDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    fun provideConceptDao(database: AppDatabase): ConceptDao = database.conceptDao()

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao = database.historyDao()
}
