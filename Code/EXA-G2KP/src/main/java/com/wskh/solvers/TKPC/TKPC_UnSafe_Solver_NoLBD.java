package com.wskh.solvers.TKPC;

import com.wskh.classes.EarlyTerminationException;
import com.wskh.classes.IntValue_Item;
import com.wskh.classes.Item;
import com.wskh.classes.PlaceItem;
import com.wskh.solvers.TOPP.TOPP_UnSafe_Solver;
import com.wskh.utils.*;
import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.*;

public class TKPC_UnSafe_Solver_NoLBD {

    private void getReducedItemList() {
        // 根据对偶值缩减项目集合
        List<IntValue_Item> reducedItemList = new ArrayList<>(curN);
        for (int i = 0; i < curN; i++) {
            IntValue_Item itemI = items[i];
            if (itemI.w <= W && itemI.h <= H && itemI.value > 0) {
                reducedItemList.add(itemI.copy());
            }
        }
        curN = reducedItemList.size();
        items = new IntValue_Item[curN];
        for (int i = 0; i < curN; i++) items[i] = reducedItemList.get(i);
    }

    public void preprocessing() {
        Arrays.sort(items, Comparator.comparingInt(o -> -o.value));
        for (int i = 0; i < curN; i++) items[i].index = i;
        while (true) {
            // shrinkingBin
            boolean b1 = false;

            int[] dpX = new int[W + 1];
            int[] dpY = new int[H + 1];
            for (Item item : items) {
                int w = item.w;
                int h = item.h;
                for (int j = W; j >= w; j--) dpX[j] = Math.max(dpX[j], dpX[j - w] + w);
                for (int j = H; j >= h; j--) dpY[j] = Math.max(dpY[j], dpY[j - h] + h);
            }
            int newW = dpX[W];
            int newH = dpY[H];

            int S = newW * newH;
            if (S < W * H) {
                W = newW;
                H = newH;
                b1 = true;
            }

            // enlargingItems
            boolean b2 = false;
            for (int i = 0; i < curN; i++) {

                IntValue_Item itemI = items[i];
                newW = W - itemI.w;
                newH = H - itemI.h;
                dpX = new int[newW + 1];
                dpY = new int[newH + 1];
                // 遍历物品
                for (int j = 0; j < curN; j++) {
                    if (j != i) {
                        Item itemJ = items[j];
                        int w = itemJ.w;
                        int h = itemJ.h;
                        // 遍历背包容量
                        for (int k = newW; k >= w; k--) dpX[k] = Math.max(dpX[k], dpX[k - w] + w);
                        for (int k = newH; k >= h; k--) dpY[k] = Math.max(dpY[k], dpY[k - h] + h);
                    }
                }
                newW = W - dpX[newW];
                newH = H - dpY[newH];

                int s = newW * newH;
                if (s > itemI.s) {
                    itemI.s = s;
                    itemI.w = newW;
                    itemI.h = newH;
                    itemI.unitValue = (double) itemI.value / itemI.s;
                    b2 = true;
                }
            }

            if (!b1 && !b2) break;
        }
    }

    Random random;

    public TKPC_UnSafe_Solver_NoLBD(Random random) {
        this.random = random;
    }

    int[] dpArr;

    private void computeUpperBound() {
        S = W * H;
        int max_k = Math.min(4, (int) (1000000L / ((long) S * curN)));
        int[][] conservativeScalesArr = new int[max_k + 1][curN];

        for (int i = 0; i < curN; i++) conservativeScalesArr[0][i] = items[i].s;
        for (int k = 1; k <= max_k; k++) {
            for (int i = 0; i < curN; i++) {
                conservativeScalesArr[k][i] = DffUtil.dff0(k, S, items[i].s);
            }
        }

        for (int[] conservativeScales : conservativeScalesArr) {
            int[] dp = new int[S + 1];
            for (int u = 0; u < conservativeScales.length; u++) {
                int w = conservativeScales[u];
                int v = items[u].value;
                for (int j = S; j >= w; j--) {
                    dp[j] = Math.max(dp[j], dp[j - w] + v);
                }
            }
            if (dpArr == null) {
                dpArr = dp;
            } else {
                for (int j = 0; j < dpArr.length; j++) {
                    dpArr[j] = Math.min(dpArr[j], dp[j]);
                }
            }
        }
        UB0_KP = dpArr[S];
        UB0 = UB0_KP;
        UB = UB0_KP;
    }

    private void addPosInList(List<Integer> list, int pos) {
        for (int i = 0; i < list.size(); i++) {
            int x = list.get(i);
            if (x == pos) return;
            if (x > pos) {
                list.add(i, pos);
                return;
            }
        }
        list.add(pos);
    }

    private String getKeyByPlaceItemList(List<PlaceItem> placeItemList) {
        int[] arr = new int[placeItemList.size()];
        for (int i = 0; i < placeItemList.size(); i++) {
            arr[i] = placeItemList.get(i).id;
        }
        Arrays.sort(arr);
        return Arrays.toString(arr);
    }

