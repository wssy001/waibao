package com.waibao.util.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RedisCommand
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisCommand {
    private String command;
    private Object value;
    private Long timestamp;
}
