package com.ticketflow.event_service.infrastructure.batch;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ExpireReservationsScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpireReservationsScheduler.class);
    private static final long EXPIRATION_MINUTES = 5;

    private final JobLauncher jobLauncher;
    private final Job expireReservationsJob;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void runExpirationJob() {
        try {
            Instant cutoff = Instant.now().minusSeconds(EXPIRATION_MINUTES * 60);
            var params = new JobParametersBuilder()
                    .addLong("cutoff", cutoff.toEpochMilli())
                    .addLong("runAt", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(expireReservationsJob, params);
        } catch (Exception e) {
            log.error("Échec du job d'expiration des réservations", e);
        }
    }
}