package cn.schoolpsych.appointment.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SensitiveDataEncryptor {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] key;

    public SensitiveDataEncryptor(@Value("${app.security.sensitive-data-key-base64}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            this.key = null;
            return;
        }
        byte[] decoded = Base64.getDecoder().decode(encodedKey);
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new IllegalStateException("Sensitive data key must decode to 16, 24, or 32 bytes");
        }
        this.key = decoded;
    }

    public byte[] encryptNullable(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        if (key == null) {
            throw new IllegalStateException("SENSITIVE_DATA_KEY_BASE64 is required for sensitive fields");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt sensitive data", exception);
        }
    }

    public String decryptNullable(byte[] encryptedValue) {
        if (encryptedValue == null || encryptedValue.length == 0) {
            return null;
        }
        if (key == null) {
            throw new IllegalStateException("SENSITIVE_DATA_KEY_BASE64 is required for sensitive fields");
        }
        if (encryptedValue.length <= IV_BYTES + (GCM_TAG_BITS / 8)) {
            throw new IllegalArgumentException("Encrypted sensitive value is invalid");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedValue);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Unable to decrypt sensitive data", exception);
        }
    }

    public boolean isConfigured() {
        return key != null;
    }
}
