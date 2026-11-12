package com.example.secondhand_project.admin.dto;

import lombok.Data;

@Data
public class AuditActionDTO {

    /** approve / reject */
    private String action;

    private String reason;
}
