package com.bluecc.generic;

import org.apache.ofbiz.base.container.Container;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericDelegator;
import org.apache.ofbiz.service.GenericAbstractDispatcher;
import org.apache.ofbiz.service.ServiceContainer;

import java.util.List;
import java.util.logging.Logger;

import static py4j.GatewayServer.DEFAULT_PORT;

public abstract class SrvBase implements Container {
    private static final Logger logger = Logger.getLogger(SrvBase.class.getName());

    protected String containerName;
    protected String configFile;

    protected ContainerConfig.Configuration.Property delegatorProp;
    protected GenericDelegator delegator;
    protected GenericAbstractDispatcher dispatcher;
    protected String host;
    protected int port;

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        this.containerName = name;
        this.configFile = configFile;

        // get the container config
        ContainerConfig.Configuration cfg = ContainerConfig.getConfiguration(containerName);
        initComps(cfg);
    }

    protected void initComps(ContainerConfig.Configuration cfg) throws ContainerException {

        ContainerConfig.Configuration.Property lookupHostProp = cfg.getProperty("bound-host");
        ContainerConfig.Configuration.Property lookupPortProp = cfg.getProperty("bound-port");
        this.delegatorProp = cfg.getProperty("delegator-name");

        // check the required delegator-name property
        if (delegatorProp == null || UtilValidate.isEmpty(delegatorProp.value())) {
            throw new ContainerException("Invalid delegator-name defined in container configuration");
        }

        this.host = lookupHostProp == null || lookupHostProp.value() == null ? "localhost" : lookupHostProp.value();
        this.port = lookupPortProp == null || lookupPortProp.value() == null ? DEFAULT_PORT : Integer.parseInt(lookupPortProp.value());

        // get the delegator for this container
        this.delegator = (GenericDelegator) DelegatorFactory.getDelegator(delegatorProp.value());

        if(this.delegator!=null) {
            // create the LocalDispatcher
            this.dispatcher = (GenericAbstractDispatcher) ServiceContainer.getLocalDispatcher(
                    delegator.getDelegatorName(), delegator);
        }
    }

    @Override
    public String getName() {
        return containerName;
    }

}

