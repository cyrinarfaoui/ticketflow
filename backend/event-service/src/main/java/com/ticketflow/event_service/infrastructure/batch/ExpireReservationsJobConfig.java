package com.ticketflow.event_service.infrastructure.batch;

import com.ticketflow.event_service.domain.Reservation;
import com.ticketflow.event_service.domain.ReservationStatus;
import com.ticketflow.event_service.infrastructure.ReservationEntity;
import com.ticketflow.event_service.infrastructure.ReservationJpaRepository;
import com.ticketflow.event_service.infrastructure.ReservationMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemWriterBuilder;
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

    @Bean
    public ItemProcessor<ReservationEntity, ReservationEntity> expireReservationProcessor() {
        // Passer par le mapper garantit qu'aucun champ (createdAt, userId…) n'est perdu au passage
        return entity -> {
            Reservation reservation = ReservationMapper.toDomain(entity);
            reservation.expire();
            return ReservationMapper.toEntity(reservation);
        };
    }

    @Bean
    public RepositoryItemWriter<ReservationEntity> expiredReservationsWriter(ReservationJpaRepository repository) {
        return new RepositoryItemWriterBuilder<ReservationEntity>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step expireReservationsStep(JobRepository jobRepository,
                                       PlatformTransactionManager transactionManager,
                                       ListItemReader<ReservationEntity> expiredReservationsReader,
                                       ItemProcessor<ReservationEntity, ReservationEntity> expireReservationProcessor,
                                       RepositoryItemWriter<ReservationEntity> expiredReservationsWriter) {
        return new StepBuilder("expireReservationsStep", jobRepository)
                .<ReservationEntity, ReservationEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredReservationsReader)
                .processor(expireReservationProcessor)
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