    private List<IloRange> addBendersCut(IloCplex masterModel, IloIntVar[][] x, List<PlaceItem> placeItemList0, boolean xFirst) {
        Integer[] idIndexMapStar = new Integer[n];
        for (int i = 0; i < placeItemList0.size(); i++) {
            idIndexMapStar[placeItemList0.get(i).id] = i;
        }
        List<IloRange> cuts = new ArrayList<>();

        HashMap<String, List<PlaceItem>> minInfeasibleListMap = new HashMap<>();
        minInfeasibleListMap.put(getKeyByPlaceItemList(placeItemList0), placeItemList0);

        // Split lists of placed items
        HashSet<String> set = new HashSet<>();
        while (true) {
            HashMap<String, List<PlaceItem>> tempMap = new HashMap<>();
            int oldSize = minInfeasibleListMap.size();
            for (Map.Entry<String, List<PlaceItem>> entry : minInfeasibleListMap.entrySet()) {
                boolean isSplit = false;
                if (set.add(entry.getKey())) {
                    List<PlaceItem> placeItemList = entry.getValue();
                    if (xFirst) {
                        List<Integer> xPositions = new ArrayList<>(W);
                        boolean[] xUsedArr = new boolean[W];
                        for (PlaceItem placeItem : placeItemList) {
                            int x1 = placeItem.x;
                            int x2 = x1 + placeItem.w;
                            if (x1 > 0) CommonUtil.addPosition(x1, xUsedArr, xPositions);
                            if (x2 < W) CommonUtil.addPosition(x2, xUsedArr, xPositions);
                        }
                        for (int xPos : xPositions) {
                            List<PlaceItem> leftPlaceItemList = new ArrayList<>(placeItemList.size());
                            List<PlaceItem> rightPlaceItemList = new ArrayList<>(placeItemList.size());
                            for (PlaceItem placeItem : placeItemList) {
                                if (placeItem.x + placeItem.w <= xPos) {
                                    leftPlaceItemList.add(placeItem);
                                } else if (placeItem.x >= xPos) {
                                    rightPlaceItemList.add(placeItem);
                                } else {
                                    break;
                                }
                            }
                            if (!leftPlaceItemList.isEmpty() && !rightPlaceItemList.isEmpty() && leftPlaceItemList.size() + rightPlaceItemList.size() == placeItemList.size()) {
                                tempMap.put(getKeyByPlaceItemList(leftPlaceItemList), leftPlaceItemList);
                                tempMap.put(getKeyByPlaceItemList(rightPlaceItemList), rightPlaceItemList);
                                isSplit = true;
                            }
                        }
                    } else {
                        List<Integer> yPositions = new ArrayList<>(H);
                        boolean[] yUsedArr = new boolean[H];
                        for (PlaceItem placeItem : placeItemList) {
                            int y1 = placeItem.y;
                            int y2 = y1 + placeItem.h;
                            if (y1 > 0) CommonUtil.addPosition(y1, yUsedArr, yPositions);
                            if (y2 < H) CommonUtil.addPosition(y2, yUsedArr, yPositions);
                        }
                        for (int yPos : yPositions) {
                            List<PlaceItem> topPlaceItemList = new ArrayList<>(placeItemList.size());
                            List<PlaceItem> bottomPlaceItemList = new ArrayList<>(placeItemList.size());
                            for (PlaceItem placeItem : placeItemList) {
                                if (placeItem.y + placeItem.h <= yPos) {
                                    bottomPlaceItemList.add(placeItem);
                                } else if (placeItem.y >= yPos) {
                                    topPlaceItemList.add(placeItem);
                                } else {
                                    break;
                                }
                            }
                            if (!topPlaceItemList.isEmpty() && !bottomPlaceItemList.isEmpty() && topPlaceItemList.size() + bottomPlaceItemList.size() == placeItemList.size()) {
                                tempMap.put(getKeyByPlaceItemList(topPlaceItemList), topPlaceItemList);
                                tempMap.put(getKeyByPlaceItemList(bottomPlaceItemList), bottomPlaceItemList);
                                isSplit = true;
                            }
                        }
                    }
                }
                if (!isSplit) tempMap.put(entry.getKey(), entry.getValue());
            }
            if (oldSize == tempMap.size()) {
                break;
            } else {
                minInfeasibleListMap = new HashMap<>(tempMap);
            }
        }

        // 尝试2：
        HashMap<String, List<PlaceItem>> tempMap = new HashMap<>();
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);

            if (xFirst) {
                boolean[] xUsedArr = new boolean[W];
                for (PlaceItem placeItem : placeItemList) xUsedArr[placeItem.x] = true;
                for (int i = 0; i < W; i++) {
                    if (xUsedArr[i]) {
                        List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
                        for (PlaceItem placeItem : placeItemList) {
                            if (placeItem.x != i) tempList.add(placeItem);
                        }
                        if (!checker.check(PlaceItem.copy(tempList))) placeItemList = tempList;
                    }
                }

                for (int i = W - 1; i >= 0; i--) {
                    List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
                    for (PlaceItem placeItem : placeItemList) {
                        if (placeItem.x <= i && placeItem.x + placeItem.w > i) tempList.add(placeItem);
                    }
                    if (!checker.check(PlaceItem.copy(tempList))) placeItemList = tempList;
                }
            } else {
                boolean[] yUsedArr = new boolean[H];
                for (PlaceItem placeItem : placeItemList) yUsedArr[placeItem.y] = true;
                for (int i = 0; i < H; i++) {
                    if (yUsedArr[i]) {
                        List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
                        for (PlaceItem placeItem : placeItemList) {
                            if (placeItem.y != i) tempList.add(placeItem);
                        }
                        if (!checker.check(PlaceItem.copy(tempList))) placeItemList = tempList;
                    }
                }

                for (int i = H - 1; i >= 0; i--) {
                    List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
                    for (PlaceItem placeItem : placeItemList) {
                        if (placeItem.y <= i && placeItem.y + placeItem.h > i) tempList.add(placeItem);
                    }
                    if (!checker.check(PlaceItem.copy(tempList))) placeItemList = tempList;
                }
            }

