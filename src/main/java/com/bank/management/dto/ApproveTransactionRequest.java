package com.bank.management.dto;

public class ApproveTransactionRequest {
    private String managerComment;

    public ApproveTransactionRequest() {
    }

    public ApproveTransactionRequest(String managerComment) {
        this.managerComment = managerComment;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }
}