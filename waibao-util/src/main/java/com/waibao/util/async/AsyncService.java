package com.waibao.util.async;

import lombok.SneakyThrows;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * AsyncService
 *
 * @author alexpetertyler
 * @since 2022/3/3
 */
@Service
public class AsyncService {

    @Async
    public void basicTask(Runnable runnable) {
        runnable.run();
    }

    @Async
    @SneakyThrows
    public <T> void basicTask(Callable<T> callable) {
        callable.call();
    }

    @Async
    public <V> Future<V> basicTask(V method) {
        return new AsyncResult<>(method);
    }
}
