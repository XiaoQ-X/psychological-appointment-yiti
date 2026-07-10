package cn.schoolpsych.appointment.domain.counselor;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "counselors")
public class Counselor extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "title", length = 128)
    private String title;

    @Column(name = "gender", length = 16)
    private String gender;

    @Column(name = "campus_id")
    private Long campusId;

    @Column(name = "expertise_json", columnDefinition = "json")
    private String expertiseJson;

    @Column(name = "intro", columnDefinition = "text")
    private String intro;

    @Column(name = "training_background", columnDefinition = "text")
    private String trainingBackground;

    @Column(name = "service_modes_json", columnDefinition = "json")
    private String serviceModesJson;

    @Column(name = "max_daily_count", nullable = false)
    private int maxDailyCount;

    @Column(name = "is_visible", nullable = false)
    private boolean visible;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected Counselor() {
    }

    public static Counselor create(
            Long accountId,
            String name,
            String avatarUrl,
            String title,
            String gender,
            Long campusId,
            String expertiseJson,
            String intro,
            String trainingBackground,
            String serviceModesJson,
            int maxDailyCount,
            boolean visible,
            String status) {
        Counselor counselor = new Counselor();
        counselor.accountId = accountId;
        counselor.update(
                name,
                avatarUrl,
                title,
                gender,
                campusId,
                expertiseJson,
                intro,
                trainingBackground,
                serviceModesJson,
                maxDailyCount,
                visible,
                status);
        return counselor;
    }

    public void update(
            String name,
            String avatarUrl,
            String title,
            String gender,
            Long campusId,
            String expertiseJson,
            String intro,
            String trainingBackground,
            String serviceModesJson,
            int maxDailyCount,
            boolean visible,
            String status) {
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.title = title;
        this.gender = gender;
        this.campusId = campusId;
        this.expertiseJson = expertiseJson;
        this.intro = intro;
        this.trainingBackground = trainingBackground;
        this.serviceModesJson = serviceModesJson;
        this.maxDailyCount = maxDailyCount;
        this.visible = visible;
        this.status = status;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getGender() {
        return gender;
    }

    public Long getCampusId() {
        return campusId;
    }

    public String getExpertiseJson() {
        return expertiseJson;
    }

    public String getIntro() {
        return intro;
    }

    public String getTrainingBackground() {
        return trainingBackground;
    }

    public String getServiceModesJson() {
        return serviceModesJson;
    }

    public int getMaxDailyCount() {
        return maxDailyCount;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getStatus() {
        return status;
    }
}
