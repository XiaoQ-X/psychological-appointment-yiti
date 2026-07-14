package cn.schoolpsych.appointment.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class SensitiveDataCodec {

    private final SensitiveDataEncryptor encryptor;
    private final ObjectMapper objectMapper;

    public SensitiveDataCodec(SensitiveDataEncryptor encryptor, ObjectMapper objectMapper) {
        this.encryptor = encryptor;
        this.objectMapper = objectMapper;
    }

    public byte[] encryptText(String value) {
        return encryptor.encryptNullable(value);
    }

    public String decryptText(byte[] value) {
        return encryptor.decryptNullable(value);
    }

    public byte[] encryptJson(Object value) {
        if (value == null) {
            return null;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Sensitive value cannot be encoded", exception);
        }
        return encryptor.encryptNullable(json);
    }

    public <T> T decryptJson(byte[] value, Class<T> type) {
        String json = encryptor.decryptNullable(value);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Sensitive value cannot be decoded", exception);
        }
    }

    public boolean isConfigured() {
        return encryptor.isConfigured();
    }
}
