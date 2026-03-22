package com.sintao.judge.callback;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Statistics;
import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;
import java.io.IOException;

@Getter
@Setter
public class StatisticsCallback implements ResultCallback<Statistics> {

    private Long maxMemory = 0L;

    @Override
    //这个方法在开始接收统计信息时会被调用
    public void onStart(Closeable closeable) {

    }

//    这个方法在每次接收到新的统计信息时被调用�?//    它从 Statistics 对象中提取当前的最大内存使用量，并与现有的 maxMemory 值比较，
//    保留较大的那个值。这样可以跟踪整个监控期间的最大内存使用情况�?    @Override
    public void onNext(Statistics statistics) {
        Long usage = statistics.getMemoryStats().getMaxUsage();//程序运行到某个时间点上的内存使用的最大�?        if (usage != null) {
            maxMemory = Math.max(usage, maxMemory);
        }
    }

    //这个方法在获取统计信息过程中遇到错误时会被调�?    @Override
    public void onError(Throwable throwable) {

    }

    //这个方法在所有的统计信息都被接收完毕后调�?    @Override
    public void onComplete() {

    }

    //这个方法用于清理资源
    @Override
    public void close() throws IOException {

    }
}

