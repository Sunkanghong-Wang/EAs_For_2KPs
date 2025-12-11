package com.wskh.utils;
import java.util.List;

public class DffUtil {

    public static int dff(int dffType, int k, int C, int x, List<Integer> xs) {
        return switch (dffType) {
            case 0 -> dff1(k, C, x);
            case 1 -> dff2(k, C, x, xs);
            case 2 -> dff3(k, C, x);
            default -> throw new RuntimeException();
        };
    }

    public static int dff(int dffType, int k, int C, int x, int[] xs) {
        return switch (dffType) {
            case 0 -> dff1(k, C, x);
            case 1 -> dff2(k, C, x, xs);
            case 2 -> dff3(k, C, x);
            default -> throw new RuntimeException();
        };
    }

    public static int dff0(int k, int C, int x) {
        double r = (k + 1) * x / (double) C;
        if (CommonUtil.isInteger(r)) {
            return x;
        } else {
            return (int) r * C / k;
        }
    }

    public static int dff1(int k, int C, int x) {
        int r = C - k;
        if (x > r) {
            return C;
        } else if (x >= k) {
            return x;
        } else {
            return 0;
        }
    }

    public static int dff2(int k, int C, int x, List<Integer> xs) {
        int r = C / 2;
        if (x > r) {
            return CommonUtil.solve1dCkp(C, xs) - CommonUtil.solve1dCkp(C - x, xs);
        } else if (x >= k) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int dff2(int k, int C, int x, int[] xs) {
        int r = C / 2;
        if (x > r) {
            return CommonUtil.solve1dCkp(C, xs) - CommonUtil.solve1dCkp(C - x, xs);
        } else if (x >= k) {
            return 1;
        } else {
            return 0;
        }
    }

    public static int dff3(int k, int C, int x) {
        int r = C / 2;
        if (x > r) {
            return 2 * (C / k - (C - x) / k);
        } else if (x == r) {
            return C / k;
        } else {
            return 2 * (x / k);
        }
    }

}