package com.bluecc.triggers;

import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class PartyFn extends FnBase {
    // >findParty: admin
    @Bean
    Function<String, GenericValue> findParty() {
        return id -> {
            try {
                return from("Party")
                        .where("partyId", id)
                        .cache().queryOne();
            } catch (GenericEntityException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }
}

