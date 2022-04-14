package com.waibao.util.tools;

import com.alibaba.fastjson.serializer.ValueFilter;

import java.math.BigDecimal;

/**
 * BigDecimalValueFilter
 *
 * @author alexpetertyler
 * @since 2022/4/10
 */
public class BigDecimalValueFilter implements ValueFilter {

    public Object process(Object object, String name, Object value) {

        if (value instanceof BigDecimal) return String.format("%.2f", ((BigDecimal) value).doubleValue());
        return value;
    }

}
