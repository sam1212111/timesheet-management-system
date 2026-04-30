package com.tms.admin.service;

import com.tms.admin.dto.ApprovalDetailResponse;
import com.tms.admin.dto.ApprovalTaskResponse;
import java.util.List;

public interface ApprovalService {
    List<ApprovalTaskResponse> getPendingApprovals(String approverId);
    ApprovalDetailResponse getApprovalDetail(String taskId, String approverId, String authorization);
    ApprovalTaskResponse approveTask(String taskId, String comments, String approverId);
    ApprovalTaskResponse rejectTask(String taskId, String comments, String approverId);
}
