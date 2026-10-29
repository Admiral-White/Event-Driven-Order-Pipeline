package goldmansachs.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for the User Service.
 * 
 * Configures:
 * - Kafka producer for publishing user events
 * - Serialization for keys and values
 * - Producer properties (acknowledgments, retries, batching, etc.)
 * 
 * @author Michael Inyang
 * @version 1.0
 */
@Configuration
@Slf4j
public class KafkaConfig {
    
    /**
     * Kafka bootstrap servers.
     * Default value: localhost:9092
     * Override with spring.kafka.bootstrap-servers in application.yml
     */
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    
    /**
     * Create ProducerFactory for Kafka producer.
     * 
     * Configuration:
     * - StringSerializer for keys
     * - StringSerializer for values
     * - Acks set to "all" for at-least-once delivery
     * - Retries enabled for failed sends
     * - Compression enabled for payload size reduction
     * 
     * @return the configured ProducerFactory
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Kafka bootstrap servers
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        
        // Serializers for keys and values
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        /**
         * Acknowledgment level:
         * - "all": Leader waits for acknowledgment from all in-sync replicas
         * - Provides at-least-once delivery semantics
         * - Suitable for transactional outbox pattern
         */
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        
        /**
         * Retry configuration:
         * - Retries on failure
         * - Backoff time between retries
         */
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
        
        /**
         * Batching configuration:
         * - Batch size for grouping messages
         * - Linger time waiting for more messages to batch
         */
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        
        /**
         * Compression:
         * - Reduces payload size
         * - Supported algorithms: snappy, lz4, gzip, zstd
         */
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        /**
         * Request timeout:
         * - Maximum time to wait for a request to complete
         */
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        
        /**
         * Idempotence:
         * - Ensures exactly-once semantics for individual messages
         * - Requires retries > 0 and acks = "all"
         */
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        /**
         * Max in flight requests:
         * - Number of requests to send without waiting for responses
         * - Must be <= 5 when idempotence is enabled
         */
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        log.info("Kafka ProducerFactory configured");
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * Create KafkaTemplate for sending messages.
     * 
     * @return the configured KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        log.info("Kafka KafkaTemplate bean created");
        return new KafkaTemplate<>(producerFactory());
    }
}
