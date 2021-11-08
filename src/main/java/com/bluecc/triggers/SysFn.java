package com.bluecc.triggers;

import com.bluecc.generic.EventResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
}

