package com.project.Emotiate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Slf4j
@Component
public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Serialize an object to JSON string
    public static String toJson(Object obj) {

        try { return mapper.writeValueAsString(obj); }
        catch (Exception e) { throw new RuntimeException("JSON serialize failed", e); }
    }


    // Deserialize a JSON string to an object of specified class
    public static <T> T fromJson(String json, Class<T> targetClass) {

        try { return mapper.readValue(json, targetClass); }
        catch (Exception e) { throw new RuntimeException("JSON deserialize failed: " + json, e); }
    }
}