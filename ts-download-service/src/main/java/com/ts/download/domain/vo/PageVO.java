package com.ts.download.domain.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> records;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public static <T> PageVO<T> of(List<T> records, Long total, Integer page, Integer pageSize) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        return vo;
    }
}
