package com.tms.ls.service;

import com.tms.ls.dto.HolidayRequest;
import com.tms.ls.dto.HolidayResponse;
import com.tms.ls.entity.Holiday;
import com.tms.ls.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HolidayServiceImpl implements HolidayService {
    
    private final HolidayRepository holidayRepository;

    public HolidayServiceImpl(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> getAllHolidays() {
        return holidayRepository.findAllByOrderByDateAsc()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HolidayResponse addHoliday(HolidayRequest request) {
        if (holidayRepository.findByDate(request.getDate()).isPresent()) {
            throw new IllegalArgumentException("A holiday already exists for this date");
        }
        Holiday holiday = new Holiday(request.getDate(), request.getName(), request.getType());
        holiday = holidayRepository.save(holiday);
        return mapToResponse(holiday);
    }

    @Override
    @Transactional
    public void deleteHoliday(String id) {
        holidayRepository.deleteById(id);
    }

    private HolidayResponse mapToResponse(Holiday entity) {
        HolidayResponse resp = new HolidayResponse();
        resp.setId(entity.getId());
        resp.setDate(entity.getDate());
        resp.setName(entity.getName());
        resp.setType(entity.getType());
        return resp;
    }
}
