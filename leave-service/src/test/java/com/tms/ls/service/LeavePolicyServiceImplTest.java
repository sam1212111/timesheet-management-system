package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeavePolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeavePolicyServiceImplTest {

    @Mock
    private LeavePolicyRepository repository;

    @InjectMocks
    private LeavePolicyServiceImpl service;

    @Test
    @DisplayName("getAllPolicies should return repository data")
    void getAllPolicies_ReturnsPolicies() {
        LeavePolicy policy = new LeavePolicy();
        policy.setLeaveType(LeaveType.CASUAL);
        when(repository.findAll()).thenReturn(List.of(policy));

        List<LeavePolicy> result = service.getAllPolicies();

        assertEquals(1, result.size());
        assertEquals(LeaveType.CASUAL, result.get(0).getLeaveType());
    }

    @Test
    @DisplayName("getPolicyByType should throw when policy is missing")
    void getPolicyByType_ThrowsWhenMissing() {
        when(repository.findByLeaveType(LeaveType.SICK)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getPolicyByType(LeaveType.SICK));
    }

    @Test
    @DisplayName("createOrUpdatePolicy should persist dto values")
    void createOrUpdatePolicy_SavesMappedPolicy() {
        LeavePolicyDto dto = new LeavePolicyDto();
        dto.setLeaveType(LeaveType.CASUAL);
        dto.setDaysAllowed(new BigDecimal("12"));
        dto.setCarryForwardAllowed(true);
        dto.setMaxCarryForwardDays(new BigDecimal("5"));
        dto.setRequiresDelegate(false);
        dto.setMinDaysNotice(2);
        dto.setActive(true);

        when(repository.findByLeaveType(LeaveType.CASUAL)).thenReturn(Optional.empty());
        when(repository.save(any(LeavePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeavePolicy saved = service.createOrUpdatePolicy(dto);

        assertEquals(LeaveType.CASUAL, saved.getLeaveType());
        assertEquals(new BigDecimal("12"), saved.getDaysAllowed());
        assertEquals(2, saved.getMinDaysNotice());
    }

    @Test
    @DisplayName("deletePolicy should remove existing policy")
    void deletePolicy_DeletesEntity() {
        LeavePolicy policy = new LeavePolicy();
        policy.setId("POL-1");
        when(repository.findById("POL-1")).thenReturn(Optional.of(policy));

        service.deletePolicy("POL-1");

        verify(repository).delete(policy);
    }
}
