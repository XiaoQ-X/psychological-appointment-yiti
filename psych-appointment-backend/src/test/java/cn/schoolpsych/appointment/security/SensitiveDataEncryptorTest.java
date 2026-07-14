package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(encryptor.decryptNullable(encrypted)).isEqualTo("13800000000");
    }

    @Test
    void returnsNullForBlankText() {
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor("");

        assertThat(encryptor.encryptNullable(" ")).isNull();
        assertThat(encryptor.decryptNullable(null)).isNull();
    }

    @Test
    void rejectsCiphertextWhenKeyDoesNotMatch() {
        byte[] firstKey = new byte[32];
        byte[] secondKey = new byte[32];
        secondKey[0] = 1;
        SensitiveDataEncryptor first = new SensitiveDataEncryptor(Base64.getEncoder().encodeToString(firstKey));
        SensitiveDataEncryptor second = new SensitiveDataEncryptor(Base64.getEncoder().encodeToString(secondKey));

        byte[] encrypted = first.encryptNullable("sensitive-marker");

        assertThatThrownBy(() -> second.decryptNullable(encrypted))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
