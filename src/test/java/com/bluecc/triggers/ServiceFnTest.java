package com.bluecc.triggers;

import org.junit.Test;

import java.util.Map;

import static com.bluecc.generic.Helper.GSON;
import static org.junit.Assert.*;

public class ServiceFnTest {
    public static class ServiceRequest{
        String serviceName;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public Map<String, Object> getServiceInParams() {
            return serviceInParams;
        }

        public void setServiceInParams(Map<String, Object> serviceInParams) {
            this.serviceInParams = serviceInParams;
        }

        Map<String, Object> serviceInParams;

        @Override
        public String toString() {
            return "ServiceRequest{" +
                    "serviceName='" + serviceName + '\'' +
                    ", serviceInParams=" + serviceInParams +
                    '}';
        }
    }

    @Test
    public void service() {
        String input="{\n" +
                "\t\"serviceName\": \"performFindList\",\n" +
                "\t\"serviceInParams\": {\n" +
                "\t\t\"entityName\": \"Product\",\n" +
                "\t\t\"viewIndex\": 0,\n" +
                "\t\t\"viewSize\": \"15\",\n" +
                "\t\t\"inputFields\": {\n" +
                "\t\t\t\"productId\": \"GZ-2644\"\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";
        ServiceRequest r=GSON.fromJson(input, ServiceRequest.class);
        System.out.println(r);
    }
}