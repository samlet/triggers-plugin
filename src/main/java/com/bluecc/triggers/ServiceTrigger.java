package com.bluecc.triggers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class ServiceTrigger {
    private static final String MODULE = ServiceTrigger.class.getName();
    private static ServiceTrigger INSTANCE;

    public static ServiceTrigger getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ServiceTrigger();
        }
        return INSTANCE;
    }

    public static final String DEFAULT_QUEUE_NAME = "task_queue";
    private final ConnectionFactory factory;

    Connection connection=null;
    Channel channel=null;
    private ServiceTrigger(){
        factory = new ConnectionFactory();
        factory.setHost("localhost");

        init();
    }

    private void init(){
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(DEFAULT_QUEUE_NAME, true, false, false, null);
        } catch (IOException | TimeoutException e) {
            Debug.logError(e, "Connect to rabbitmq fail: "+e.getMessage(), MODULE);
        }
    }

    public void publish(String message) throws IOException {
        if(this.channel!=null){
            channel.basicPublish("", DEFAULT_QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes(StandardCharsets.UTF_8));
            Debug.logInfo(" [x] Sent '" + message + "'", MODULE);
        }
    }
}

