package com.example.sanitationassessment.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;
}
