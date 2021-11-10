package com.bluecc.generic;

import org.apache.ofbiz.entityext.eca.EntityEcaRule;
import org.apache.ofbiz.entityext.eca.EntityEcaUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class EntityModels {
    Platform platform;
    String entityEcaReaderName;

    @Inject
    public EntityModels(Platform platform){
        this.platform=platform;
        entityEcaReaderName = EntityEcaUtil.getEntityEcaReaderName(platform.getDelegator().getDelegatorBaseName());
    }

    public Map<String, List<EntityEcaRule>> getEntityEventMap(String entityName) {
        Map<String, Map<String, List<EntityEcaRule>>> ecaCache = getEcaRules();
        if (ecaCache == null) return null;
        return ecaCache.get(entityName);
    }

    public Map<String, Map<String, List<EntityEcaRule>>> getEcaRules() {
        return EntityEcaUtil.getEntityEcaCache(this.entityEcaReaderName);
    }
}

