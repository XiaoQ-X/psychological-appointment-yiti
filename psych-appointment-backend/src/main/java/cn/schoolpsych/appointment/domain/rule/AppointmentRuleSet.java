package cn.schoolpsych.appointment.domain.rule;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment_rule_sets")
public class AppointmentRuleSet extends BaseEntity {

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "settings_json", nullable = false, columnDefinition = "json")
    private String settingsJson;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "published_by", nullable = false)
    private Long publishedBy;

    protected AppointmentRuleSet() {
    }

    public static AppointmentRuleSet draft(String name, String settingsJson, Long publishedBy) {
        AppointmentRuleSet ruleSet = new AppointmentRuleSet();
        ruleSet.name = name;
        ruleSet.settingsJson = settingsJson;
        ruleSet.active = false;
        ruleSet.effectiveFrom = LocalDateTime.now();
        ruleSet.publishedBy = publishedBy;
        return ruleSet;
    }

    public void updateDraft(String name, String settingsJson, Long publishedBy) {
        if (active || effectiveTo != null) {
            throw new IllegalStateException("Published appointment rules cannot be edited");
        }
        this.name = name;
        this.settingsJson = settingsJson;
        this.publishedBy = publishedBy;
    }

    public void activate(Long publishedBy, LocalDateTime effectiveFrom) {
        this.active = true;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = null;
        this.publishedBy = publishedBy;
    }

    public void deactivate(LocalDateTime effectiveTo) {
        this.active = false;
        this.effectiveTo = effectiveTo;
    }

    public Long getId() {
        return super.getId();
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public Long getPublishedBy() {
        return publishedBy;
    }
}
