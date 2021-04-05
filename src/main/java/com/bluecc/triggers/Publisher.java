package com.bluecc.triggers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.io.IOException;
import java.util.Map;

public class Publisher {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> publishMessage(DispatchContext dctx, Map<String, ?> context) throws IOException {
        String msg=(String) context.get("message");
        ServiceTrigger.getInstance().publish(msg);
        Map<String, Object> response = ServiceUtil.returnSuccess();
        Map<String,?> resp=UtilMisc.toMap("queue", ServiceTrigger.DEFAULT_QUEUE_NAME);
        response.put("resp", resp);
        return response;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> publishData(DispatchContext dctx, Map<String, ?> context) throws IOException {
        String key=(String) context.get("key");
        Map<String,?> payload=(Map<String,?>) context.get("payload");
        String json = new ObjectMapper().writeValueAsString(payload);
        ServiceConfigurator.getInstance().putData(key, json);
        return ServiceUtil.returnSuccess();
    }
}

