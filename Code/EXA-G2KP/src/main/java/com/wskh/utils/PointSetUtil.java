package com.wskh.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PointSetUtil {

    public static List<Integer> normalPatterns2(boolean[] t, int[] ws, int W, int excludeIndex, int minWIndex) {
        if (W < 0) return new ArrayList<>();
        t[0] = true;
        for (int i = 0; i < ws.length; i++) {
            if (i != excludeIndex && i != minWIndex) {
                int wi = ws[i];
                for (int p = W - wi; p >= 0; p--) {
                    if (t[p]) {
                        t[p + wi] = true;
                    }
                }
            }
        }
        List<Integer> list = new ArrayList<>(t.length);
        for (int i = 0; i < t.length; i++) if (t[i]) list.add(i);
        return list;
    }

    public static List<Integer> normalPatterns(int[] ws, int W, int excludeIndex) {
        if (W < 0) return new ArrayList<>();
        boolean[] t = new boolean[W + 1];
        t[0] = true;
        for (int i = 0; i < ws.length; i++) {
            if (i != excludeIndex) {
                int wi = ws[i];
                for (int p = W - wi; p >= 0; p--) {
                    if (t[p]) {
                        t[p + wi] = true;
                    }
                }
            }
        }
        List<Integer> list = new ArrayList<>(t.length);
        for (int i = 0; i < t.length; i++) if (t[i]) list.add(i);
        return list;
    }

    public static List<Integer>[] MIM_Pro(int W, int[] ws, List<Integer> xList, int minWIndex) {
        int n = ws.length;
        // 计算 tMin （包含预处理1）
        int obj = Integer.MAX_VALUE;
        List<Integer>[] xListList = new List[n];
        boolean[][] leftSelectedArray = new boolean[n][W + 1];
        boolean[][] rightSelectedArray = new boolean[n][W + 1];
        for (int t = 1; t <= W; t++) {
            boolean leftRemove = t <= CommonUtil.ceilToInt((W - ws[minWIndex]) / 2d);
            int temp_obj = 0;
            List<Integer>[] temp_xListList = new List[n];
            boolean[][] temp_leftSelectedArray = new boolean[n][W + 1];
            boolean[][] temp_rightSelectedArray = new boolean[n][W + 1];
            for (int i = 0; i < n; i++) {
                List<Integer> i_list, rightList;
                if (leftRemove) {
                    i_list = normalPatterns2(temp_leftSelectedArray[i], ws, Math.min(W - ws[i], t - 1), i, minWIndex);
                    rightList = normalPatterns(ws, W - ws[i] - t, i);
                } else {
                    i_list = normalPatterns2(temp_leftSelectedArray[i], ws, Math.min(W - ws[i], t - 1), i, -1);
                    rightList = normalPatterns2(new boolean[W + 1], ws, W - ws[i] - t, i, minWIndex);
                }
                for (int p : rightList) {
                    int rightP = W - ws[i] - p;
                    temp_rightSelectedArray[i][rightP] = true;
                    if (!temp_leftSelectedArray[i][rightP]) i_list.add(rightP);
                }
                temp_xListList[i] = i_list;
                temp_obj += i_list.size();
            }
            if (temp_obj < obj) {
                obj = temp_obj;
                leftSelectedArray = temp_leftSelectedArray;
                rightSelectedArray = temp_rightSelectedArray;
                xListList = temp_xListList;
            }
        }

        for (List<Integer> i_list : xListList) Collections.sort(i_list);

        // 执行预处理2
        int[][] wImproved = new int[n][W + 1];
        for (int i = 0; i < n; i++) Arrays.fill(wImproved[i], ws[i]);

        for (int k = 0; k < n; k++) {
            List<Integer> xListK = xListList[k];
            for (int p : xListK) {
                if (leftSelectedArray[k][p]) {
                    int q = W;
                    for (int i = 0; i < n; i++) {
                        if (i != k) {
                            List<Integer> list = xListList[i];
                            for (int s : list) {
                                if (s >= p + wImproved[k][p]) {
                                    q = Math.min(q, s);
                                }
                            }
                        }
                    }
                    wImproved[k][p] = q - p;
                }
            }
        }

        for (int k = 0; k < ws.length; k++) {
            List<Integer> listK = xListList[k];
            for (int p = 0; p < W + 1; p++) {
                if (rightSelectedArray[k][p]) {
                    int q = 0;
                    for (int i = 0; i < ws.length; i++) {
                        if (i != k) {
                            List<Integer> xListI = xListList[i];
                            for (int s : xListI) {
                                if (s + wImproved[i][s] <= p) {
                                    q = Math.max(q, s + wImproved[i][s]);
                                }
                            }
                        }
                    }
                    wImproved[k][q] = p + ws[k] - q;
                    if (p != q) {
                        rightSelectedArray[k][p] = false;
                        if (!leftSelectedArray[k][p]) {
                            for (int i = 0; i < listK.size(); i++) {
                                int x = listK.get(i);
                                if(x == p){
                                    listK.remove(i);
                                    break;
                                }
                            }
                        }
                        rightSelectedArray[k][q] = true;
                        if (!leftSelectedArray[k][q]) listK.add(q);
                    }
                }
            }
        }

        for (List<Integer> list : xListList) Collections.sort(list);

        for (int k = 0; k < n; k++) {
            List<Integer> xListK = xListList[k];
            for (int i = xListK.size() - 1; i >= 0; i--) {
                int p = xListK.get(i);
                for (int j = i + 1; j < xListK.size(); j++) {
                    int s = xListK.get(j);
                    if (s + wImproved[k][s] <= p + wImproved[k][p]) {
                        xListK.remove(i);
                        break;
                    }
                }
            }
        }

        // 获取所有List的并集
        boolean[] allMimSelectedArray = new boolean[W + 1];
        for (List<Integer> list : xListList) {
            for (int p : list) {
                if (!allMimSelectedArray[p]) {
                    allMimSelectedArray[p] = true;
                    xList.add(p);
                }
            }
        }
        Collections.sort(xList);

        return xListList;
    }

}