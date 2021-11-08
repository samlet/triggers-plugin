package com.bluecc.triggers;

import com.bluecc.generic.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.ofbiz.base.util.Debug;
import org.yaml.snakeyaml.Yaml;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class InfoConsumer implements Runnable {
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

    boolean stop = false;
    Yaml yaml = new Yaml();
    // Gson gson=new Gson();
    public static final Gson gson = new GsonBuilder()
            // .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(LocalDateTime.class, new Helper.LocalDateTimeAdapterWithFormat().nullSafe())
            .setPrettyPrinting()
            .create();

    public InfoConsumer(String... topics) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER_URL + ":" + KAFKA_SERVER_PORT);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CLIENT_ID);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        this.topics = topics;

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                // System.out.print(".");
                consumer.subscribe(Arrays.asList(this.topics));
                // ConsumerRecords<Integer, String> records = consumer.poll(1000);
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                // ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                // System.out.println(".. receive records: " + records.count() + ", stop: " + stop);
                for (ConsumerRecord<String, String> record : records) {
                    Debug.logInfo("Received message: (" + record.key() + ", "
                            + record.value() + ") at offset " + record.offset(), MODULE);
                    // Debug.logInfo(".. with header: %s", MODULE, record.headers());
                    Object values = yaml.load(record.value());
                    System.out.println(values);
                    if (values instanceof Map) {
                        System.out.println("keys: " + ((Map<?, ?>) values).keySet());
                        Object valueType = ((Map<?, ?>) values)
                                .values().stream().findFirst().get();
                        System.out.println("values type: " + valueType
                                .getClass().getName());
                        // if(valueType instanceof List){
                        String fn = ((Map<?, ?>) values).keySet().stream().findFirst().get().toString();
                        Debug.logInfo("invoke %s with %s", MODULE, fn, valueType);
                        Object result = Hubs.HUBS.process(fn, valueType);
                        System.out.println(">> invoke result: " + gson.toJson(result));
                    } else if (values instanceof List) {
                        System.out.println("list: " + values);
                    } else if (values instanceof String) {
                        System.out.println("string: " + values);
                    } else {
                        System.out.println("skip value: " + values);
                    }
                }

            } catch (RuntimeException e) {
                Debug.logError(e, e.getMessage(), MODULE);
            }
        } // end while

        consumer.close();
        System.out.println(".. Oops, stop info-consumer");
    }

    public void serve() {
        executor.submit(() -> {
            run();
            Debug.logInfo(".. consumer end.", MODULE);
            return null;
        });
    }
}

