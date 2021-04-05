package com.bluecc.triggers;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.*;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ServiceConfiguratorTest {

    @Test
    public void checkService() throws NotRegisteredException {
        Consul client = Consul.builder().build(); // connect on localhost
        AgentClient agentClient = client.agentClient();

        String serviceId = "1";
        Registration service = ImmutableRegistration.builder()
                .id(serviceId)
                .name("myService")
                .port(8080)
                .check(Registration.RegCheck.ttl(3L)) // registers with a TTL of 3 seconds
                .tags(Collections.singletonList("tag1"))
                .meta(Collections.singletonMap("version", "1.0"))
                .build();

        agentClient.register(service);

        // Check in with Consul (serviceId required only).
        // Client will prepend "service:" for service level checks.
        // Note that you need to continually check in before the TTL expires, otherwise your service's state will be marked as "critical".
        agentClient.pass(serviceId);
    }

    @Test
    public void testKv(){
        Consul client = Consul.builder().build(); // connect on localhost
        KeyValueClient kvClient = client.keyValueClient();

        kvClient.putValue("foo", "bar");
        String value = kvClient.getValueAsString("foo").get(); // bar
        System.out.println("get value "+value);
    }
}