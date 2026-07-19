package com.alahly.momkn.finthos.admin.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
@AllArgsConstructor
public class AdminUserPage {

    private java.util.List<AdminUserResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
