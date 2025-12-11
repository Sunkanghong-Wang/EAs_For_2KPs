package com.wskh.utils;

public class TimeUtil {
    public static long TimeLimit;
    public static long startTime;

    public static long getCurTime() {
        return System.currentTimeMillis() - startTime;
    }

    public static boolean isTimeLimit() {
        return System.currentTimeMillis() - startTime >= TimeLimit;
    }

    public static long getRemainingTime() {
        return Math.max(0, TimeLimit - System.currentTimeMillis() + startTime);
    }
}
