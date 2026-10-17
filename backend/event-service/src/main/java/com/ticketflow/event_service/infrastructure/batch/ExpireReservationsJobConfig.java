package com.ticketflow.event_service.infrastructure.batch;

import com.ticketflow.event_service.application.ReservationService;
import com.ticketflow.event_service.domain.ReservationStatus;
import com.ticketflow.event_service.infrastructure.ReservationEntity;
import com.ticketflow.event_service.infrastructure.ReservationJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;

@Configuration
public class ExpireReservationsJobConfig {

    private static final int CHUNK_SIZE = 20;

    @Bean
    @StepScope
    public ListItemReader<ReservationEntity> expiredReservationsReader(
            ReservationJpaRepository repository,
            @Value("#{jobParameters['cutoff']}") Long cutoffEpochMillis) {
        Instant cutoff = Instant.ofEpochMilli(cutoffEpochMillis);
        var expired = repository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, cutoff);
        return new ListItemReader<>(expired);
    }

    // Pas de Processor : la règle métier (expirer ET rendre les places) est dans ReservationService, déjà testée.
    // Le batch se contente de lui confier chaque réservation trouvée. Un paquet = une transaction.
    @Bean
    public ItemWriter<ReservationEntity> expiredReservationsWriter(ReservationService reservationService) {
        return chunk -> chunk.forEach(entity -> reservationService.expireReservation(entity.getId()));
    }

    @Bean
    public Step expireReservationsStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       ListItemReader<ReservationEntity> expiredReservationsReader,
                                       ItemWriter<ReservationEntity> expiredReservationsWriter) {
        return new StepBuilder("expireReservationsStep", jobRepository)
                .<ReservationEntity, ReservationEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredReservationsReader)
                .writer(expiredReservationsWriter)
                .build();
    }

    @Bean
    public Job expireReservationsJob(JobRepository jobRepository, Step expireReservationsStep) {
        return new JobBuilder("expireReservationsJob", jobRepository)
                .start(expireReservationsStep)
                .build();
    }
}