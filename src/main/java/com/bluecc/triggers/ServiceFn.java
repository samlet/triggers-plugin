package com.bluecc.triggers;

import com.adapters.srv.Requester;
import lombok.Builder;
import lombok.Data;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class ServiceFn extends FnBase{
    private static final String MODULE = ServiceFn.class.getName();
    @Data
    public static class ServiceRequest{
        String serviceName;
        Map<String, Object> serviceInParams;
    }

    @Data
    @Builder
    public static class ServiceResult{
        String result;
        Map<String, Object> serviceOutputParams;
        String message;
    }

    @Bean
    Function<ServiceRequest, ServiceResult> service(){
        return request -> {
            Requester requester=new Requester();
            try {
                Map<String, Object> ctx=request.getServiceInParams();
                Map<String, Object> data= requester.responseAsJson(request.getServiceName(), ctx, getUserLogin());
                return ServiceResult.builder()
                        .result(ServiceUtil.isSuccess(data)?"ok":"fail")
                        .serviceOutputParams(data)
                        .build();
            } catch (GenericServiceException|GenericEntityException e) {
                Debug.logError(e, e.getMessage(), MODULE);
                return ServiceResult.builder()
                        .result("fail")
                        .message(e.getMessage())
                        .build();
            }
        };
    }
}
