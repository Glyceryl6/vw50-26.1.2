package com.sqzj.vw50.client;

import java.util.ArrayDeque;
import java.util.Deque;

public class ClientRepeatLimiter {

    private static final int MAX_PER_MINUTE = 6;
    private static final int MIN_INTERVAL_MS = 1200;
    private static final Deque<Long> SEND_TIMES = new ArrayDeque<>();
    private static long lastSendTime;

    public static boolean tryAcquire() {
        long now = System.currentTimeMillis();
        while (!SEND_TIMES.isEmpty() && now - SEND_TIMES.peekFirst() > 60_000L) {
            SEND_TIMES.removeFirst();
        }

        if (now - lastSendTime < MIN_INTERVAL_MS) return false;
        if (SEND_TIMES.size() >= MAX_PER_MINUTE) return false;
        SEND_TIMES.addLast(now);
        lastSendTime = now;
        return true;
    }

}