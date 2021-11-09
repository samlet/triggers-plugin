package com.bluecc.triggers;

import com.bluecc.generic.EventResponse;
import com.bluecc.pay.SrvBase;
import com.bluecc.pay.SrvRoutines;
import com.google.common.collect.Maps;
import lombok.Builder;
import lombok.Data;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.util.Debug;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import static com.bluecc.generic.Helper.GSON;

public class Hubs extends SrvBase {
    private static final String MODULE = Hubs.class.getName();
    // private static final Logger logger = Logger.getLogger(Hubs.class.getName());

    @Data
    @Builder
    public static class FireProc {
        Function<Object, Object> fn;
        Type[] typeArguments;
    }

    InfoConsumer infoConsumer;
    public static Hubs HUBS;
    Map<String, FireProc> subscribers = Maps.newConcurrentMap();

    @Override
    public boolean start() throws ContainerException {
        HUBS = this;

        // infoConsumer = new InfoConsumer("sagasConsumer");
        infoConsumer = new InfoConsumer(InfoConsumer.InforConfig.builder()
                .subscribeTopics(new String[]{"sagas"})
                .sinkTopic("sagasEvent")
                .build());
        infoConsumer.serve();

        initFuncs();
        System.out.println(" [✔] Hubs started");
        return true;
    }

    @Override
    public void stop() throws ContainerException {
        infoConsumer.stop(true);
    }

    // public void subscribe(String procName, Function<Object, Object> proc) {
    //     subscribers.put(procName, proc);
    // }

    public Object processRaw(String procName, String message) {
        FireProc proc = subscribers.get(procName);
        if (proc != null) {
            Type inputType=proc.typeArguments[0];
            return process(procName, GSON.fromJson(message, inputType));
        } else {
            throw new RuntimeException("Cannot find function " + procName);
        }
    }

    public Object process(String procName, Object message) {
        if (procName.contains("|")) {
            return processFilter(procName.split("\\|"), message);
        }
        FireProc proc = subscribers.get(procName);
        if (proc != null) {
            return proc.fn.apply(message);
        } else {
            Debug.logWarning("Cannot handle " + procName, MODULE);
        }
        return new EventResponse<>("fail", null);
    }

    public Object processFilter(String[] procs, Object message) {
        FireProc proc = subscribers.get(procs[0]);
        if (proc == null) {
            throw new RuntimeException("Cannot find function " + procs[0]);
        }
        Function<Object, Object> fn = proc.fn;
        for (int i = 1; i < procs.length; ++i) {
            FireProc nextProc = subscribers.get(procs[i]);
            if (nextProc != null) {
                fn = fn.andThen(nextProc.fn);
            } else {
                throw new RuntimeException("Cannot find function " + procs[i]);
            }
        }
        return fn.apply(message);
    }

    private void initFuncs() {
        // >echo: [samlet, 18]
        // Function<Object, Object> proc = f -> {
        //     System.out.println("echo: " + f.toString());
        //     return new EventResponse<>("ok", null);
        // };
        // subscribe("echo", proc);

        registerFn(new SysFn(),
                new PartyFn(),
                new OrderFn()
        );
    }

    private void registerFn(Object... fnList) {
        for (Object fn : fnList) {
            for (Method method : fn.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    Function<Object, Object> proc;
                    try {
                        Type genericReturnType = method.getGenericReturnType();
                        Type[] actualTypeArguments = null;
                        if (genericReturnType instanceof ParameterizedType) {
                            actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
                        }
                        proc = (Function<Object, Object>) method.invoke(fn);
                        subscribers.put(method.getName(), FireProc.builder()
                                .fn(proc)
                                .typeArguments(actualTypeArguments)
                                .build());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Cannot register fn " + method.getName());
                    }
                }
            }
        }
    }
}

