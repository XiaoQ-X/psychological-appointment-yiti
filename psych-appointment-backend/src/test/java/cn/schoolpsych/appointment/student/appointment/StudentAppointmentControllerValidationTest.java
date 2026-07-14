package cn.schoolpsych.appointment.student.appointment;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.schoolpsych.appointment.common.exception.GlobalExceptionHandler;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StudentAppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudentAppointmentControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentAppointmentService studentAppointmentService;

    @MockBean
    private StudentAppointmentQueryService studentAppointmentQueryService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private AccountRepository accountRepository;

    @Test
    void rejectsUnknownUrgencyAsBadRequestBeforeServiceInvocation() throws Exception {
        mockMvc.perform(post("/api/student/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slotId": 1,
                                  "firstVisit": true,
                                  "issueTypes": ["academic-stress"],
                                  "description": "Need support",
                                  "urgencyLevel": "URGENT",
                                  "consentVersionId": 1,
                                  "consentAgreed": true,
                                  "risk": {
                                    "selfHarm": false,
                                    "harmOthers": false,
                                    "crisisEvent": false,
                                    "psychiatricTreatment": false,
                                    "medication": false,
                                    "willingContact": true
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(studentAppointmentService);
    }
}
