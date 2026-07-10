package cn.schoolpsych.appointment.domain.notice;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "notices")
public class Notice extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "mediumtext")
    private String content;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "publish_at")
    private LocalDateTime publishAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    protected Notice() {
    }
}
