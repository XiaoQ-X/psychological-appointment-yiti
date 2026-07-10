package cn.schoolpsych.appointment.admin.management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(
        @NotNull Long campusId,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 255) String locationDesc,
        @Size(max = 32) String status) {
}
