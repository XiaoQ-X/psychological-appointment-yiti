package cn.schoolpsych.appointment.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

@Component
public class SensitiveDataMigrationRunner implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataMigrationRunner.class);

    private final AppointmentSensitiveDataService sensitiveData;
    private final SensitiveDataMigrationService migrationService;

    public SensitiveDataMigrationRunner(
            AppointmentSensitiveDataService sensitiveData,
            SensitiveDataMigrationService migrationService) {
        this.sensitiveData = sensitiveData;
        this.migrationService = migrationService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!sensitiveData.isConfigured()) {
            log.warn("Sensitive data migration skipped because SENSITIVE_DATA_KEY_BASE64 is not configured");
            return;
        }
        SensitiveDataMigrationService.MigrationResult result = migrationService.migrateLegacyPlaintext();
        if (result.total() > 0) {
            log.info(
                    "Encrypted legacy appointment data: appointments={}, forms={}, risks={}, referrals={}",
                    result.appointments(), result.forms(), result.risks(), result.referrals());
        }
    }
}
