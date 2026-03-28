package com.sintao.judge.callback;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Statistics;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;

@Getter
public class StatisticsCallback implements ResultCallback<Statistics> {

    private Long maxMemory = 0L;

    @Override
    public void onStart(Closeable closeable) {
        // No-op.
    }

    @Override
    public void onNext(Statistics statistics) {
        if (statistics == null || statistics.getMemoryStats() == null) {
            return;
        }
        Long usage = statistics.getMemoryStats().getMaxUsage();
        if (usage != null) {
            maxMemory = Math.max(usage, maxMemory);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        // No-op.
    }

    @Override
    public void onComplete() {
        // No-op.
    }

    @Override
    public void close() throws IOException {
        // No-op.
    }
}
