package cn.schoolpsych.appointment.domain.consent;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "consent_versions")
public class ConsentVersion extends BaseEntity {

    @Column(name = "version_no", nullable = false, unique = true, length = 32)
    private String versionNo;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "mediumtext")
    private String content;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    protected ConsentVersion() {
    }

    public Long getId() {
        return super.getId();
    }
}
