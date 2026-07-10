package cn.schoolpsych.appointment.admin.management;

import java.util.List;

import cn.schoolpsych.appointment.domain.counselor.Counselor;

public record CounselorResponse(
        Long id,
        Long accountId,
        String username,
        String name,
        String avatarUrl,
        String title,
        String gender,
        Long campusId,
        String campusName,
        List<String> expertise,
        String intro,
        String trainingBackground,
        List<String> serviceModes,
        int maxDailyCount,
        boolean visible,
        String status) {
    static CounselorResponse from(
            Counselor counselor,
            String username,
            String campusName,
            List<String> expertise,
            List<String> serviceModes) {
        return new CounselorResponse(
                counselor.getId(),
                counselor.getAccountId(),
                username,
                counselor.getName(),
                counselor.getAvatarUrl(),
                counselor.getTitle(),
                counselor.getGender(),
                counselor.getCampusId(),
                campusName,
                expertise,
                counselor.getIntro(),
                counselor.getTrainingBackground(),
                serviceModes,
                counselor.getMaxDailyCount(),
                counselor.isVisible(),
                counselor.getStatus());
    }
}
