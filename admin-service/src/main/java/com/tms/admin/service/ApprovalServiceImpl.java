package com.tms.admin.service;

import com.tms.admin.dto.ApprovalCompletedEvent;
import com.tms.admin.dto.ApprovalTaskResponse;
import com.tms.admin.entity.ApprovalStatus;
import com.tms.admin.entity.ApprovalTask;
import com.tms.admin.repository.ApprovalTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalTaskRepository taskRepository;
    private final ApprovalCompletionEventPublisher approvalCompletionEventPublisher;

    public ApprovalServiceImpl(ApprovalTaskRepository taskRepository,
                               ApprovalCompletionEventPublisher approvalCompletionEventPublisher) {
        this.taskRepository = taskRepository;
        this.approvalCompletionEventPublisher = approvalCompletionEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalTaskResponse> getPendingApprovals(String approverId) {
        return taskRepository.findByApproverIdAndStatusOrderByCreatedAtDesc(approverId, ApprovalStatus.PENDING)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public ApprovalTaskResponse approveTask(String taskId, String comments, String approverId) {
        return processTask(taskId, comments, approverId, ApprovalStatus.APPROVED);
    }

    @Override
    @Transactional
    public ApprovalTaskResponse rejectTask(String taskId, String comments, String approverId) {
        return processTask(taskId, comments, approverId, ApprovalStatus.REJECTED);
    }

    private ApprovalTaskResponse processTask(String taskId, String comments, String approverId, ApprovalStatus newStatus) {
        ApprovalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Approval Task not found with id: " + taskId));

        if (!task.getApproverId().equals(approverId)) {
            throw new IllegalArgumentException("You are not authorized to approve this task");
        }

        if (task.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("Task is already processed. Current status: " + task.getStatus());
        }

        task.setStatus(newStatus);
        task.setComments(comments);

        task = taskRepository.save(task);

        ApprovalCompletedEvent event = new ApprovalCompletedEvent(
                task.getTargetType().name(),
                task.getTargetId(),
                task.getEmployeeId(),
                newStatus.name(),
                comments
        );
        approvalCompletionEventPublisher.publishApprovalCompletedEvent(event);

        return mapToResponse(task);
    }

    private ApprovalTaskResponse mapToResponse(ApprovalTask task) {
        ApprovalTaskResponse resp = new ApprovalTaskResponse();
        resp.setId(task.getId());
        resp.setTargetType(task.getTargetType());
        resp.setTargetId(task.getTargetId());
        resp.setEmployeeId(task.getEmployeeId());
        resp.setApproverId(task.getApproverId());
        resp.setStatus(task.getStatus());
        resp.setComments(task.getComments());
        resp.setCreatedAt(task.getCreatedAt());
        resp.setUpdatedAt(task.getUpdatedAt());
        return resp;
    }
}
