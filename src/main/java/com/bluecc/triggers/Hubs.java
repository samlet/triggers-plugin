package com.bluecc.triggers;

import com.bluecc.generic.EventResponse;
import com.bluecc.pay.SrvBase;
import com.bluecc.pay.SrvRoutines;
import com.google.common.collect.Maps;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.util.Debug;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

public class Hubs extends SrvBase {
    private static final String MODULE = Hubs.class.getName();
    // private static final Logger logger = Logger.getLogger(Hubs.class.getName());

    InfoConsumer infoConsumer;
    public static Hubs HUBS;
    Map<String, Function<Object, Object>> subscribers = Maps.newConcurrentMap();

    @Override
    public boolean start() throws ContainerException {
        HUBS = this;

        // infoConsumer = new InfoConsumer("sagasConsumer");
        infoConsumer = new InfoConsumer("sagas");
        infoConsumer.serve();

        initFuncs();
        System.out.println(" [✔] Hubs started");
        return true;
    }

    @Override
    public void stop() throws ContainerException {
        infoConsumer.stop(true);
    }

    public void subscribe(String procName, Function<Object, Object> proc) {
        subscribers.put(procName, proc);
    }

    public Object process(String procName, Object message) {
        if (procName.contains("|")) {
            return processFilter(procName.split("\\|"), message);
        }
        Function<Object, Object> proc = subscribers.get(procName);
        if (proc != null) {
            return proc.apply(message);
        } else {
            Debug.logWarning("Cannot handle " + procName, MODULE);
        }
        return new EventResponse<>("fail", null);
    }

    public Object processFilter(String[] procs, Object message) {
        Function<Object, Object> proc = subscribers.get(procs[0]);
        if (proc == null) {
            throw new RuntimeException("Cannot find function " + procs[0]);
        }
        for (int i = 1; i < procs.length; ++i) {
            Function<Object, Object> nextProc = subscribers.get(procs[i]);
            if (nextProc != null) {
                proc = proc.andThen(nextProc);
            } else {
                throw new RuntimeException("Cannot find function " + procs[i]);
            }
        }
        return proc.apply(message);
    }

    private void initFuncs() {
        // >echo: [samlet, 18]
        Function<Object, Object> proc = f -> {
            System.out.println("echo: " + f.toString());
            return new EventResponse<>("ok", null);
        };
        subscribe("echo", proc);

        registerFn(new SysFn(),
                new PartyFn());
    }

    private void registerFn(Object... fnList) {
        for (Object fn : fnList) {
            for (Method method : fn.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    Function<Object, Object> proc = null;
                    try {
                        proc = (Function<Object, Object>) method.invoke(fn);
                        subscribers.put(method.getName(), proc);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Cannot register fn " + method.getName());
                    }
                }
            }
        }
    }
}

