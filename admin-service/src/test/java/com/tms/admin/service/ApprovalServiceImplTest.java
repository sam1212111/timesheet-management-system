package com.tms.admin.service;

import com.tms.admin.dto.ApprovalTaskResponse;
import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.ApprovalTask;
import com.tms.admin.entity.TargetType;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceImplTest {

    @Mock
    private ApprovalTaskRepository taskRepository;

    @Mock
    private ApprovalCompletionEventPublisher approvalCompletionEventPublisher;

    @InjectMocks
    private ApprovalServiceImpl service;

    @Test
    @DisplayName("getPendingApprovals should return mapped tasks")
    void getPendingApprovals_ReturnsMappedTasks() {
        ApprovalTask task = buildTask();
        when(taskRepository.findByApproverIdAndStatusOrderByCreatedAtDesc("MGR-1", ApprovalStatus.PENDING))
                .thenReturn(List.of(task));

        List<ApprovalTaskResponse> result = service.getPendingApprovals("MGR-1");

        assertEquals(1, result.size());
        assertEquals("TASK-1", result.get(0).getId());
    }

    @Test
    @DisplayName("approveTask should persist status change and publish event")
    void approveTask_UpdatesTaskAndPublishes() {
        ApprovalTask task = buildTask();
        when(taskRepository.findById("TASK-1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(ApprovalTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalTaskResponse response = service.approveTask("TASK-1", "Looks good", "MGR-1");

        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertEquals("Looks good", response.getComments());
        verify(approvalCompletionEventPublisher).publishApprovalCompletedEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("rejectTask should reject unauthorized approver")
    void rejectTask_RejectsUnauthorizedApprover() {
        ApprovalTask task = buildTask();
        when(taskRepository.findById("TASK-1")).thenReturn(Optional.of(task));

        assertThrows(IllegalArgumentException.class, () -> service.rejectTask("TASK-1", "No", "OTHER"));
    }

    private ApprovalTask buildTask() {
        ApprovalTask task = new ApprovalTask();
        task.setId("TASK-1");
        task.setApproverId("MGR-1");
        task.setEmployeeId("EMP-1");
        task.setTargetId("LEAVE-1");
        task.setTargetType(TargetType.LEAVE);
        task.setStatus(ApprovalStatus.PENDING);
        return task;
    }
}
