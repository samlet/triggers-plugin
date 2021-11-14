package com.bluecc.triggers;

import com.bluecc.generic.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Builder;
import lombok.Data;
import org.apache.ofbiz.base.container.ContainerConfig;
import org.apache.ofbiz.base.container.ContainerException;
import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
    Injector injector;
    ContainerConfig.Configuration.Property consumeProp;
    ContainerConfig.Configuration.Property produceProp;

    public static final Gson gson = new GsonBuilder()
            // .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
//            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime.class, new Helper.LocalDateTimeAdapter().nullSafe())
            .setPrettyPrinting()
            .create();

    @Override
    protected void initComps(ContainerConfig.Configuration cfg) throws ContainerException {
        super.initComps(cfg);
        consumeProp = cfg.getProperty("consume");
        produceProp = cfg.getProperty("produce");
    }

    @Override
    public boolean start() throws ContainerException {
        HUBS = this;
        injector= Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
            }
        });

        String consume=consumeProp==null?"sagas":consumeProp.value();
        String produce=produceProp==null?"sagasEvent":produceProp.value();

        // infoConsumer = new InfoConsumer("sagasConsumer");
        infoConsumer = new InfoConsumer(InfoConsumer.InforConfig.builder()
                .subscribeTopics(new String[]{consume})
                .sinkTopic(produce)
                .build());
        infoConsumer.serve();

        initFuncs();

        System.out.format(" [✔] Hubs started, consume %s, produce %s\n", consume, produce);
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
            return process(procName, gson.fromJson(message, inputType));
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

    private static ApplicationContext applicationContext;
    private void initFuncs() {
        // >echo: [samlet, 18]
        // Function<Object, Object> proc = f -> {
        //     System.out.println("echo: " + f.toString());
        //     return new EventResponse<>("ok", null);
        // };
        // subscribe("echo", proc);
        // applicationContext =
        //         new AnnotationConfigApplicationContext(Hubs.class);
        // for (String beanName : applicationContext.getBeanDefinitionNames()) {
        //     System.out.println("\t- "+beanName);
        // }

        for(Class<?> clz: getConfigInterfaces()){
            registerFn(injector.getInstance(clz));
        }

        // registerFn(new SysFn(),
        //         new PartyFn(),
        //         new OrderFn(),
        //         new ServiceFn()
        // );
    }

    public Set<Class<?>> getConfigInterfaces() {
        Reflections reflections = new Reflections("com.bluecc.triggers",
                new TypeAnnotationsScanner());
        return reflections.getTypesAnnotatedWith(Configuration.class);
    }

    void scanMethods(){
        for (Method m : getFunctions()) {
            String returnType=m.getReturnType().getName();
            // System.out.format("- %s.%s -> %s\n",
            //         m.getDeclaringClass().getName(),
            //         m.getName(),
            //         returnType);
            if(returnType.equals("java.util.function.Function")){
                System.out.println(".. register fn - "+m.getName());
                registerMethod(injector.getInstance(m.getDeclaringClass()), m);
            }
        }
    }
    private Set<Method> getFunctions() {
        Reflections reflections = new Reflections(
                "com.bluecc.triggers",
                new MethodAnnotationsScanner());
        return reflections.getMethodsAnnotatedWith(Bean.class);
    }

    private void registerFn(Object... fnList) {
        for (Object fn : fnList) {
            for (Method method : fn.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class)) {
                    registerMethod(fn, method);
                }
            }
        }
    }

    private void registerMethod(Object fn, Method method) {
        String returnType=method.getReturnType().getSimpleName();
        System.out.format("\t- %s: %s -> %s\n",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                returnType);
        Preconditions.checkArgument(returnType.equals("Function"),
                "Only register function with Bean annotation");

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

    public static Object getComponent(String name){
        switch (name){
            case "models": return HUBS.injector.getInstance(EntityModels.class);
            case "services": return HUBS.injector.getInstance(ServiceModels.class);
            case "consumer": return HUBS.infoConsumer;
            case "producer": return HUBS.infoConsumer.getProducer();
            default:
                throw new GeneralRuntimeException("No such component "+name);
        }
    }
}

