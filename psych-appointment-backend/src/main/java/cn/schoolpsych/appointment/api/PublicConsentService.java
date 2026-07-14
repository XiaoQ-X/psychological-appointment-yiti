package cn.schoolpsych.appointment.api;

import cn.schoolpsych.appointment.domain.consent.ConsentVersion;
import cn.schoolpsych.appointment.repository.ConsentVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicConsentService {

    private final ConsentVersionRepository consentVersionRepository;

    public PublicConsentService(ConsentVersionRepository consentVersionRepository) {
        this.consentVersionRepository = consentVersionRepository;
    }

    @Transactional(readOnly = true)
    public PublicConsentResponse current() {
        ConsentVersion version = consentVersionRepository
                .findFirstByStatusOrderByPublishedAtDesc("PUBLISHED")
                .orElseThrow(() -> new IllegalArgumentException("No published consent version"));
        return new PublicConsentResponse(
                version.getId(),
                version.getVersionNo(),
                version.getTitle(),
                version.getContent(),
                version.getPublishedAt());
    }
}
