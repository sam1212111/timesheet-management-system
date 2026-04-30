package com.tms.admin.dto;

public class ApprovalDetailResponse {

    private ApprovalTaskResponse task;
    private Object targetDetail;

    public ApprovalTaskResponse getTask() {
        return task;
    }

    public void setTask(ApprovalTaskResponse task) {
        this.task = task;
    }

    public Object getTargetDetail() {
        return targetDetail;
    }

    public void setTargetDetail(Object targetDetail) {
        this.targetDetail = targetDetail;
    }
}
