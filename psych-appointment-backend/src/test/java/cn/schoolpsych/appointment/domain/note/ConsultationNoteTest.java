package cn.schoolpsych.appointment.domain.note;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConsultationNoteTest {

    @Test
    void submitCreatesSubmittedNote() {
        ConsultationNote note = ConsultationNote.submit(
                1L,
                2L,
                3L,
                new byte[]{1},
                new byte[]{2},
                "STABLE",
                new byte[]{3},
                true);

        assertThat(note.getAppointmentId()).isEqualTo(1L);
        assertThat(note.getStudentId()).isEqualTo(2L);
        assertThat(note.getCounselorId()).isEqualTo(3L);
        assertThat(note.getRiskChange()).isEqualTo("STABLE");
        assertThat(note.isNeedReferral()).isTrue();
        assertThat(note.getStatus()).isEqualTo(NoteStatus.SUBMITTED);
    }
}
