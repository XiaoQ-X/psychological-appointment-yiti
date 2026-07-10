package cn.schoolpsych.appointment.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import cn.schoolpsych.appointment.domain.schedule.CounselorScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounselorScheduleTemplateRepository extends JpaRepository<CounselorScheduleTemplate, Long> {
    List<CounselorScheduleTemplate> findByStatusOrderByIdAsc(String status);

    List<CounselorScheduleTemplate> findByCounselorIdAndStatusOrderByIdAsc(Long counselorId, String status);

    @Query("""
            select template from CounselorScheduleTemplate template
            where template.status = 'ACTIVE'
              and template.dayOfWeek in :dayOfWeeks
              and template.effectiveFrom <= :endDate
              and (template.effectiveTo is null or template.effectiveTo >= :startDate)
              and (:counselorId is null or template.counselorId = :counselorId)
            order by template.counselorId asc, template.dayOfWeek asc, template.startTime asc
            """)
    List<CounselorScheduleTemplate> findActiveForGeneration(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("dayOfWeeks") Collection<Byte> dayOfWeeks,
            @Param("counselorId") Long counselorId);
}
