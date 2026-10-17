package com.learnlock.data.remote.ai

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.learnlock.data.AppDatabase
import com.learnlock.data.preferences.AppPreferencesManager

class ShortAnswerGradingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        val db = AppDatabase.getDatabase(applicationContext)
        val generator = ConceptGenerator(AppPreferencesManager(applicationContext))
        for (attempt in db.historyDao().getPendingShortAnswerAttempts()) {
            val grade = generator.evaluateShortAnswer(attempt.questionText, attempt.correctAnswer, attempt.userAnswer)
                ?: return Result.retry()
            db.historyDao().finishShortAnswerGrading(attempt.id, grade)
        }
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "learnlock-short-answer-grading"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ShortAnswerGradingWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }
}
