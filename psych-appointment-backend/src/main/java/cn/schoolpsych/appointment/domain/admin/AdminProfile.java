package cn.schoolpsych.appointment.domain.admin;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class AdminProfile extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "department", length = 128)
    private String department;

    @Column(name = "permission_scope_json", columnDefinition = "json")
    private String permissionScopeJson;

    protected AdminProfile() {
    }

    public static AdminProfile create(Long accountId, String name, String department) {
        AdminProfile profile = new AdminProfile();
        profile.accountId = accountId;
        profile.name = name;
        profile.department = department;
        profile.permissionScopeJson = "{}";
        return profile;
    }
}
