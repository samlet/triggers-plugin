package com.bluecc.triggers;

import com.bluecc.generic.EventResponse;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.*;

public class SysFnTest {
    SysFn sysFn = new SysFn();
    Map<String, Function<Object, Object>> subscribers = Maps.newConcurrentMap();
    @Before
    public void setUp() throws Exception {
        System.out.println("..");
        for (Method method : sysFn.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                Function<Object, Object> proc = (Function<Object, Object>) method.invoke(sysFn);
                subscribers.put(method.getName(), proc);
            }
        }
    }

    @Test
    public void print() throws InvocationTargetException, IllegalAccessException {
        Function<Object, Object> proc = subscribers.get("print");
        assertNotNull(proc);
        List<String> message = Lists.newArrayList("hello", "world");
        EventResponse<?> response = (EventResponse<?>) proc.apply(message);
        System.out.println(response);
    }

    @Test
    public void uppercase() {
        List<String> rs = (List<String>) subscribers.get("uppercase")
                .apply(Lists.newArrayList("hello", "world"));
        rs.forEach(e -> System.out.println(e));

        rs = (List<String>) subscribers.get("camelcase")
                .apply(Lists.newArrayList("hello_world"));
        rs.forEach(e -> System.out.println(e));

        rs = (List<String>) subscribers.get("uppercase")
                .andThen(subscribers.get("camelcase"))
                .apply(Lists.newArrayList("hello_world", "hi, samlet"));
        rs.forEach(e -> System.out.println(e));
    }
}