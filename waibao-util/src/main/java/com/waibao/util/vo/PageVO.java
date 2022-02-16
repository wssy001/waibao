package com.waibao.util.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * PageVO
 *
 * @author alexpetertyler
 * @since 2022-01-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private long index = 1;
    private long count = 20;
    private long maxIndex;
    private long maxSize;
    private List<T> list;
}
