package com.bluecc.triggers;

import com.bluecc.generic.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.Data;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.ofbiz.base.util.Debug;
import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
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
    // private final String[] topics;

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

    @Data
    @Builder
    public static class InforConfig {
        String[] subscribeTopics;
        String sinkTopic;
    }

    @Data
    @Builder
    public static class ResultData {
        String resultStatus;
        String caller;
        Object data;
    }

    InforConfig config;
    InfoProducer producer;

    public InfoConsumer(InforConfig config) {
        this.config = config;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER_URL + ":" + KAFKA_SERVER_PORT);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CLIENT_ID);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        // this.topics = topics;
        producer = new InfoProducer(config.getSinkTopic(), true);
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                // System.out.print(".");
                consumer.subscribe(Arrays.asList(this.config.subscribeTopics));
                // ConsumerRecords<Integer, String> records = consumer.poll(1000);
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                // ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                // System.out.println(".. receive records: " + records.count() + ", stop: " + stop);
                for (ConsumerRecord<String, String> record : records) {
                    Debug.logInfo("Received message: (" + record.key() + ", "
                            + record.value() + ") at offset " + record.offset(), MODULE);
                    Debug.logInfo(".. with header: %s", MODULE, record.headers());
                    String cntType="text/yaml";
                    String dataType="String";
                    String fn="";
                    for (Header header : record.headers()) {
                        if (header.key().equals("type") ) {
                            String val = new String(header.value(), StandardCharsets.UTF_8);
                            System.out.format("\t%s = %s\n", header.key(), val);

                            cntType=val;
                        }
                        if(header.key().equals("contentType")){
                            String val = new String(header.value(), StandardCharsets.UTF_8);
                            System.out.format("\t%s = %s\n", header.key(), val);
                        }
                        if(header.key().equals("dataType")){
                            dataType = new String(header.value(), StandardCharsets.UTF_8);
                            System.out.format("\t%s = %s\n", header.key(), dataType);
                        }
                        if(header.key().equals("fn")){
                            fn = new String(header.value(), StandardCharsets.UTF_8);
                            System.out.format("\t%s = %s\n", header.key(), dataType);
                        }
                    }

                    if(cntType.equals("text/yaml")) {
                        Object values = yaml.load(record.value());
                        System.out.println(values);
                        if (values instanceof Map) {
                            System.out.println("keys: " + ((Map<?, ?>) values).keySet());
                            Object valueType = ((Map<?, ?>) values)
                                    .values().stream().findFirst().get();
                            System.out.println("values type: " + valueType
                                    .getClass().getName());
                            // if(valueType instanceof List){
                            fn = ((Map<?, ?>) values).keySet().stream().findFirst().get().toString();
                            Debug.logInfo("invoke %s with %s", MODULE, fn, valueType);
                            Object result = Hubs.HUBS.process(fn, valueType);
                            sendResult(fn, result);
                        } else if (values instanceof List) {
                            System.out.println("list: " + values);
                        } else if (values instanceof String) {
                            System.out.println("string: " + values);
                        } else {
                            System.out.println("skip value: " + values);
                        }
                    }else if(cntType.equals("application/json")){
                        System.out.println(record.value());
                        if(!fn.isEmpty()){
                            Object result = Hubs.HUBS.processRaw(fn, record.value());
                            sendResult(fn, result);
                        }
                    }else{
                        Debug.logWarning("Cannot handle content-type %s", MODULE, cntType);
                    }
                }

            } catch (RuntimeException e) {
                Debug.logError(e, e.getMessage(), MODULE);
            }
        } // end while

        consumer.close();
        System.out.println(".. Oops, stop info-consumer");
    }

    private void sendResult(String fn, Object result) {
        String resultJson = gson.toJson(ResultData.builder()
                .resultStatus("ok")
                .caller(fn)
                .data(result)
                .build());
        System.out.println(">> invoke result: " + resultJson);
        producer.send(resultJson);
    }

    public void serve() {
        executor.submit(() -> {
            run();
            Debug.logInfo(".. consumer end.", MODULE);
            return null;
        });
    }
}

