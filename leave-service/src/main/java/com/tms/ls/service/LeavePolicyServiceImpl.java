package com.tms.ls.service;

import com.tms.common.exception.ResourceNotFoundException;
import com.tms.ls.dto.LeavePolicyDto;
import com.tms.ls.entity.LeavePolicy;
import com.tms.ls.entity.LeaveType;
import com.tms.ls.repository.LeavePolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeavePolicyServiceImpl implements LeavePolicyService {

    private final LeavePolicyRepository repository;

    public LeavePolicyServiceImpl(LeavePolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeavePolicy> getAllPolicies() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public LeavePolicy getPolicyByType(LeaveType type) {
        return repository.findByLeaveType(type)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found for type: " + type));
    }

    @Override
    @Transactional
    public LeavePolicy createOrUpdatePolicy(LeavePolicyDto dto) {
        LeavePolicy policy = repository.findByLeaveType(dto.getLeaveType())
                .orElse(new LeavePolicy());
        
        policy.setLeaveType(dto.getLeaveType());
        policy.setDaysAllowed(dto.getDaysAllowed());
        policy.setCarryForwardAllowed(dto.isCarryForwardAllowed());
        policy.setMaxCarryForwardDays(dto.getMaxCarryForwardDays());
        policy.setRequiresDelegate(dto.isRequiresDelegate());
        if (dto.getMinDaysNotice() != null) {
            policy.setMinDaysNotice(dto.getMinDaysNotice());
        }
        policy.setActive(dto.isActive());
        
        return repository.save(policy);
    }

    @Override
    @Transactional
    public void deletePolicy(String id) {
        LeavePolicy policy = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        repository.delete(policy);
    }
}
