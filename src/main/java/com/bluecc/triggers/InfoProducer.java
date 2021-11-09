package com.bluecc.triggers;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.ofbiz.base.util.Debug;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public class InfoProducer {
    private static final String MODULE = InfoProducer.class.getName();
    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final Boolean isAsync;

    public static final String KAFKA_SERVER_URL = "localhost";
    public static final int KAFKA_SERVER_PORT = 9092;
    public static final String CLIENT_ID = "sagasProducer";
    AtomicLong messageNo = new AtomicLong(1);

    public InfoProducer(String topic, Boolean isAsync) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", KAFKA_SERVER_URL + ":" + KAFKA_SERVER_PORT);
        properties.put("client.id", CLIENT_ID);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(properties);
        this.topic = topic;
        this.isAsync = isAsync;
    }

    public void send(String messageStr) {
        long messageNumber = messageNo.getAndIncrement();
        if (isAsync) { // Send asynchronously
            long startTime = System.currentTimeMillis();
            ProducerRecord<String, String> rec = new ProducerRecord<>(topic,
                    Long.toString(messageNumber), messageStr);
            rec.headers().add("serial", Long.toString(messageNumber).getBytes(StandardCharsets.UTF_8));
            producer.send(rec,
                    new KafkaCallBack(startTime, messageNumber, messageStr));
        } else { // Send synchronously
            try {
                producer.send(new ProducerRecord<String, String>(topic,
                        Long.toString(messageNumber),
                        messageStr)).get();
                System.out.println("Sent message: (" + messageStr + ")");
            } catch (InterruptedException | ExecutionException e) {
                // e.printStackTrace();
                // handle the exception
                Debug.logError(e, MODULE);
            }
        }
    }
}

class KafkaCallBack implements Callback {
    private static final String MODULE = KafkaCallBack.class.getName();
    private final long startTime;
    private final long key;
    private final String message;

    public KafkaCallBack(long startTime, long key, String message) {
        this.startTime = startTime;
        this.key = key;
        this.message = message;
    }

    /**
     * onCompletion method will be called when the record sent to the Kafka Server has been acknowledged.
     *
     * @param metadata  The metadata contains the partition and offset of the record. Null if an error occurred.
     * @param exception The exception thrown during processing of this record. Null if no error occurred.
     */
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (metadata != null) {
            Debug.logVerbose(
                    "message(" + key + ", " + message + ") sent to partition(" + metadata.partition() +
                            "), " +
                            "offset(" + metadata.offset() + ") in " + elapsedTime + " ms", MODULE);
        } else {
            // exception.printStackTrace();
            Debug.logError(exception, MODULE);
        }
    }
}