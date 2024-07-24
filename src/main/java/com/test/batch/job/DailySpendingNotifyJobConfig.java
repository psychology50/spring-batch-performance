package com.test.batch.job;

import com.test.batch.step.SendSpendingNotifyStepConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class DailySpendingNotifyJobConfig {
    private final JobRepository jobRepository;
    private final SendSpendingNotifyStepConfig sendSpendingNotifyStepConfig;

    @Bean
    public Job dailyNotificationJob(PlatformTransactionManager transactionManager) {
        return new JobBuilder("dailyNotificationJob", jobRepository)
                .start(sendSpendingNotifyStepConfig.sendSpendingNotifyStepAdvanced(transactionManager))
                .on("FAILED")
                .stopAndRestart(sendSpendingNotifyStepConfig.sendSpendingNotifyStepAdvanced(transactionManager))
                .on("*")
                .end()
                .end()
                .build();
    }
}