            tempMap.put(getKeyByPlaceItemList(placeItemList), placeItemList);
        }
        minInfeasibleListMap = new HashMap<>(tempMap);

        // First attempt: Delete one by one in ascending order of area
        tempMap = new HashMap<>();
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);
            placeItemList.sort((o1, o2) -> Integer.compare(o1.s, o2.s));
            List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
            for (int i = placeItemList.size() - 1; i >= 0; i--) {
                PlaceItem removedPlaceItem = placeItemList.remove(i);
                boolean check = checker.check(PlaceItem.copy(placeItemList));
                if (TimeUtil.isTimeLimit()) {
                    return cuts;
                }
                if (check) {
                    placeItemList.addLast(removedPlaceItem);
                    tempList.add(removedPlaceItem);
                }
            }
            tempMap.put(getKeyByPlaceItemList(tempList), new ArrayList<>(tempList));
        }
        minInfeasibleListMap = new HashMap<>(tempMap);

        // Second attempt: Delete one by one in ascending order of width
        tempMap = new HashMap<>();
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);
            placeItemList.sort((o1, o2) -> Integer.compare(o1.w, o2.w));
            List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
            for (int i = placeItemList.size() - 1; i >= 0; i--) {
                PlaceItem removedPlaceItem = placeItemList.remove(i);
                boolean check = checker.check(PlaceItem.copy(placeItemList));
                if (check) {
                    placeItemList.addLast(removedPlaceItem);
                    tempList.add(removedPlaceItem);
                }
            }
            tempMap.put(getKeyByPlaceItemList(tempList), new ArrayList<>(tempList));
        }
        minInfeasibleListMap = new HashMap<>(tempMap);

        // Third attempt: Delete one by one in ascending order of height
        tempMap = new HashMap<>();
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);
            placeItemList.sort((o1, o2) -> Integer.compare(o1.h, o2.h));
            List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
            for (int i = placeItemList.size() - 1; i >= 0; i--) {
                PlaceItem removedPlaceItem = placeItemList.remove(i);
                boolean check = checker.check(PlaceItem.copy(placeItemList));
                if (check) {
                    placeItemList.addLast(removedPlaceItem);
                    tempList.add(removedPlaceItem);
                }
            }
            tempMap.put(getKeyByPlaceItemList(tempList), new ArrayList<>(tempList));
        }
        minInfeasibleListMap = new HashMap<>(tempMap);

        // Fourth attempt: Delete one by one in ascending order of perimeter
        tempMap = new HashMap<>();
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);
            placeItemList.sort((o1, o2) -> Integer.compare(o1.w + o1.h, o2.w + o2.h));
            List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
            for (int i = placeItemList.size() - 1; i >= 0; i--) {
                PlaceItem removedPlaceItem = placeItemList.remove(i);
                boolean check = checker.check(PlaceItem.copy(placeItemList));
                if (check) {
                    placeItemList.addLast(removedPlaceItem);
                    tempList.add(removedPlaceItem);
                }
            }
            tempMap.put(getKeyByPlaceItemList(tempList), new ArrayList<>(tempList));
        }
        minInfeasibleListMap = new HashMap<>(tempMap);

        // Fifth attempt: Delete one by one in ascending order of coordinates
        tempMap = new HashMap<>();
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);
            if (xFirst) {
                placeItemList.sort((o1, o2) -> Integer.compare(o1.x, o2.x));
            } else {
                placeItemList.sort((o1, o2) -> Integer.compare(o1.y, o2.y));
            }
            List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
            for (int i = placeItemList.size() - 1; i >= 0; i--) {
                PlaceItem removedPlaceItem = placeItemList.remove(i);
                boolean check = checker.check(PlaceItem.copy(placeItemList));
                if (check) {
                    placeItemList.addLast(removedPlaceItem);
                    tempList.add(removedPlaceItem);
                }
            }
            tempMap.put(getKeyByPlaceItemList(tempList), new ArrayList<>(tempList));
        }
        minInfeasibleListMap = new HashMap<>(tempMap);

        // Sixth attempt: Delete one by one in ascending order of overlapping scores
        tempMap = new HashMap<>();
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);
            List<Integer> scores = new ArrayList<>(placeItemList.size());
            for (int i = 0; i < placeItemList.size(); i++) {
                int s = 0;
                PlaceItem placeItemI = placeItemList.get(i);
                for (int j = 0; j < placeItemList.size(); j++) {
                    if (i != j) {
                        PlaceItem placeItemJ = placeItemList.get(j);
                        if (xFirst) {
                            if (!(placeItemI.x + placeItemI.w <= placeItemJ.x) || (placeItemI.x >= placeItemJ.x + placeItemJ.w)) {
                                s++;
                            }
                        } else {
                            if (!(placeItemI.y + placeItemI.h <= placeItemJ.y) || (placeItemI.y >= placeItemJ.y + placeItemJ.h)) {
                                s++;
                            }
                        }
                    }
                }
                scores.add(s);
            }
            List<Map.Entry<PlaceItem, Integer>> itemScorePairs = new ArrayList<>();
            for (int i = 0; i < placeItemList.size(); i++) {
                itemScorePairs.add(new AbstractMap.SimpleEntry<>(placeItemList.get(i), scores.get(i)));
            }
            itemScorePairs.sort((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue()));
            for (int i = 0; i < placeItemList.size(); i++) {
                placeItemList.set(i, itemScorePairs.get(i).getKey());
            }
            List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
            for (int i = placeItemList.size() - 1; i >= 0; i--) {
                PlaceItem removedPlaceItem = placeItemList.remove(i);
                boolean check = checker.check(PlaceItem.copy(placeItemList));
                if (check) {
                    placeItemList.addLast(removedPlaceItem);
                    tempList.add(removedPlaceItem);
                }
            }
            tempMap.put(getKeyByPlaceItemList(tempList), new ArrayList<>(tempList));
        }
        minInfeasibleListMap = new HashMap<>(tempMap);

        // Seventh attempt: Delete one by one in random order and try 6 times
        for (int h = 0; h < 6; h++) {
            tempMap = new HashMap<>();
            for (String key : minInfeasibleListMap.keySet()) {
                List<PlaceItem> placeItemList = new ArrayList<>(minInfeasibleListMap.get(key));
                Collections.shuffle(placeItemList, random);
                List<PlaceItem> tempList = new ArrayList<>(placeItemList.size());
                for (int i = placeItemList.size() - 1; i >= 0; i--) {
                    PlaceItem removedPlaceItem = placeItemList.remove(i);
                    boolean check = checker.check(PlaceItem.copy(placeItemList));
                    if (check) {
                        placeItemList.addLast(removedPlaceItem);
                        tempList.add(removedPlaceItem);
                    }
                }
                tempMap.put(getKeyByPlaceItemList(tempList), new ArrayList<>(tempList));
            }
            minInfeasibleListMap = new HashMap<>(tempMap);
        }

        // Traverse each infeasible subset
        for (String key : minInfeasibleListMap.keySet()) {
            List<PlaceItem> placeItemList = minInfeasibleListMap.get(key);

            // 获取重叠
            boolean haveOverlap = false;
            PlaceItem[][] overlapPlaceItem = new PlaceItem[placeItemList.size()][placeItemList.size()];
            int[][] overlapPlaceItemIndex = new int[placeItemList.size()][placeItemList.size()];
            for (int i = 0; i < placeItemList.size(); i++) {
                int index = 0;
                PlaceItem placeItemI = placeItemList.get(i);
                int posI = !xFirst ? placeItemI.y : placeItemI.x;
                int lenI = !xFirst ? placeItemI.h : placeItemI.w;
                for (int j = 0; j < placeItemList.size(); j++) {
                    if (i != j) {
                        PlaceItem placeItemJ = placeItemList.get(j);
                        int posJ = !xFirst ? placeItemJ.y : placeItemJ.x;
                        int lenJ = !xFirst ? placeItemJ.h : placeItemJ.w;
                        if (posI >= posJ + lenJ || posJ >= posI + lenI) {
                            continue;
                        }
                        overlapPlaceItem[i][index] = placeItemList.get(j);
                        overlapPlaceItemIndex[i][index++] = j;
                        haveOverlap = true;
                    }
                }
            }
            if (!haveOverlap) continue;

            try {
                IloCplex lpCplex = new IloCplex();
                lpCplex.setWarning(null);
                lpCplex.setOut(null);
                lpCplex.setParam(IloCplex.IntParam.Threads, 1);
                List<IloNumVar[]> varList = new ArrayList<>();
                IloLinearNumExpr target = lpCplex.linearNumExpr();
                for (PlaceItem placeItemI : placeItemList) {
                    int posI = !xFirst ? placeItemI.y : placeItemI.x;
                    int lenI = !xFirst ? placeItemI.h : placeItemI.w;
                    IloNumVar[] numVars = new IloNumVar[]{lpCplex.numVar(0, posI), lpCplex.numVar(posI, !xFirst ? H - lenI : W - lenI),};
                    target.addTerm(numVars[1], 1);
                    target.addTerm(numVars[0], -1);
                    varList.add(numVars);
                }
                lpCplex.addMaximize(target);
                for (int i = 0; i < placeItemList.size(); i++) {
                    for (int j = 0; j < placeItemList.size(); j++) {
                        if (overlapPlaceItem[i][j] == null) {
                            break;
                        }
                        PlaceItem placeItemI = placeItemList.get(i);
                        int lenI = !xFirst ? placeItemI.h : placeItemI.w;
                        lpCplex.addLe(lpCplex.diff(varList.get(overlapPlaceItemIndex[i][j])[1], varList.get(i)[0]), lenI - 1);
                    }
                }
                if (lpCplex.solve()) {
                    IloLinearNumExpr cut = masterModel.linearNumExpr();
                    int count = 0;
                    for (int i = 0; i < placeItemList.size(); i++) {
                        double[] values = lpCplex.getValues(varList.get(i));
                        int itemIndex = idIndexMap[placeItemList.get(i).id];
                        boolean b = false;
                        List<Integer> list = (xFirst ? xListList[itemIndex] : yListList[itemIndex]);
                        for (int k = 0; k < list.size(); k++) {
                            int pos = list.get(k);
                            if (pos <= values[1] && pos >= values[0]) {
                                cut.addTerm(1, x[itemIndex][k]);
                                b = true;
                            }
                        }
                        if (b) {
                            count++;
                        }
                    }
                    cuts.add(masterModel.le(cut, count - 1));
                } else {
                    throw new RuntimeException("Add Cut model without solution");
                }
                lpCplex.end();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return cuts;
    }

    public class LazyCallback extends IloCplex.LazyConstraintCallback {
        IloIntVar[][] x;
        boolean xFirst;
        IloCplex masterModel;
        List<Integer>[] listList;
        List<Integer> R;

        public LazyCallback(IloIntVar[][] x, boolean xFirst, IloCplex masterModel, List<Integer>[] listList, List<Integer> R) {
            this.x = x;
            this.xFirst = xFirst;
            this.masterModel = masterModel;
            this.listList = listList;
            this.R = R;
        }

        public void main() throws IloException {

            System.out.print("\t Join lazyCut => ");
            List<PlaceItem> placeItemList = new ArrayList<>();
            for (int i = 0; i < curN; i++) {
                for (int j = 0; j < x[i].length; j++) {
                    if (getValue(x[i][j]) > 0.5) {
                        Item item = items[i];
                        if (!xFirst) {
                            placeItemList.add(new PlaceItem(item.id, item.index, 0, yListList[i].get(j), item.w, item.h, item.s));
                        } else {
                            placeItemList.add(new PlaceItem(item.id, item.index, xListList[i].get(j), 0, item.w, item.h, item.s));
                        }
                        break;
                    }
                }
            }

            long startTime = System.currentTimeMillis();
            boolean check = checker.check(placeItemList);
            System.out.print(getObjValue() + " => ");
            System.out.print("check: " + (System.currentTimeMillis() - startTime) + " ms");
            if (!TimeUtil.isTimeLimit()) {
                if (check) {
                    System.out.println(" => true");
                    UB = Math.min(UB, CommonUtil.ceilToInt(getBestObjValue()));
                    int objValue = (int) Math.round(getObjValue());
                    if (objValue > LB) {
                        bestPlaceItemList = placeItemList;
                        LB = objValue;
                        System.out.println("\t Find better pattern with objective value: " + objValue);
                    }
                    if (UB == LB) throw new EarlyTerminationException();
                } else {
                    long s = System.currentTimeMillis();
                    System.out.println(" => false");
                    List<IloRange> cuts;
                    try {
                        cuts = addBendersCut(masterModel, x, new ArrayList<>(placeItemList), xFirst);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    for (IloRange cut : cuts) addLocal(cut);
                    System.out.println("\t Add local cuts: " + cuts.size() + " , " + (System.currentTimeMillis() - s) + " ms");
                }
            }

        }
    }

    YCheckUtil checker;
    public long exploredNodes;
    public long generatedNodes;

    private void combinatorialBendersDecomposition(boolean xFirst, boolean haveNull) throws Exception {
        checker = new YCheckUtil(W, H, xFirst, n);

        List<Integer>[] listList = (xFirst ? xListList : yListList);
        List<Integer> R = (xFirst ? xList : yList);
        IloCplex masterModel = new IloCplex();
        masterModel.setOut(null);
        masterModel.setWarning(null);
        masterModel.setParam(IloCplex.IntParam.Threads, 1);

        IloIntVar[][] x = new IloIntVar[curN][];
        IloLinearNumExpr[] y = new IloLinearNumExpr[curN];
        for (int i = 0; i < curN; i++) {
            y[i] = masterModel.linearNumExpr();
            x[i] = masterModel.boolVarArray(listList[i].size());
            for (int j = 0; j < x[i].length; j++) {
                y[i].addTerm(1, x[i][j]);
            }
            masterModel.addLe(y[i], 1);
        }

        IloLinearNumExpr[] binV = new IloLinearNumExpr[R.size()];
        boolean[][] dpBools = new boolean[R.size()][curN];
        for (int i = 0; i < binV.length; i++) binV[i] = masterModel.linearNumExpr();
        for (int i = 0; i < curN; i++) {
            Item item = items[i];
            List<Integer> intArrayList = listList[i];
            int w = (xFirst ? item.w : item.h);
            int h = (xFirst ? item.h : item.w);
            for (int j = 0; j < intArrayList.size(); j++) {
                int xj = intArrayList.get(j);
                for (int k = 0; k < R.size(); k++) {
                    int q = R.get(k);
                    if (q >= xj && q <= xj + w - 1) {
                        binV[k].addTerm(h, x[i][j]);
                        dpBools[k][i] = true;
                    }
                }
            }
        }
        for (int k = 0; k < binV.length; k++) {
            int C = (xFirst ? H : W);
            int[] dp = new int[C + 1];
            for (Item item : items) {
                int i = item.index;
                if (dpBools[k][i]) {
                    int c = (xFirst ? item.h : item.w);
                    for (int j = C; j >= c; j--) dp[j] = Math.max(dp[j], dp[j - c] + c);
                }
            }
            masterModel.addLe(binV[k], dp[C]);
        }

        // obj function
        IloLinearNumExpr target = masterModel.linearNumExpr();
        for (int i = 0; i < curN; i++) {
            int pi = items[i].value;
            for (int j = 0; j < x[i].length; j++) {
                target.addTerm(pi, x[i][j]);
            }
        }

        masterModel.addMaximize(target);

        // Conflict-Based Cuts
        Set<String> set = new HashSet<>();
        for (int[] conflict : conflictList) {
            int i = conflict[0];
            int j = conflict[1];
            if (i > j) {
                j = i ^ j;
                i = i ^ j;
                j = i ^ j;
            }
            if (set.add(i + "," + j)) {
                masterModel.addLe(masterModel.sum(y[i], y[j]), 1);
            }
        }

        for (int i = 0; i < curN; i++) {
            Item itemI = items[i];
            for (int j = i + 1; j < curN; j++) {
                Item itemJ = items[j];
                if (itemI.w + itemJ.w > W && itemI.h + itemJ.h > H) {
                    if (set.add(i + "," + j)) {
                        masterModel.addLe(masterModel.sum(y[i], y[j]), 1);
                    }
                }
            }
        }

        for (int i = 0; i < curN; i++) {
            IntValue_Item itemI = items[i];
            List<Integer> listI = listList[i];
            for (int j : dominateArr1[i]) {
                IntValue_Item itemJ = items[j];
                if (itemI.w == itemJ.w && itemI.h == itemJ.h) {
                    List<Integer> listJ = listList[j];
                    IloLinearNumExpr expr = masterModel.linearNumExpr();
                    for (int p = 0; p < listI.size(); p++) expr.addTerm(listI.get(p), x[i][p]);
                    for (int q = 0; q < listJ.size(); q++) expr.addTerm(-listJ.get(q), x[j][q]);
                    masterModel.addGe(expr, 0);
                }
                masterModel.addGe(y[i], y[j]);
            }
        }

        for (int index : pPie) masterModel.addEq(y[index], 1);

        // Bound-Based Cuts
        masterModel.addLe(target, UB);
        masterModel.addGe(target, LB);
//        masterModel.setParam(IloCplex.Param.MIP.Tolerances.UpperCutoff, UB);
//        masterModel.setParam(IloCplex.Param.MIP.Tolerances.LowerCutoff, LB);

        if (!haveNull) {
            // add init solution
            double[][] values = new double[curN][];
            int size = 0;
            for (int i = 0; i < curN; i++) {
                values[i] = new double[listList[i].size()];
                size += values[i].length;
            }
            for (PlaceItem placeItem : bestPlaceItemList) {
                int pos = xFirst ? placeItem.x : placeItem.y;
                int itemIndex = idIndexMap[placeItem.id];
                List<Integer> posList = listList[itemIndex];
                int posIndex = -1;
                for (int i = 0; i < posList.size(); i++) {
                    if (posList.get(i) == pos) {
                        posIndex = i;
                        break;
                    }
                }
                values[itemIndex][posIndex] = 1;
            }
            double[] initValues = new double[size];
            IloIntVar[] iloIntVars = new IloIntVar[size];
            int c = 0;
            for (int i = 0; i < x.length; i++) {
                for (int j = 0; j < x[i].length; j++) {
                    initValues[c] = values[i][j];
                    iloIntVars[c++] = x[i][j];
                }
            }
            masterModel.addMIPStart(iloIntVars, initValues);
        }

        masterModel.use(new LazyCallback(x, xFirst, masterModel, listList, R));

        masterModel.setParam(IloCplex.DoubleParam.TimeLimit, TimeUtil.getRemainingTime() / 1000d);
        try {
            masterModel.solve();
            IloCplex.Status status = masterModel.getStatus();
            if (status.equals(IloCplex.Status.Infeasible) || status.equals(IloCplex.Status.Optimal)) UB = LB;
        } catch (EarlyTerminationException e) {

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        exploredNodes = masterModel.getNnodes();
        long unexploredNodes = masterModel.getNnodesLeft();
        generatedNodes = exploredNodes + unexploredNodes;
        masterModel.end();
    }

    HashSet<Integer> pPie = new HashSet<>();

    private void redQuick() {
        redTime = System.currentTimeMillis();
        boolean relax = (long) S * curN > 1000000L;
        List<Integer> indexList = new ArrayList<>(curN);
        for (int i = 0; i < curN; i++) indexList.add(i);
        indexList.sort((o1, o2) -> Integer.compare(items[o1].value, items[o2].value));

        // 开始 dominate 缩减
        boolean[] out = new boolean[curN];
        for (int i : indexList) {
            if (out[i] || pPie.contains(i)) continue;

            // 计算放了i之后必须放的物品id
            Set<Integer> mustPackedItemIndexSet = new HashSet<>(pPie);
            for (int j : eachItemSameIndexList[i]) mustPackedItemIndexSet.add(j);
            boolean[] mustBolArray = new boolean[n];
            for (int index : mustPackedItemIndexSet) mustBolArray[index] = true;

            // (a) Free conflict
            boolean haveConflict = false;
            for (int j : mustPackedItemIndexSet) {
                if (out[j]) {
                    haveConflict = true;
                    break;
                }
                for (int k : eachItemDifIndexList[j]) {
                    if (mustBolArray[k]) {
                        haveConflict = true;
                        break;
                    }
                }
                if (haveConflict) break;
            }
            if (haveConflict) {
                for (int j : eachItemOutAfterOutList[i]) out[j] = true;
                continue;
            }

            // (b) Free area
            int mustPackedS = 0;
            for (int j : mustPackedItemIndexSet) mustPackedS += items[j].s;
            if (mustPackedS > S) {
                for (int j : eachItemOutAfterOutList[i]) out[j] = true;
                continue;
            }

            // (c) Free cost
            int cost = 0;
            for (int j : mustPackedItemIndexSet) cost += items[j].value;
            if (cost > UB) {
                for (int j : eachItemOutAfterOutList[i]) out[j] = true;
                continue;
            }

            // (d) Free number
            int totalW = 0;
            int nx = 0;
            List<Integer> wList = new ArrayList<>();
            for (int j : mustPackedItemIndexSet) {
                boolean b = true;
                Item itemJ = items[j];
                for (int k : mustPackedItemIndexSet) {
                    if (k != j) {
                        Item itemK = items[k];
                        if (itemK.h + itemJ.h <= H) {
                            b = false;
                            break;
                        }
                    }
                }
                if (b) {
                    totalW += itemJ.w;
                    nx++;
                } else {
                    wList.add(itemJ.w);
                }
            }
            if (totalW > W) {
                for (int j : eachItemOutAfterOutList[i]) out[j] = true;
                continue;
            }
            int totalH = 0;
            int ny = 0;
            List<Integer> hList = new ArrayList<>();
            for (int j : mustPackedItemIndexSet) {
                boolean b = true;
                Item itemJ = items[j];
                for (int k : mustPackedItemIndexSet) {
                    if (k != j) {
                        Item itemK = items[k];
                        if (itemK.w + itemJ.w <= W) {
                            b = false;
                            break;
                        }
                    }
                }
                if (b) {
                    totalH += itemJ.h;
                    ny++;
                } else {
                    hList.add(itemJ.h);
                }
            }
            if (totalH > H) {
                for (int j : eachItemOutAfterOutList[i]) out[j] = true;
                continue;
            }

            Collections.sort(wList);
            totalW = W - totalW;
            for (int w : wList) {
                totalW -= w;
                if (totalW >= 0) {
                    nx++;
                } else {
                    break;
                }
            }
            Collections.sort(hList);
            totalH = H - totalH;
            for (int h : hList) {
                totalH -= h;
                if (totalH >= 0) {
                    ny++;
                } else {
                    break;
                }
            }
            if (nx * ny < mustPackedItemIndexSet.size()) {
                for (int j : eachItemOutAfterOutList[i]) out[j] = true;
                continue;
            }

            // (e) Area program
            int remainS = S - mustPackedS;
            if (relax) {
                cost += dpArr[remainS];
            } else {
                boolean[] bs = new boolean[curN];
                for (int j : mustPackedItemIndexSet) bs[j] = true;
                for (int j : eachItemDifIndexList[i]) bs[j] = true;

                int[] dp = new int[remainS + 1];
                for (IntValue_Item item : items) {
                    if (!out[item.index] && !bs[item.index]) {
                        int w = item.s;
                        int v = item.value;
                        // 遍历背包容量
                        for (int j = remainS; j >= w; j--) dp[j] = Math.max(dp[j], dp[j - w] + v);
                    }
                }
                cost += dp[remainS];
            }
            if (cost <= LB) {
                for (int j : eachItemOutAfterOutList[i]) out[j] = true;
                continue;
            }

            // (f) Update I_in
            cost = 0;
            remainS = S;
            boolean[] tempOut = new boolean[curN];
            for (int j : pPie) {
                tempOut[j] = true;
                remainS -= items[j].s;
                cost += items[j].value;
            }

            if (relax) {
                cost += dpArr[remainS];
            } else {

                for (int j : eachItemOutAfterOutList[i]) tempOut[j] = true;

                int[] dp = new int[remainS + 1];
                for (int j = 0; j < curN; j++) {
                    if (!out[j] && !tempOut[j]) {
                        IntValue_Item itemJ = items[j];
                        int w = itemJ.s;
                        int v = itemJ.value;
                        // 遍历背包容量
                        for (int k = remainS; k >= w; k--) {
                            dp[k] = Math.max(dp[k], dp[k - w] + v);
                        }
                    }
                }
                cost += dp[remainS];
            }
            if (cost <= LB) {
                pPie.addAll(mustPackedItemIndexSet);
                for (int j : mustPackedItemIndexSet) {
                    for (int k : eachItemDifIndexList[j]) out[k] = true;
                }
            }
        }

        I_in = pPie.size();
        I_out = 0;
        List<IntValue_Item> reducedItemList = new ArrayList<>();
        for (IntValue_Item item : items) {
            if (out[item.index]) {
                I_out++;
            } else {
                reducedItemList.add(item);
            }
        }

        if (reducedItemList.size() < curN) {
            Integer[] idIndexMap = new Integer[n];
            for (int i = 0; i < reducedItemList.size(); i++) {
                idIndexMap[reducedItemList.get(i).id] = i;
            }

            ArrayList<Integer> copyP = new ArrayList<>(pPie);
            pPie.clear();
            for (int index : copyP) pPie.add(idIndexMap[items[index].id]);

            int[][] tempEachItemDifIndexList = new int[reducedItemList.size()][];
            for (int i = 0; i < reducedItemList.size(); i++) {
                List<Integer> list = new ArrayList<>();
                for (int j : eachItemDifIndexList[reducedItemList.get(i).index]) {
                    Integer integer = idIndexMap[items[j].id];
                    if (integer != null) list.add(integer);
                }
                int[] arr = new int[list.size()];
                for (int j = 0; j < arr.length; j++) arr[j] = list.get(j);
                tempEachItemDifIndexList[i] = arr;
            }
            eachItemDifIndexList = tempEachItemDifIndexList;

            int[][] tempEachItemSameIndexList = new int[reducedItemList.size()][];
            for (int i = 0; i < reducedItemList.size(); i++) {
                List<Integer> list = new ArrayList<>();
                for (int j : eachItemSameIndexList[reducedItemList.get(i).index]) {
                    Integer integer = idIndexMap[items[j].id];
                    if (integer != null) list.add(integer);
                }
                int[] arr = new int[list.size()];
                for (int j = 0; j < arr.length; j++) arr[j] = list.get(j);
                tempEachItemSameIndexList[i] = arr;
            }
            eachItemSameIndexList = tempEachItemSameIndexList;

            int[][] tempEachItemOutAfterOutList = new int[reducedItemList.size()][];
            for (int i = 0; i < reducedItemList.size(); i++) {
                List<Integer> list = new ArrayList<>();
                for (int j : eachItemOutAfterOutList[reducedItemList.get(i).index]) {
                    Integer integer = idIndexMap[items[j].id];
                    if (integer != null) list.add(integer);
                }
                int[] arr = new int[list.size()];
                for (int j = 0; j < arr.length; j++) arr[j] = list.get(j);
                tempEachItemOutAfterOutList[i] = arr;
            }
            eachItemOutAfterOutList = tempEachItemOutAfterOutList;

            HashSet<Integer>[] tempDominateListArr1 = new HashSet[reducedItemList.size()];
            for (int i = 0; i < reducedItemList.size(); i++) {
                HashSet<Integer> list = new HashSet<>();
                for (int j : dominateArr1[reducedItemList.get(i).index]) {
                    Integer integer = idIndexMap[items[j].id];
                    if (integer != null) list.add(integer);
                }
                tempDominateListArr1[i] = list;
            }
            dominateArr1 = tempDominateListArr1;

            // 修改冲突约束
            List<int[]> newConflictList = new ArrayList<>();
            for (int[] conflict : conflictList) {
                if (!out[conflict[0]] && !out[conflict[1]]) {
                    conflict[0] = idIndexMap[items[conflict[0]].id];
                    conflict[1] = idIndexMap[items[conflict[1]].id];
                    newConflictList.add(conflict);
                }
            }
            conflictList = newConflictList;

            items = new IntValue_Item[reducedItemList.size()];
            for (int i = 0; i < reducedItemList.size(); i++) {
                items[i] = reducedItemList.get(i);
                items[i].index = i;
            }
            curN = reducedItemList.size();
        }

        // 必须打包
        int initValue = 0;
        List<Item> mustPackedItemList = new ArrayList<>();
        for (int bindA : pPie) {
            initValue += items[bindA].value;
            mustPackedItemList.add(items[bindA]);
        }

        redTime = System.currentTimeMillis() - redTime;
        System.out.println("curN = " + curN + " , I_out = " + I_out + " , I_in = " + I_in + " => " + redTime + " ms");

        if (initValue > LB) {
            // 进行 Opp check
            TOPP_UnSafe_Solver oppSolver = new TOPP_UnSafe_Solver(random);
            Item[] oppItems = new Item[mustPackedItemList.size()];
            for (int i = 0; i < mustPackedItemList.size(); i++) oppItems[i] = mustPackedItemList.get(i).copy();
            List<PlaceItem> placeItemList = oppSolver.solve(W, H, n, oppItems);
            if (placeItemList == null) {
                UB = LB;
                System.out.println("Better UB: " + UB);
                throw new EarlyTerminationException();
            }
            LB = initValue;
            bestPlaceItemList = placeItemList;
            System.out.println("Find better: " + LB);
            if (LB == UB) throw new EarlyTerminationException();
        }
    }

    public List<Integer>[] xListList;
    public List<Integer>[] yListList;
    public List<Integer> xList;
    public List<Integer> yList;
    Integer[] idIndexMap;
    public long oppCnt;
    public long exactOppCnt;
    public long exactOppTime, oppTime;

    HashSet<Integer>[] dominateArr1;
    int[][] eachItemDifIndexList;
    int[][] eachItemSameIndexList;
    int[][] eachItemOutAfterOutList;

    private void bendersDecomposition(TKPC_LS_Heu_Solver labelSettingSolver) {

        dominateArr1 = labelSettingSolver.dominateArr1;
        eachItemDifIndexList = labelSettingSolver.eachItemDifIndexList;
        eachItemSameIndexList = labelSettingSolver.eachItemSameIndexList;
        eachItemOutAfterOutList = labelSettingSolver.eachItemOutAfterOutList;

        redQuick();

        if (curN == 0) {
            if (!TimeUtil.isTimeLimit()) UB = LB;
            return;
        }

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
        xList = new ArrayList<>();
        xListList = PointSetUtil.MIM_Pro(W, ws, xList, minWIndex);
        yList = new ArrayList<>();
        yListList = PointSetUtil.MIM_Pro(H, hs, yList, minHIndex);

        System.out.println("xList.size: " + xList.size() + " , yList.size: " + yList.size());

        idIndexMap = new Integer[n];
        for (int i = 0; i < curN; i++) idIndexMap[items[i].id] = i;

        boolean haveNull = false;
        for (PlaceItem placeItem : bestPlaceItemList) {
            if (idIndexMap[placeItem.id] == null) {
                haveNull = true;
                break;
            }
        }
        if (!haveNull) {
            for (PlaceItem placeItem : bestPlaceItemList) {
                int itemIndex = idIndexMap[placeItem.id];
                addPosInList(xListList[itemIndex], placeItem.x);
                addPosInList(yListList[itemIndex], placeItem.y);
                addPosInList(xList, placeItem.x);
                addPosInList(yList, placeItem.y);
            }
        }

        try {
            combinatorialBendersDecomposition(Arrays.stream(xListList).mapToInt(List::size).sum() < Arrays.stream(yListList).mapToInt(List::size).sum(), haveNull);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int n, curN;
    public int UB, LB;
    public int UB0_KP, UB0_LS, UB0, LB0;
    public long lb0Time, ub0kpTime, ub0lsTime, redTime;
    IntValue_Item[] items;
    int W, H, S;
    public List<PlaceItem> bestPlaceItemList;
    public int I_in, I_out;
    List<int[]> conflictList;

    public void solve(int n, int initW, int initH, IntValue_Item[] init_items, List<int[]> init_conflictList) {

        this.n = n;
        this.items = init_items;
        this.curN = init_items.length;
        this.W = initW;
        this.H = initH;
        this.conflictList = init_conflictList;

        // 问题缩减
        getReducedItemList();
        preprocessing();

        // 排序
        Arrays.sort(items, (o1, o2) -> -CommonUtil.compareDouble((double) o1.value / o1.s, (double) o2.value / o2.s));
        for (int i = 0; i < curN; i++) items[i].index = i;

        boolean[] have = new boolean[n];
        Integer[] idIndexMap = new Integer[n];
        for (IntValue_Item item : items) {
            have[item.id] = true;
            idIndexMap[item.id] = item.index;
        }

        // 修改冲突约束
        List<int[]> newConflictList = new ArrayList<>();
        for (int[] conflict : conflictList) {
            if (have[conflict[0]] && have[conflict[1]]) {
                conflict[0] = idIndexMap[conflict[0]];
                conflict[1] = idIndexMap[conflict[1]];
                newConflictList.add(conflict);
            }
        }
        conflictList = newConflictList;

        // Compute UB0
        ub0kpTime = System.currentTimeMillis();
        computeUpperBound();
        ub0kpTime = System.currentTimeMillis() - ub0kpTime;

        // Compute LB0
        lb0Time = System.currentTimeMillis();
        TKPC_LS_Heu_Solver lsHeuSolver = new TKPC_LS_Heu_Solver(n, curN, S, W, H, items, conflictList);
        try {
            lsHeuSolver.solve();
        } catch (EarlyTerminationException e) {

        }
        if (lsHeuSolver.bestPlaceItemList != null) {
            LB0 = -lsHeuSolver.minReducedCost;
            bestPlaceItemList = lsHeuSolver.bestPlaceItemList;
            LB = LB0;
        }
        lb0Time = System.currentTimeMillis() - lb0Time;

        if (UB == LB) return;

        // Compute UB1
        ub0lsTime = System.currentTimeMillis();
        try {
            UB0_LS = new TKPC_LS_UB_Solver(lsHeuSolver).solve();
            UB0 = Math.min(UB0, UB0_LS);
            UB = UB0;
        } catch (EarlyTerminationException e) {
        }
        ub0lsTime = System.currentTimeMillis() - ub0lsTime;

        if (UB == LB) return;

        // 运行 Benders 分解算法
        try {
            bendersDecomposition(lsHeuSolver);
        } catch (EarlyTerminationException e) {
        }

    }

}