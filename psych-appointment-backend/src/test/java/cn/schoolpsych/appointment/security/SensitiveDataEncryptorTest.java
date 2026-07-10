package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class SensitiveDataEncryptorTest {

    @Test
    void encryptsNullableTextWhenKeyIsConfigured() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor(Base64.getEncoder().encodeToString(key));

        byte[] encrypted = encryptor.encryptNullable("13800000000");

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).hasSizeGreaterThan(12);
        assertThat(new String(encrypted)).doesNotContain("13800000000");
    }

    @Test
    void returnsNullForBlankText() {
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor("");

        assertThat(encryptor.encryptNullable(" ")).isNull();
    }
}
