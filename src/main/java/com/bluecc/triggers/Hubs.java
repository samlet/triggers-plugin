package com.bluecc.triggers;

import com.bluecc.pay.SrvBase;
import com.bluecc.pay.SrvRoutines;
import org.apache.ofbiz.base.container.ContainerException;

import java.util.logging.Logger;

public class Hubs extends SrvBase {
    private static final Logger logger = Logger.getLogger(Hubs.class.getName());
    InfoConsumer infoConsumer;
    @Override
    public boolean start() throws ContainerException {
        infoConsumer=new InfoConsumer("sagasConsumer");
        infoConsumer.serve();
        System.out.println(" [✔] Hubs started");
        return true;
    }

    @Override
    public void stop() throws ContainerException {
        infoConsumer.stop(true);
    }
}
