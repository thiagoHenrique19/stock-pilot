package com.stockpilot.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpilot.core.port.out.PayloadCodec;
import org.springframework.stereotype.Component;

@Component
class JacksonPayloadCodec implements PayloadCodec {

    private final ObjectMapper objectMapper;

    JacksonPayloadCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String write(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize " + payload.getClass().getSimpleName(), e);
        }
    }

    @Override
    public <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot read stored " + type.getSimpleName(), e);
        }
    }
}
