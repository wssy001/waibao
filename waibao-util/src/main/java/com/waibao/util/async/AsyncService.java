package com.waibao.util.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

/**
 * AsyncService
 *
 * @author alexpetertyler
 * @since 2022/3/3
 */
@Async
@Service
public class AsyncService {

    public void basicTask(Runnable runnable) {
        runnable.run();
    }

    public <V> Future<V> basicTask(V method) {
        return new AsyncResult<>(method);
    }
}
