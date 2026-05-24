package com.nudgery.android.di

import com.nudgery.android.settings.AppSettings
import com.nudgery.android.viewmodel.AnswerFormViewModel
import com.nudgery.android.viewmodel.CreateNudgeViewModel
import com.nudgery.android.viewmodel.EditNudgeViewModel
import com.nudgery.android.viewmodel.NudgeDetailViewModel
import com.nudgery.android.viewmodel.NudgeListViewModel
import com.nudgery.android.viewmodel.SettingsViewModel
import com.nudgery.shared.db.DatabaseDriverFactory
import com.nudgery.shared.scheduler.NotificationScheduler
import com.nudgery.shared.scheduler.WorkManagerNotificationScheduler
import com.nudgery.shared.repository.AnswerRepository
import com.nudgery.shared.repository.NotificationFireRepository
import com.nudgery.shared.usecase.ComputeNextFireTimeUseCase
import com.nudgery.shared.usecase.ExportAnswersUseCase
import com.nudgery.shared.usecase.GetVisualizationDataUseCase
import com.nudgery.shared.usecase.RecordAnswerUseCase
import com.nudgery.shared.usecase.SetAnswerHiddenUseCase
import com.nudgery.shared.usecase.UpdateNudgeUseCase
import kotlinx.datetime.Instant
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<NotificationScheduler> { WorkManagerNotificationScheduler(androidContext()) }
    single { AppSettings(androidContext()) }

    viewModel {
        NudgeListViewModel(
            nudgeRepository = get(),
            scheduleRepository = get(),
            answerRepository = get<AnswerRepository>(),
            notificationFireRepository = get<NotificationFireRepository>(),
            computeNextFireTime = get<ComputeNextFireTimeUseCase>(),
            updateNudge = get<UpdateNudgeUseCase>()
        )
    }

    viewModel {
        CreateNudgeViewModel(createNudge = get(), nudgeRepository = get())
    }

    viewModel { (nudgeId: String) ->
        NudgeDetailViewModel(
            nudgeId = nudgeId,
            nudgeRepository = get(),
            questionRepository = get(),
            questionOptionRepository = get(),
            scheduleRepository = get(),
            answerRepository = get(),
            notificationFireRepository = get<NotificationFireRepository>(),
            computeNextFireTime = get<ComputeNextFireTimeUseCase>(),
            getVisualizationData = get<GetVisualizationDataUseCase>(),
            setAnswerHidden = get<SetAnswerHiddenUseCase>(),
            exportAnswers = get<ExportAnswersUseCase>(),
            updateNudge = get<UpdateNudgeUseCase>()
        )
    }

    viewModel { (nudgeId: String) ->
        EditNudgeViewModel(
            nudgeId = nudgeId,
            nudgeRepository = get(),
            questionRepository = get(),
            questionOptionRepository = get(),
            scheduleRepository = get(),
            updateNudge = get<UpdateNudgeUseCase>()
        )
    }

    viewModel { (nudgeId: String, scheduledAt: Instant?) ->
        AnswerFormViewModel(
            nudgeId = nudgeId,
            scheduledAt = scheduledAt,
            questionRepository = get(),
            questionOptionRepository = get(),
            recordAnswer = get<RecordAnswerUseCase>()
        )
    }

    viewModel {
        SettingsViewModel(appSettings = get())
    }
}
