package com.tms.ls.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tms.ls.dto.HolidayRequest;
import com.tms.ls.dto.HolidayResponse;
import com.tms.ls.entity.HolidayType;
import com.tms.ls.service.HolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HolidayControllerTest {

    @Mock
    private HolidayService holidayService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(new HolidayController(holidayService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /holidays should return all holidays")
    void getAllHolidays_Success() throws Exception {
        HolidayResponse holiday = new HolidayResponse();
        holiday.setId("HOL-001");
        holiday.setDate(LocalDate.of(2026, 1, 26));
        holiday.setName("Republic Day");
        holiday.setType(HolidayType.MANDATORY);

        when(holidayService.getAllHolidays()).thenReturn(List.of(holiday));

        mockMvc.perform(get("/api/v1/leave/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("HOL-001"))
                .andExpect(jsonPath("$[0].name").value("Republic Day"));
    }

    @Test
    @DisplayName("POST /holidays should create a holiday")
    void addHoliday_Success() throws Exception {
        HolidayRequest request = new HolidayRequest();
        request.setDate(LocalDate.of(2026, 8, 15));
        request.setName("Independence Day");
        request.setType(HolidayType.MANDATORY);

        HolidayResponse response = new HolidayResponse();
        response.setId("HOL-002");
        response.setDate(request.getDate());
        response.setName(request.getName());
        response.setType(request.getType());

        when(holidayService.addHoliday(any(HolidayRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/leave/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("HOL-002"))
                .andExpect(jsonPath("$.type").value("MANDATORY"));
    }

    @Test
    @DisplayName("DELETE /holidays/{id} should delete a holiday")
    void deleteHoliday_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/leave/holidays/HOL-003"))
                .andExpect(status().isNoContent());

        verify(holidayService).deleteHoliday("HOL-003");
    }
}
