package com.bluecc.triggers;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.ofbiz.base.util.Debug;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class InfoConsumer implements Runnable{
    private static final String MODULE = InfoConsumer.class.getName();
    private final KafkaConsumer<String, String> consumer;
    private final String[] topics;

    public static final String KAFKA_SERVER_URL = "localhost";
    public static final int KAFKA_SERVER_PORT = 9092;
    public static final String CLIENT_ID = "sagasConsumer";
    ThreadPoolExecutor executor;

    public boolean isStop() {
        return stop;
    }

    public void stop(boolean stop) {
        this.stop = stop;
    }

    boolean stop=false;

    public InfoConsumer(String... topics) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER_URL + ":" + KAFKA_SERVER_PORT);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CLIENT_ID);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        this.topics = topics;

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    }

    @Override
    public void run() {
        while(!stop) {
            consumer.subscribe(Arrays.asList(this.topics));
            // ConsumerRecords<Integer, String> records = consumer.poll(1000);
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                Debug.logInfo("Received message: (" + record.key() + ", "
                        + record.value() + ") at offset " + record.offset(), MODULE);
            }
        }
    }

    public void serve(){
        executor.submit(() -> {
            run();
            return null;
        });
    }
}

