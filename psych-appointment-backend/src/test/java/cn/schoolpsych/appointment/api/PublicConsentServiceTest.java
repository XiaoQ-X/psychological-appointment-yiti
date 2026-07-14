package cn.schoolpsych.appointment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.consent.ConsentVersion;
import cn.schoolpsych.appointment.repository.ConsentVersionRepository;
import org.junit.jupiter.api.Test;

class PublicConsentServiceTest {

    @Test
    void returnsLatestPublishedConsentVersion() {
        ConsentVersionRepository repository = mock(ConsentVersionRepository.class);
        ConsentVersion version = mock(ConsentVersion.class);
        LocalDateTime publishedAt = LocalDateTime.of(2026, 7, 1, 9, 0);
        when(version.getId()).thenReturn(3L);
        when(version.getVersionNo()).thenReturn("v3");
        when(version.getTitle()).thenReturn("Student consent");
        when(version.getContent()).thenReturn("Published content");
        when(version.getPublishedAt()).thenReturn(publishedAt);
        when(repository.findFirstByStatusOrderByPublishedAtDesc("PUBLISHED"))
                .thenReturn(Optional.of(version));

        PublicConsentResponse response = new PublicConsentService(repository).current();

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.versionNo()).isEqualTo("v3");
        assertThat(response.title()).isEqualTo("Student consent");
        assertThat(response.content()).isEqualTo("Published content");
        assertThat(response.publishedAt()).isEqualTo(publishedAt);
    }
}
