package com.theos.detectivehelper.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器 - 使用雪花算法简化版本
 */
public class IdGenerator {

    private static final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());

    private static final long TIMESTAMP_BITS = 41L;
    private static final long SEQUENCE_BITS = 10L;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1L;

    /**
     * 生成唯一ID
     */
    public static Long generateId() {
        long currentTime = System.currentTimeMillis();
        long currentSeq = sequence.getAndIncrement() & MAX_SEQUENCE;

        return (currentTime << SEQUENCE_BITS) | currentSeq;
    }

    /**
     * 从ID中提取时间戳
     */
    public static long extractTimestamp(long id) {
        return id >> SEQUENCE_BITS;
    }

}