package com.bluecc.triggers;

import com.drafts.generic.Stack;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericAbstractDispatcher;
import org.apache.ofbiz.service.ServiceContainer;

public class FnBase {
    private GenericDelegator delegator;
    private GenericAbstractDispatcher dispatcher;

    public GenericDelegator getDelegator() {
        if(delegator==null){
            delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
        }
        return delegator;
    }

    public GenericAbstractDispatcher getDispatcher() {
        if(dispatcher==null){
            delegator=getDelegator();
            this.dispatcher = (GenericAbstractDispatcher) ServiceContainer.getLocalDispatcher(delegator.getDelegatorName(),
                    delegator);
        }
        return dispatcher;
    }

    public GenericValue getUserLogin() throws GenericEntityException {
        return EntityQuery.use(getDelegator()).from("UserLogin")
                .where("userLoginId", "system")
                .cache().queryOne();
    }

    public GenericValue getUserLogin(String userLoginId)  {
        try {
            return EntityQuery.use(getDelegator()).from("UserLogin")
                    .where("userLoginId", userLoginId)
                    .cache().queryOne();
        } catch (GenericEntityException e) {
            throw new GeneralRuntimeException(e.getMessage(), e);
        }
    }

    public EntityQuery from(String entityName){
        return EntityQuery.use(getDelegator()).from(entityName);
    }
}
