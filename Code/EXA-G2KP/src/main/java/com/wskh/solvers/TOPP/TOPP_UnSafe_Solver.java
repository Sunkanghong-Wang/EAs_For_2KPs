package com.wskh.solvers.TOPP;

import com.wskh.classes.EarlyTerminationException;
import com.wskh.classes.Item;
import com.wskh.classes.PlaceItem;
import com.wskh.utils.*;
import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.*;

public class TOPP_UnSafe_Solver extends Solver {

    public TOPP_UnSafe_Solver(Random random) {
        super(random);
    }

    private String getKeyByPlaceItemList(List<PlaceItem> placeItemList) {
        int[] arr = new int[placeItemList.size()];
        for (int i = 0; i < placeItemList.size(); i++) {
            arr[i] = placeItemList.get(i).id;
        }
        Arrays.sort(arr);
        return Arrays.toString(arr);
    }

    public int infeasibleSetSize;

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

            infeasibleSetSize += placeItemList.size();

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

    public int liftCnt;
    public int addCutCnt;

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
            List<PlaceItem> placeItemList = new ArrayList<>();
            for (int i = 0; i < x.length; i++) {
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
            synchronized (this) {
                boolean check = checker.check(placeItemList);
                if (!TimeUtil.isTimeLimit()) {
                    if (check) {
                        feasiblePlaceItemList = placeItemList;
                        throw new EarlyTerminationException();
                    } else {
                        List<IloRange> cuts;
                        try {
                            liftCnt++;
                            cuts = addBendersCut(masterModel, x, new ArrayList<>(placeItemList), xFirst);
                            addCutCnt += cuts.size();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        for (IloRange cut : cuts) {
                            addLocal(cut);
                        }
                    }
                }
            }
        }
    }

    public boolean xFirst;
    public List<Integer>[] xListList;
    public List<Integer>[] yListList;
    Integer[] idIndexMap;
    public YCheckUtil checker;

    public void ExactOpp() throws Exception {
        Arrays.sort(items, Item.itemComparatorByDecreaseSWH);

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

        xFirst = Arrays.stream(xListList).mapToInt(List::size).sum() < Arrays.stream(yListList).mapToInt(List::size).sum();

        checker = new YCheckUtil(W, H, xFirst, n);

        idIndexMap = new Integer[n];
        for (int i = 0; i < curN; i++) {
            idIndexMap[items[i].id] = i;
        }

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
            masterModel.addEq(y[i], 1);
        }

//        IloLinearNumExpr[] binV = new IloLinearNumExpr[R.size()];
//        for (int i = 0; i < binV.length; i++) {
//            binV[i] = masterModel.linearNumExpr();
//        }
//        for (int i = 0; i < curN; i++) {
//            Item item = items[i];
//            List<Integer> intArrayList = listList[i];
//            int w = (xFirst ? item.w : item.h);
//            int h = (xFirst ? item.h : item.w);
//            for (int j = 0; j < intArrayList.size(); j++) {
//                int xj = intArrayList.get(j);
//                for (int k = 0; k < R.size(); k++) {
//                    int q = R.get(k);
//                    if (q >= xj && q <= xj + w - 1) {
//                        binV[k].addTerm(h, x[i][j]);
//                    }
//                }
//            }
//        }
//        for (IloLinearNumExpr expr : binV) {
//            masterModel.addLe(expr, (xFirst ? H : W));
//        }

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
            for (int i = 0; i < curN; i++) {
                if (dpBools[k][i]) {
                    Item item = items[i];
                    int c = (xFirst ? item.h : item.w);
                    for (int j = C; j >= c; j--) dp[j] = Math.max(dp[j], dp[j - c] + c);
                }
            }
            masterModel.addLe(binV[k], dp[C]);
        }

        HashSet<Integer>[] dominateArr1 = new HashSet[n]; // j=dominateArr1[i]: representing i dominant j (index is id)
        for (int i = 0; i < n; i++) dominateArr1[i] = new HashSet<>();
        for (int i = 0; i < curN; i++) {
            Item itemI = items[i];
            for (int j = i + 1; j < curN; j++) {
                Item itemJ = items[j];
                if (itemI.w <= itemJ.w && itemI.h <= itemJ.h) {
                    // itemI dominate itemJ
                    dominateArr1[itemI.id].add(itemJ.id);
                } else if (itemJ.w <= itemI.w && itemJ.h <= itemI.h) {
                    // itemJ dominate itemI
                    dominateArr1[itemJ.id].add(itemI.id);
                }
            }
        }

        for (int id = 0; id < n; id++) {
            if (idIndexMap[id] != null) {
                int i = idIndexMap[id];
                Item itemI = items[i];
                List<Integer> listI = listList[i];
                for (int jId : dominateArr1[id]) {
                    if (idIndexMap[jId] != null) {
                        int j = idIndexMap[jId];
                        Item itemJ = items[j];
                        if (itemI.w == itemJ.w && itemI.h == itemJ.h) {
                            List<Integer> listJ = listList[j];
                            IloLinearNumExpr expr = masterModel.linearNumExpr();
                            for (int p = 0; p < listI.size(); p++) {
                                expr.addTerm(listI.get(p), x[i][p]);
                            }
                            for (int q = 0; q < listJ.size(); q++) {
                                expr.addTerm(-listJ.get(q), x[j][q]);
                            }
                            masterModel.addGe(expr, 0);
                        }
                    }
                }
            }
        }

        masterModel.setParam(IloCplex.DoubleParam.TimeLimit, TimeUtil.getRemainingTime() / 1000d);

        masterModel.use(new LazyCallback(x, xFirst, masterModel, listList, R));
        try {
            masterModel.solve();
        } catch (EarlyTerminationException e) {
            exploredNodes = masterModel.getNnodes() + 1;
            long unexploredNodes = masterModel.getNnodesLeft() - 1;
            generatedNodes = exploredNodes + unexploredNodes;
            masterModel.end();
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        exploredNodes = masterModel.getNnodes();
        long unexploredNodes = masterModel.getNnodesLeft();
        generatedNodes = exploredNodes + unexploredNodes;
        masterModel.end();
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

        List<PlaceItem> placeItemList;

        // 预处理
        preprocessing();

        // 快速界限 OPP
        Item[] cloneItems = items.clone();
        if (!FastOppUtil.fast_Bound_OPP(curN, W, H, cloneItems)) return null;

        // 快速启发式 OPP
        placeItemList = FastOppUtil.fast_Heu_OPP(curN, W, H, cloneItems, random);
        if (placeItemList != null) return placeItemList;

        // 精确 OPP
        exactOppTime = System.currentTimeMillis();
        exactOppCnt = 1;

        try {
            ExactOpp();
            exactOppTime = System.currentTimeMillis() - exactOppTime;
            return feasiblePlaceItemList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}