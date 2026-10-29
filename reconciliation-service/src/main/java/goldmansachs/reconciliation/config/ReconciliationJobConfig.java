package goldmansachs.reconciliation.config;

import goldmansachs.reconciliation.listener.SkipListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final SkipListener skipListener;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public JdbcCursorItemReader<Map<String, Object>> reconciliationReader() {
        return new JdbcCursorItemReaderBuilder<Map<String, Object>>()
                .name("reconciliationReader")
                .dataSource(dataSource)
                .sql("SELECT s.id, s.settlement_id, s.order_id, s.status, s.settlement_amount " +
                     "FROM settlement_records s " +
                     "WHERE s.status = 'PROCESSING' " +
                     "ORDER BY s.created_at ASC")
                .rowMapper((rs, rowNum) -> mapRow(rs))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Map<String, Object>> reconciliationWriter() {
        return new JdbcBatchItemWriterBuilder<Map<String, Object>>()
                .dataSource(dataSource)
                .sql("UPDATE settlement_records SET status = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?")
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setString(1, "COMPLETED");
                    ps.setLong(2, ((Number) item.get("id")).longValue());
                })
                .build();
    }

    @Bean
    public Step reconciliationStep() {
        return new StepBuilder("reconciliationStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(CHUNK_SIZE, transactionManager)
                .reader(reconciliationReader())
                .processor(item -> {
                    log.info("Processing settlement: {}", item.get("settlement_id"));
                    // Add business logic for reconciliation
                    return item;
                })
                .writer(reconciliationWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Job reconciliationJob() {
        return new JobBuilder("reconciliationJob", jobRepository)
                .start(reconciliationStep())
                .build();
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("settlement_id", rs.getString("settlement_id"));
        row.put("order_id", rs.getString("order_id"));
        row.put("status", rs.getString("status"));
        row.put("settlement_amount", rs.getBigDecimal("settlement_amount"));
        return row;
    }
}
