package com.tms.ls.service;

import com.tms.ls.dto.HolidayRequest;
import com.tms.ls.dto.HolidayResponse;
import com.tms.ls.entity.Holiday;
import com.tms.ls.entity.HolidayType;
import com.tms.ls.repository.HolidayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceImplTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayServiceImpl service;

    @Test
    @DisplayName("getAllHolidays should return mapped holidays")
    void getAllHolidays_ReturnsMappedResponses() {
        Holiday holiday = new Holiday(LocalDate.of(2026, 1, 26), "Republic Day", HolidayType.MANDATORY);
        holiday.setId("HOL-1");
        when(holidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of(holiday));

        List<HolidayResponse> result = service.getAllHolidays();

        assertEquals(1, result.size());
        assertEquals("HOL-1", result.get(0).getId());
        assertEquals("Republic Day", result.get(0).getName());
    }

    @Test
    @DisplayName("addHoliday should reject duplicate dates")
    void addHoliday_RejectsDuplicateDate() {
        HolidayRequest request = new HolidayRequest();
        request.setDate(LocalDate.of(2026, 8, 15));

        when(holidayRepository.findByDate(request.getDate())).thenReturn(Optional.of(new Holiday()));

        assertThrows(IllegalArgumentException.class, () -> service.addHoliday(request));
    }

    @Test
    @DisplayName("deleteHoliday should delegate to repository")
    void deleteHoliday_DelegatesToRepository() {
        service.deleteHoliday("HOL-3");

        verify(holidayRepository).deleteById("HOL-3");
    }

    @Test
    @DisplayName("addHoliday should save and map a new holiday")
    void addHoliday_SavesHoliday() {
        HolidayRequest request = new HolidayRequest();
        request.setDate(LocalDate.of(2026, 12, 25));
        request.setName("Christmas");
        request.setType(HolidayType.MANDATORY);

        when(holidayRepository.findByDate(request.getDate())).thenReturn(Optional.empty());
        when(holidayRepository.save(any(Holiday.class))).thenAnswer(invocation -> {
            Holiday holiday = invocation.getArgument(0);
            holiday.setId("HOL-4");
            return holiday;
        });

        HolidayResponse response = service.addHoliday(request);

        assertEquals("HOL-4", response.getId());
        assertEquals("Christmas", response.getName());
    }
}
