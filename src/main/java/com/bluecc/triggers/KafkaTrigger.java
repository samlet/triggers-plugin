package com.bluecc.triggers;

import com.adapters.srv.ProcContext;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.ofbiz.base.util.Debug;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class KafkaTrigger {
    private static final String MODULE = KafkaTrigger.class.getName();
    private static KafkaTrigger INSTANCE;

    public static KafkaTrigger getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new KafkaTrigger();
        }
        return INSTANCE;
    }

    InfoProducer producer;
    private KafkaTrigger(){
        this.producer=new InfoProducer("sagasProducer", true);
    }

    public void createTopic(Properties properties, String topicName) throws ExecutionException, InterruptedException {
        try (Admin admin = Admin.create(properties)) {
            int partitions = 1;
            short replicationFactor = 1;
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);

            CreateTopicsResult result = admin.createTopics(
                    Collections.singleton(newTopic)
            );

            KafkaFuture<Void> future = result.values().get(topicName);
            future.get();
        }
    }

    public void fire(ProcContext ctx){
        Debug.logInfo("fire on service invoke: "+ctx.getService().getName(), MODULE);
    }

    public void succ(ProcContext ctx) {
        Debug.logInfo("succ on service invoke: "+ctx.getService().getName(), MODULE);
    }
}

