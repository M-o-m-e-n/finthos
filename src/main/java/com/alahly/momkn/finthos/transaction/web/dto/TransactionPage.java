package com.alahly.momkn.finthos.transaction.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TransactionPage {

    private List<TransactionItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
