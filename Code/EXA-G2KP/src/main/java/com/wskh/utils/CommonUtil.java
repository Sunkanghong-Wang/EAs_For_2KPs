package com.wskh.utils;

import com.wskh.classes.Parameter;
import com.wskh.classes.PlaceItem;
import java.util.List;

public class CommonUtil {

    public static int floorToInt(double x) {
        return (int) Math.floor(x + Parameter.EPS);
    }

    public static int ceilToInt(double x) {
        return (int) Math.ceil(x - Parameter.EPS);
    }

    public static long ceilToLong(double x) {
        return (long) Math.ceil(x - Parameter.EPS);
    }

    public static boolean isInteger(double x) {
        // 计算差值并与 EPS 比较
        return Math.abs(x - Math.round(x)) < Parameter.EPS;
    }

    public static int compareDouble(double a, double b) {
        double diff = a - b;
        if (diff > Parameter.EPS) return 1;
        if (diff < -Parameter.EPS) return -1;
        return 0;
    }

    public static boolean isOverlap(PlaceItem r1, PlaceItem r2) {
        return !(r2.x >= r1.x + r1.w || // r2 在 r1 的右边
                r2.x + r2.w <= r1.x || // r2 在 r1 的左边
                r2.y >= r1.y + r1.h || // r2 在 r1 的上方
                r2.y + r2.h <= r1.y);  // r2 在 r1 的下方
    }

    public static int solve1dCkp(int C, int[] xs) {
        // 要确保 xs 升序排序
        int res = 0;
        for (int x : xs) {
            C -= x;
            if (C >= 0) {
                res++;
            } else {
                break;
            }
        }
        return res;
    }

    public static int solve1dCkp(int C, List<Integer> xs) {
        // 要确保 xs 升序排序
        int res = 0;
        for (int x : xs) {
            C -= x;
            if (C >= 0) {
                res++;
            } else {
                break;
            }
        }
        return res;
    }

    public static void addPosition(int p, boolean[] positionUsedArr, List<Integer> positions) {
        if (!positionUsedArr[p]) {
            positionUsedArr[p] = true;
            positions.add(p);
        }
    }

}