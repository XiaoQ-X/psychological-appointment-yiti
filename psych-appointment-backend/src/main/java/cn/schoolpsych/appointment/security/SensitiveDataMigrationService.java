package cn.schoolpsych.appointment.security;

import java.util.ArrayList;
import java.util.List;

import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentFormMetadata;
import cn.schoolpsych.appointment.domain.appointment.RiskAssessment;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewMetadata;
import cn.schoolpsych.appointment.domain.appointment.RiskScreeningAnswers;
import cn.schoolpsych.appointment.domain.referral.Referral;
import cn.schoolpsych.appointment.repository.AppointmentFormRepository;
import cn.schoolpsych.appointment.repository.AppointmentRepository;
import cn.schoolpsych.appointment.repository.ReferralRepository;
import cn.schoolpsych.appointment.repository.RiskAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveDataMigrationService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentFormRepository formRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ReferralRepository referralRepository;
    private final AppointmentSensitiveDataService sensitiveData;

    public SensitiveDataMigrationService(
            AppointmentRepository appointmentRepository,
            AppointmentFormRepository formRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            ReferralRepository referralRepository,
            AppointmentSensitiveDataService sensitiveData) {
        this.appointmentRepository = appointmentRepository;
        this.formRepository = formRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.referralRepository = referralRepository;
        this.sensitiveData = sensitiveData;
    }

    @Transactional
    public MigrationResult migrateLegacyPlaintext() {
        List<Appointment> appointments = new ArrayList<>();
        for (Appointment appointment : appointmentRepository.findAll()) {
            if (appointment.getCancelReasonEncrypted() == null
                    && appointment.getLegacyCancelReason() != null
                    && !appointment.getLegacyCancelReason().isBlank()) {
                appointment.migrateCancelReason(sensitiveData.encryptText(appointment.getLegacyCancelReason()));
                appointments.add(appointment);
            }
        }

        List<AppointmentForm> forms = new ArrayList<>();
        for (AppointmentForm form : formRepository.findAll()) {
            if (form.getMetadataEncrypted() == null) {
                AppointmentFormMetadata metadata = sensitiveData.readFormMetadata(form);
                form.migrateMetadata(sensitiveData.encryptFormMetadata(
                        metadata.issueTypes(), metadata.urgencyLevel(), metadata.contactTime()));
                forms.add(form);
            }
        }

        List<RiskAssessment> risks = new ArrayList<>();
        for (RiskAssessment risk : riskAssessmentRepository.findAll()) {
            boolean changed = false;
            if (risk.getAnswersEncrypted() == null) {
                RiskScreeningAnswers answers = sensitiveData.readRiskAnswers(risk);
                risk.migrateAnswers(sensitiveData.encryptRiskAnswers(answers));
                changed = true;
            }
            if (risk.getReviewMetadataEncrypted() == null
                    && (risk.getReviewedBy() != null || risk.getReviewedAt() != null)) {
                RiskReviewMetadata metadata = sensitiveData.readReviewMetadata(risk);
                risk.migrateReviewMetadata(sensitiveData.encryptReviewMetadata(
                        metadata.reviewedBy(), metadata.reviewedAt()));
                changed = true;
            }
            if (changed) {
                risks.add(risk);
            }
        }

        List<Referral> referrals = new ArrayList<>();
        for (Referral referral : referralRepository.findAll()) {
            if (referral.getDestinationEncrypted() == null
                    && referral.getLegacyDestination() != null
                    && !referral.getLegacyDestination().isBlank()) {
                referral.migrateDestination(sensitiveData.encryptText(referral.getLegacyDestination()));
                referrals.add(referral);
            }
        }

        appointmentRepository.saveAll(appointments);
        formRepository.saveAll(forms);
        riskAssessmentRepository.saveAll(risks);
        referralRepository.saveAll(referrals);
        return new MigrationResult(appointments.size(), forms.size(), risks.size(), referrals.size());
    }

    public record MigrationResult(int appointments, int forms, int risks, int referrals) {

        public int total() {
            return appointments + forms + risks + referrals;
        }
    }
}
