package cn.schoolpsych.appointment.domain.account;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountStatus status;

    @Column(name = "wx_openid", unique = true, length = 128)
    private String wxOpenid;

    @Column(name = "wx_unionid", length = 128)
    private String wxUnionid;

    @Column(name = "force_password_change", nullable = false)
    private boolean forcePasswordChange;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "locked_until")
    private java.time.LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    protected Account() {
    }

    public static Account create(String username, String passwordHash, AccountRole role, boolean forcePasswordChange) {
        Account account = new Account();
        account.username = username;
        account.passwordHash = passwordHash;
        account.role = role;
        account.status = AccountStatus.ACTIVE;
        account.forcePasswordChange = forcePasswordChange;
        account.loginFailCount = 0;
        return account;
    }

    public void markLoginSuccess() {
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void markLoginFailed(int maxFailures, int lockMinutes) {
        this.loginFailCount += 1;
        if (this.loginFailCount >= maxFailures) {
            this.status = AccountStatus.LOCKED;
            this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.forcePasswordChange = false;
        this.passwordChangedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.status = AccountStatus.ACTIVE;
    }

    public String passwordVersion() {
        return this.passwordChangedAt == null ? "" : this.passwordChangedAt.toString();
    }

    public boolean isLockedNow() {
        return this.status == AccountStatus.LOCKED
                && this.lockedUntil != null
                && this.lockedUntil.isAfter(LocalDateTime.now());
    }

    public void unlockIfExpired() {
        if (this.status == AccountStatus.LOCKED && this.lockedUntil != null && !this.lockedUntil.isAfter(LocalDateTime.now())) {
            this.status = AccountStatus.ACTIVE;
            this.lockedUntil = null;
            this.loginFailCount = 0;
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AccountRole getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isForcePasswordChange() {
        return forcePasswordChange;
    }

    public int getLoginFailCount() {
        return loginFailCount;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }
}
