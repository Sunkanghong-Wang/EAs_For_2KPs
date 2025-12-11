package com.wskh.utils;

import com.wskh.classes.Item;
import com.wskh.classes.PlaceItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FastOppUtil {

    public static List<PlaceItem> fast_Heu_OPP(int curN, int W, int H, Item[] copyItems, Random random) {
        Arrays.sort(copyItems, Item.itemComparatorByDecreaseS);
        List<PlaceItem> placeItemList = SkyLineUtil.skyLineBasedTabuSearchInOneBin(W, H, copyItems, random);
        if (placeItemList.size() == curN) return placeItemList;
        return null;
    }

    private static int f(int dffType, int k, int C, int x, int[] xs) {
        return switch (dffType) {
            case 1 -> DffUtil.dff0(k, C, x);
            case 2 -> DffUtil.dff1(k, C, x);
            case 3 -> DffUtil.dff3(k, C, x);
            case 4 -> DffUtil.dff2(k, C, x, xs);
            default -> throw new RuntimeException();
        };
    }

    public static boolean fast_Bound_OPP(int curN, int W, int H, Item[] copyItems) {
        // 冲突检测
        int totalS = 0;
        for (int i = 0; i < curN; i++) {
            Item itemI = copyItems[i];
            if (itemI.h > H) return false;
            totalS += itemI.s;
            for (int j = i + 1; j < curN; j++) {
                if (i != j) {
                    Item itemJ = copyItems[j];
                    if (itemI.w + itemJ.w > W && itemI.h + itemJ.h > H) {
                        return false;
                    }
                }
            }
        }

        // 计算下界
        // LB0
        int sppH_LB = CommonUtil.ceilToInt((double) totalS / W);
        if (sppH_LB > H) return false;

        // LB1
        Arrays.sort(copyItems, (o1, o2) -> -Integer.compare(o1.h, o2.h));
        int[] totalW_Arr = new int[curN];
        totalW_Arr[0] = copyItems[0].w;
        for (int i = 1; i < curN; i++) {
            totalW_Arr[i] = copyItems[i].w + totalW_Arr[i - 1];
        }
        int k = 0;
        for (; k < curN; k++) if (totalW_Arr[k] > W) break;
        k--;
        int l = k + 1;
        for (; l < curN; l++) {
            Item itemL = copyItems[l];
            int il = k - 1;
            for (; il >= 0; il--) if (itemL.w + totalW_Arr[il] <= W) break;
            il++;
            int lb = itemL.h + copyItems[il].h;
            if (sppH_LB < lb) {
                if (lb > H) return false;
                sppH_LB = lb;
            }
        }

        // LB2
        int halfW = W / 2;
        for (int alpha = 1; alpha <= halfW; alpha++) {
            int part1 = 0;
            int part2 = 0;
            int part3 = 0;
            for (Item item : copyItems) {
                int w = item.w;
                int h = item.h;
                if (w > W - alpha) {
                    part1 += h;
                } else if (w > halfW) {
                    part1 += h;
                    part3 += ((W - w) * h);
                } else if (w >= alpha) {
                    part2 += item.s;
                }
            }
            int lb = part1 - Math.max(0, CommonUtil.ceilToInt((part2 - part3) / (double) W));
            if (sppH_LB < lb) {
                if (lb > H) return false;
                sppH_LB = lb;
            }
        }

        // LB3
        int[] ws = new int[curN];
        for (int i = 0; i < curN; i++) ws[i] = copyItems[i].w;
        Arrays.sort(ws);
        boolean[] booleans = new boolean[W + 1];
        booleans[0] = true;
        List<Integer> arr2 = new ArrayList<>();
        for (Item item : copyItems) {
            int w = item.w;
            if (w <= halfW) {
                if (!booleans[w]) {
                    booleans[w] = true;
                    arr2.add(w);
                }
            } else {
                w = W - w;
                if (!booleans[w]) {
                    booleans[w] = true;
                    arr2.add(w);
                }
            }
        }
        for (k = 1; k <= 4; k++) {
            for (int beta : arr2) {
                if (k == 1) {
                    for (int alpha = 1; alpha <= W; alpha++) {
                        int lb = 0;
                        for (Item item : copyItems) {
                            lb += (f(k, alpha, W, f(2, beta, W, item.w, ws), ws) * item.h);
                        }
                        lb = CommonUtil.ceilToInt((double) lb / f(k, alpha, W, f(2, beta, W, W, ws), ws));
                        if (sppH_LB < lb) {
                            if (lb > H) return false;
                            sppH_LB = lb;
                        }
                    }
                } else {
                    for (int alpha : arr2) {
                        int lb = 0;
                        for (Item item : copyItems) {
                            lb += (f(k, alpha, W, f(2, beta, W, item.w, ws), ws) * item.h);
                        }
                        lb = CommonUtil.ceilToInt((double) lb / f(k, alpha, W, f(2, beta, W, W, ws), ws));
                        if (sppH_LB < lb) {
                            if (lb > H) {
                                return false;
                            }
                            sppH_LB = lb;
                        }
                    }
                }
            }
        }
        return true;
    }
}