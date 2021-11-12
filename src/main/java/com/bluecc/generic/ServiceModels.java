package com.bluecc.generic;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;
import org.apache.ofbiz.entityext.eca.EntityEcaRule;
import org.apache.ofbiz.entityext.eca.EntityEcaUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceDispatcher;
import org.apache.ofbiz.service.eca.ServiceEcaRule;
import org.apache.ofbiz.service.eca.ServiceEcaUtil;
import org.apache.ofbiz.service.group.GroupModel;
import org.apache.ofbiz.service.group.GroupServiceModel;
import org.apache.ofbiz.service.group.ServiceGroupEngine;
import org.apache.ofbiz.service.group.ServiceGroupReader;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class ServiceModels {
    Platform platform;

    @Inject
    public ServiceModels(Platform platform) {
        this.platform = platform;
    }

    public Map<String, Map<String, List<ServiceEcaRule>>> getEcaRules() {
        if (ServiceEcaUtil.ecaCache == null) ServiceEcaUtil.readConfig();
        return ServiceEcaUtil.ecaCache;
    }

    public Map<String, List<ServiceEcaRule>> getServiceEventMap(String serviceName) {
        return getEcaRules().get(serviceName);
    }

    @Data
    @Builder
    public static class ServiceGroup {
        ModelService model;
        GroupModel group;

        public List<GroupServiceModel> getServices() {
            return group.getServices();
        }
    }

    public List<ServiceGroup> getServiceGroups() throws GenericServiceException {
        DispatchContext disp = platform.getDispatcher().getDispatchContext();
        List<ServiceGroup> services = Lists.newArrayList();
        ServiceGroupEngine groupEngine = getGroupEngine();
        for (String serviceName : disp.getAllServiceNames()) {
            ModelService modelService = disp.getModelService(serviceName);
            if (modelService.getEngineName().equals("group")) {
                GroupModel groupModel=modelService.getInternalGroup();
                if(groupModel==null) {
                    groupModel = ServiceGroupReader.getGroupModel(groupEngine.getLocation(modelService));
                }
                Preconditions.checkArgument(groupModel!=null,
                        "Cannot find group model for "+serviceName);

                services.add(ServiceGroup.builder()
                        .model(modelService)
                        .group(groupModel)
                        .build());
            }
        }
        return services;
    }

    private ServiceGroupEngine getGroupEngine() {
        return new ServiceGroupEngine(
                ServiceDispatcher.getInstance(platform.getDelegator()));
    }
}

