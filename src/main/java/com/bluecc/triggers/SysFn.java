package com.bluecc.triggers;

import com.bluecc.generic.EventResponse;
import com.google.common.base.CaseFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Invoke with filter pattern:
 * <pre>
 * >uppercase|camelcase: [hi, world]
 * </pre>
 */
@Configuration
public class SysFn {
    @Bean
    Function<List<?>, EventResponse<?>> print(){
        return list -> {
            System.out.println("print: "+list.toString());
            return new EventResponse<>("ok", null);
        };
    }

    // >uppercase: [hell, word]
    @Bean
    Function<List<String>, List<String>> uppercase(){
        return list -> list.stream().map(e -> e.toUpperCase())
                .collect(Collectors.toList());
    }

    @Bean
    public Function<List<String>, List<String>> lowercase() {
        return list -> list.stream().map(e -> e.toLowerCase())
                .collect(Collectors.toList());
    }

    public static String toCamelcase(String str){
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str);
    }

    @Bean
    public Function<List<String>, List<String>> camelcase() {
        return list -> list.stream().map(e -> toCamelcase(e))
                .collect(Collectors.toList());
    }

    // >commands: verbose
    @Bean
    Function<String, List<String>> commands(){
        return level -> new ArrayList<>(Hubs.HUBS.subscribers.keySet());
    }
}

