package com.bluecc.triggers;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;

import java.util.Optional;

public class ServiceConfigurator {
    private static final String MODULE = ServiceConfigurator.class.getName();
    private static ServiceConfigurator INSTANCE;

    public static ServiceConfigurator getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ServiceConfigurator();
        }
        return INSTANCE;
    }

    public String getQueueName(){
        return ServiceTrigger.DEFAULT_QUEUE_NAME;
    }

    KeyValueClient kvClient;
    private ServiceConfigurator() {
        Consul client = Consul.builder().build(); // connect on localhost
        kvClient = client.keyValueClient();
    }

    public void putData(String key, String value){
        kvClient.putValue(key, value);
    }

    public Optional<String> getData(String key){
        return kvClient.getValueAsString(key);
    }
}

