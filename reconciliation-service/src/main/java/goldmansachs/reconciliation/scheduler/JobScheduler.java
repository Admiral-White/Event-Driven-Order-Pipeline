package goldmansachs.reconciliation.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class JobScheduler {
    private final JobLauncher jobLauncher;
    private final Job reconciliationJob;

    @Scheduled(cron = "${reconciliation.schedule.cron:0 0 2 * * ?}")
    public void scheduleReconciliationJob() {
        log.info("Starting scheduled reconciliation job at {}", LocalDateTime.now());

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(reconciliationJob, jobParameters);

            if (jobExecution.isRunning()) {
                log.info("Reconciliation job is running");
            } else {
                log.info("Reconciliation job completed with status: {}", jobExecution.getStatus());
            }
        } catch (Exception e) {
            log.error("Error scheduling reconciliation job", e);
        }
    }

    @Scheduled(fixedDelayString = "${reconciliation.retry-delay:300000}")
    public void retryFailedReconciliations() {
        log.debug("Checking for failed reconciliations to retry");
        // Implementation for retrying failed reconciliations
    }
}
