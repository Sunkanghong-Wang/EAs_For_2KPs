package com.wskh.solvers.TOPP;

import com.wskh.classes.EarlyTerminationException;
import com.wskh.classes.Item;
import com.wskh.classes.PlaceItem;
import com.wskh.utils.FastOppUtil;
import com.wskh.utils.PointSetUtil;
import com.wskh.utils.TimeUtil;
import com.wskh.utils.YCheckUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TOPP_Safe_Solver extends Solver {

    public TOPP_Safe_Solver(Random random) {
        super(random);
    }

    int minIdx;
    int failCnt;
    int maxFailCnt;

    public void dfsYFirst(int idx, int[] skylines, PlaceItem lastPlaceItem, PlaceItem[] placeItemArr) {
        if (idx < curN) {

            // 看看是否存在无法放的物品
            if (idx >= minIdx) {
                for (int i = idx; i < curN; i++) {
                    List<Integer> list = yListList[i];
                    Item itemI = items[i];
                    boolean b = false;
                    for (int pos : list) {
                        boolean canPack = true;
                        int maxPos = pos + itemI.h;
                        for (; pos < maxPos; pos++) {
                            if (skylines[pos] + itemI.w > W) {
                                canPack = false;
                                break;
                            }
                        }
                        if (canPack) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        failCnt = 0;
                        return;
                    }
                }
                if (++failCnt >= maxFailCnt) {
                    minIdx++;
                    failCnt = 0;
                }
            }

            if (TimeUtil.isTimeLimit()) throw new EarlyTerminationException();
            generatedNodes++;

            Item item = items[idx];
            int itemW = item.w;
            int itemH = item.h;
            PlaceItem placeItem = item.packed(0, 0);
            placeItemArr[idx] = placeItem;

            // 遍历所有位置
            List<Integer> list = yListList[idx];
            for (int pos : list) {
                int packedPos = pos;
                if (lastPlaceItem != null && lastPlaceItem.w == itemW && lastPlaceItem.h == itemH) {
                    if (lastPlaceItem.y > pos) continue;
                }
                int maxPos = pos + itemH;
                for (; packedPos < maxPos; packedPos++) {
                    skylines[packedPos] += itemW;
                    if (skylines[packedPos] > W) {
                        for (; packedPos >= pos; packedPos--) {
                            skylines[packedPos] -= itemW;
                        }
                        break;
                    }
                }
                if (packedPos == maxPos) {
                    placeItem.y = pos;
                } else {
                    continue;
                }

                dfsYFirst(idx + 1, skylines, placeItem, placeItemArr);

                packedPos = pos;
                for (; packedPos < maxPos; packedPos++) skylines[packedPos] -= itemW;
            }
        } else {
            // 叶子节点，则进行check
            generatedNodes++;
            List<PlaceItem> placeItemList = Arrays.asList(placeItemArr);
            boolean check = checker.check(placeItemList);
            if (check) {
                feasiblePlaceItemList = placeItemList;
                exploredNodes++;
                throw new EarlyTerminationException();
            }
        }
        exploredNodes++;
    }

    public void dfsXFirst(int idx, int[] skylines, PlaceItem lastPlaceItem, PlaceItem[] placeItemArr) {
        if (idx < curN) {

            // 看看是否存在无法放的物品
            if (idx >= minIdx) {
                for (int i = idx; i < curN; i++) {
                    List<Integer> list = xListList[i];
                    Item itemI = items[i];
                    boolean b = false;
                    for (int pos : list) {
                        boolean canPack = true;
                        int maxPos = pos + itemI.w;
                        for (; pos < maxPos; pos++) {
                            if (skylines[pos] + itemI.h > H) {
                                canPack = false;
                                break;
                            }
                        }
                        if (canPack) {
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        failCnt = 0;
                        return;
                    }
                }
                if (++failCnt >= maxFailCnt) {
                    minIdx++;
                    failCnt = 0;
                }
            }

            if (TimeUtil.isTimeLimit()) throw new EarlyTerminationException();
            generatedNodes++;

            Item item = items[idx];
            int itemW = item.w;
            int itemH = item.h;
            PlaceItem placeItem = item.packed(0, 0);
            placeItemArr[idx] = placeItem;

            // 遍历所有位置
            List<Integer> list = xListList[idx];
            for (int pos : list) {
                int packedPos = pos;
                if (lastPlaceItem != null && lastPlaceItem.w == itemW && lastPlaceItem.h == itemH) {
                    if (lastPlaceItem.x > pos) continue;
                }
                int maxPos = pos + itemW;
                for (; packedPos < maxPos; packedPos++) {
                    skylines[packedPos] += itemH;
                    if (skylines[packedPos] > H) {
                        for (; packedPos >= pos; packedPos--) {
                            skylines[packedPos] -= itemH;
                        }
                        break;
                    }
                }
                if (packedPos == maxPos) {
                    placeItem.x = pos;
                } else {
                    continue;
                }

                dfsXFirst(idx + 1, skylines, placeItem, placeItemArr);

                packedPos = pos;
                for (; packedPos < maxPos; packedPos++) skylines[packedPos] -= itemH;
            }

            exploredNodes++;
        } else {
            // 叶子节点，则进行check
            generatedNodes++;
            List<PlaceItem> placeItemList = Arrays.asList(placeItemArr);
            boolean check = checker.check(placeItemList);
            if (check) {
                feasiblePlaceItemList = placeItemList;
                exploredNodes++;
                throw new EarlyTerminationException();
            }
            exploredNodes++;
        }
    }

    public List<Integer>[] xListList;
    public List<Integer>[] yListList;
    public YCheckUtil checker;

    public void ExactOpp() {
        Arrays.sort(items, Item.itemComparatorByDecreaseSWH);
//        Arrays.sort(items, Item.itemComparatorByIncreaseSWH);
//        Arrays.sort(items, Item.itemComparatorByDecreaseHW);

        // MIM_Pro pro
        int[] ws = new int[curN];
        int[] hs = new int[curN];
        int minWIndex = -1;
        int minW = Integer.MAX_VALUE;
        int minHIndex = -1;
        int minH = Integer.MAX_VALUE;
        for (int i = 0; i < curN; i++) {
            Item item = items[i];
            ws[i] = item.w;
            hs[i] = item.h;
            if (minW > item.w) {
                minW = item.w;
                minWIndex = i;
            }
            if (minH > item.h) {
                minH = item.h;
                minHIndex = i;
            }
        }

        List<Integer> xList = new ArrayList<>();
        xListList = PointSetUtil.MIM_Pro(W, ws, xList, minWIndex);
        List<Integer> yList = new ArrayList<>();
        yListList = PointSetUtil.MIM_Pro(H, hs, yList, minHIndex);

        boolean xFirst = Arrays.stream(xListList).mapToInt(List::size).sum() < Arrays.stream(yListList).mapToInt(List::size).sum();

        checker = new YCheckUtil(W, H, xFirst, n);

        minIdx = 2;

        try {
            if (xFirst) {
                maxFailCnt = 0;
                for (int i = 0; i < curN; i++) maxFailCnt += xListList[i].size();
                maxFailCnt /= curN;
                dfsXFirst(0, new int[W], null, new PlaceItem[curN]);
            } else {
                maxFailCnt = 0;
                for (int i = 0; i < curN; i++) maxFailCnt += yListList[i].size();
                maxFailCnt /= curN;
                dfsYFirst(0, new int[H], null, new PlaceItem[curN]);
            }
        } catch (EarlyTerminationException e) {
        }
    }

    public int n;
    public List<PlaceItem> feasiblePlaceItemList;
    public long exactOppCnt;
    public long exactOppTime;
    public long exploredNodes;
    public long generatedNodes;

    public List<PlaceItem> solve(int initW, int initH, int n, Item[] initItems) {

        // 完美包装OPP
        if (n <= 200 && Math.max(initW, initH) >= 50) {
            try {
                int s = 0;
                for (Item initItem : initItems) s += initItem.s;
                if (s == initW * initH) {
                    PPP_Solver pppSolver = new PPP_Solver();
                    List<PlaceItem> placeItemList = pppSolver.solve(initW, initH, n, initItems);
                    exploredNodes = pppSolver.exploredNodes;
                    generatedNodes = pppSolver.generatedNodes;
                    return placeItemList;
                }
            } catch (OutOfMemoryError e) {
                System.out.println("PPP Algorithm OutOfMemoryError: " + TimeUtil.getCurTime());
            }
        }

        this.W = initW;
        this.H = initH;
        this.n = n;
        this.curN = initItems.length;
        this.items = initItems;

        // 预处理
        preprocessing();

        // 快速界限 OPP
        Item[] cloneItems = items.clone();
        if (!FastOppUtil.fast_Bound_OPP(curN, W, H, cloneItems)) return null;

        // 快速启发式 OPP
        List<PlaceItem> placeItemList = FastOppUtil.fast_Heu_OPP(curN, W, H, cloneItems, random);
        if (placeItemList != null) return placeItemList;

        // 精确 OPP
        exactOppTime = System.currentTimeMillis();
        exactOppCnt = 1;

        try {
            // 普通OPP
            ExactOpp();
            exactOppTime = System.currentTimeMillis() - exactOppTime;
//            System.out.println(exploredNodes + " " + generatedNodes);
//            System.out.println(minIdx + " " + curN);
            return feasiblePlaceItemList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}