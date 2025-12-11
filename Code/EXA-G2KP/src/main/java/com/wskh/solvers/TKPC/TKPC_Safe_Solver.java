package com.wskh.solvers.TKPC;

import com.wskh.classes.*;
import com.wskh.solvers.GTKP.GTKP_LS_Heu_Solver;
import com.wskh.solvers.GTKP.GTKP_LS_UB_Solver;
import com.wskh.solvers.TOPP.TOPP_Safe_Solver;
import com.wskh.utils.CommonUtil;
import com.wskh.utils.DffUtil;
import com.wskh.utils.TimeUtil;

import java.util.*;

public class TKPC_Safe_Solver {

    final int LONG_SIZE = 64;

    static class Node {
        byte opp; // -1 不可行，1 可行，0 未探索
        //        boolean opp;
        Node left;
        Node right;
    }

    static class LabelNode {
        public int value;
        public int remainingCapacity;
        public long[] itemUnEnableLabel;

        public LabelNode copy() {
            LabelNode copy = new LabelNode();
            copy.itemUnEnableLabel = itemUnEnableLabel.clone();
            copy.value = value;
            copy.remainingCapacity = remainingCapacity;
            return copy;
        }
    }

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
                    itemI.unitValue = (double) itemI.value / (double) s;
                    b2 = true;
                }
            }

            if (!b1 && !b2) break;
        }
    }

    Random random;

    public TKPC_Safe_Solver(Random random) {
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

    public long exploredNodes;
    public long generatedNodes;
    public long oppCnt;
    public long exactOppCnt;
    public long exactOppTime;
    public long oppTime;

    private void DepthFirstSearchA1(int a, LabelNode labelNode, Item[] packedItemArr, int packedItemNum) {
        if (a < curN) {

            // 剪枝
            if (dpArr[labelNode.remainingCapacity] + labelNode.value <= LB) return;

            int tempIdx = a;
            int localUB = labelNode.value;
            int tempRemainingCapacity = labelNode.remainingCapacity;
            for (; tempIdx < curN; tempIdx++) {
                if ((labelNode.itemUnEnableLabel[tempIdx / LONG_SIZE] & (1L << (tempIdx % LONG_SIZE))) == 0) {
                    IntValue_Item tempItem = items[tempIdx];
                    if (tempItem.s <= tempRemainingCapacity) {
                        localUB += tempItem.value;
                        tempRemainingCapacity -= tempItem.s;
                    } else {
                        localUB += CommonUtil.ceilToLong(tempItem.unitValue * tempRemainingCapacity);
                        break;
                    }
                }
            }
            if (localUB <= LB) return;

            boolean canBePacked = (labelNode.itemUnEnableLabel[a / LONG_SIZE] & (1L << (a % LONG_SIZE))) == 0;

            // 放物品
            if (canBePacked && labelNode.remainingCapacity >= items[a].s) {

                if (TimeUtil.isTimeLimit()) throw new EarlyTerminationException();

                // 打包 a
                // 将与 item 绑定的物品全部打包
                LabelNode newLabelNode = labelNode.copy();
                int clone_packedItemNum = packedItemNum;
                for (int i : eachItemSameIndexList[a]) {
                    int pos1_i = i / LONG_SIZE;
                    int pos2_i = i % LONG_SIZE;
                    long val_i = 1L << pos2_i;
                    if ((newLabelNode.itemUnEnableLabel[pos1_i] & val_i) == 0) {
                        IntValue_Item itemA = items[i];
                        newLabelNode.remainingCapacity -= itemA.s;
                        if (newLabelNode.remainingCapacity < 0) break;
                        packedItemArr[clone_packedItemNum++] = itemA;
                        newLabelNode.value += itemA.value;
                        newLabelNode.itemUnEnableLabel[pos1_i] |= val_i;
                        for (int j : eachItemDifIndexList[i]) {
                            int pos1_j = j / LONG_SIZE;
                            int pos2_j = j % LONG_SIZE;
                            long val_j = 1L << pos2_j;
                            newLabelNode.itemUnEnableLabel[pos1_j] |= val_j;
                        }
                    }
                }

                if (newLabelNode.remainingCapacity >= 0 && newLabelNode.value <= UB) {
                    generatedNodes++;
                    if (newLabelNode.value > LB) {
                        // 进行 Opp check
                        long oppStartTime = System.currentTimeMillis();
                        TOPP_Safe_Solver oppSolver = new TOPP_Safe_Solver(random);
                        Item[] oppItems = new Item[clone_packedItemNum];
                        for (int i = 0; i < clone_packedItemNum; i++) oppItems[i] = packedItemArr[i].copy();
                        List<PlaceItem> placeItemList = oppSolver.solve(W, H, n, oppItems);
                        oppCnt++;
                        exactOppCnt += oppSolver.exactOppCnt;
                        exactOppTime += oppSolver.exactOppTime;
                        oppTime += (System.currentTimeMillis() - oppStartTime);
                        // 如果check通过，则回到正常DFS
                        if (placeItemList != null) {
                            // 更新全局最优解
                            bestPlaceItemList = placeItemList;
                            LB = newLabelNode.value;
                            System.out.println("Find better: " + LB);
                            if (LB == UB) throw new EarlyTerminationException();
                            DepthFirstSearchA1(a + 1, newLabelNode, packedItemArr, clone_packedItemNum);
                            exploredNodes++;
                        }
                    } else {
                        DepthFirstSearchA1(a + 1, newLabelNode, packedItemArr, clone_packedItemNum);
                        exploredNodes++;
                    }
                }

            }
            // 不放物品
            if (canBePacked) {
                generatedNodes++;
                for (int i : eachItemOutAfterOutList[a]) {
                    int pos1_i = i / LONG_SIZE;
                    int pos2_i = i % LONG_SIZE;
                    long val_i = 1L << pos2_i;
                    labelNode.itemUnEnableLabel[pos1_i] |= val_i;
                }
                DepthFirstSearchA1(a + 1, labelNode, packedItemArr, packedItemNum);
                exploredNodes++;
            } else {
                DepthFirstSearchA1(a + 1, labelNode, packedItemArr, packedItemNum);
            }
        }
    }

    private void DepthFirstSearchA3(int a, Node node, LabelNode labelNode, Item[] packedItemArr, int packedItemNum) {
        if (a < curN) {

            // 剪枝
            if (dpArr[labelNode.remainingCapacity] + labelNode.value < objLB) return;

            int tempIdx = a;
            int localUB = labelNode.value;
            int tempRemainingCapacity = labelNode.remainingCapacity;
            for (; tempIdx < curN; tempIdx++) {
                if ((labelNode.itemUnEnableLabel[tempIdx / LONG_SIZE] & (1L << (tempIdx % LONG_SIZE))) == 0) {
                    IntValue_Item tempItem = items[tempIdx];
                    if (tempItem.s <= tempRemainingCapacity) {
                        localUB += tempItem.value;
                        tempRemainingCapacity -= tempItem.s;
                    } else {
                        localUB += CommonUtil.ceilToLong(tempItem.unitValue * tempRemainingCapacity);
                        break;
                    }
                }
            }
            if (localUB < objLB) return;

            boolean canBePacked = (labelNode.itemUnEnableLabel[a / LONG_SIZE] & (1L << (a % LONG_SIZE))) == 0;

            // 放物品
            if (canBePacked && labelNode.remainingCapacity >= items[a].s
                    && (node.left == null || node.left.opp != -1)) {

                if (TimeUtil.isTimeLimit()) throw new EarlyTerminationException();

                // 打包 a
                // 将与 item 绑定的物品全部打包
                LabelNode newLabelNode = labelNode.copy();
                int clone_packedItemNum = packedItemNum;
                for (int i : eachItemSameIndexList[a]) {
                    int pos1_i = i / LONG_SIZE;
                    int pos2_i = i % LONG_SIZE;
                    long val_i = 1L << pos2_i;
                    if ((newLabelNode.itemUnEnableLabel[pos1_i] & val_i) == 0) {
                        IntValue_Item itemA = items[i];
                        newLabelNode.remainingCapacity -= itemA.s;
                        if (newLabelNode.remainingCapacity < 0) break;
                        packedItemArr[clone_packedItemNum++] = itemA;
                        newLabelNode.value += itemA.value;
                        newLabelNode.itemUnEnableLabel[pos1_i] |= val_i;
                        for (int j : eachItemDifIndexList[i]) {
                            int pos1_j = j / LONG_SIZE;
                            int pos2_j = j % LONG_SIZE;
                            long val_j = 1L << pos2_j;
                            newLabelNode.itemUnEnableLabel[pos1_j] |= val_j;
                        }
                    }
                }

                if (newLabelNode.remainingCapacity >= 0 && newLabelNode.value <= UB) {
                    Node leftNode = node.left;
                    boolean newNode = false;
                    if (leftNode == null) {
                        leftNode = new Node();
                        node.left = leftNode;
                        generatedNodes++;
                        newNode = true;
                    }

                    if (newLabelNode.value >= objLB && leftNode.opp == 0) {
                        // 进行 Opp check
                        long oppStartTime = System.currentTimeMillis();
                        TOPP_Safe_Solver oppSolver = new TOPP_Safe_Solver(random);
                        Item[] oppItems = new Item[clone_packedItemNum];
                        for (int i = 0; i < clone_packedItemNum; i++) oppItems[i] = packedItemArr[i].copy();
                        List<PlaceItem> placeItemList = oppSolver.solve(W, H, n, oppItems);
                        oppCnt++;
                        exactOppCnt += oppSolver.exactOppCnt;
                        exactOppTime += oppSolver.exactOppTime;
                        oppTime += (System.currentTimeMillis() - oppStartTime);
                        // 如果check通过，则回到正常DFS
                        leftNode.opp = (byte) (placeItemList != null ? 1 : -1);
                        if (placeItemList != null) {
                            // 更新全局最优解
                            bestPlaceItemList = placeItemList;
                            LB = newLabelNode.value;
                            objLB = LB + 1;
                            System.out.println("Find better: " + LB);
                            if (LB == UB) throw new EarlyTerminationException();
                            DepthFirstSearchA3(a + 1, leftNode, newLabelNode, packedItemArr, clone_packedItemNum);
                            if (newNode) exploredNodes++;
                        }
                    } else {
                        DepthFirstSearchA3(a + 1, leftNode, newLabelNode, packedItemArr, clone_packedItemNum);
                        if (newNode) exploredNodes++;
                    }
                }

            }
            // 不放物品
            if (canBePacked) {
                for (int i : eachItemOutAfterOutList[a]) {
                    int pos1_i = i / LONG_SIZE;
                    int pos2_i = i % LONG_SIZE;
                    long val_i = 1L << pos2_i;
                    labelNode.itemUnEnableLabel[pos1_i] |= val_i;
                }
                boolean newNode = false;
                if (node.right == null) {
                    node.right = new Node();
                    node.right.opp = node.opp;
                    generatedNodes++;
                    newNode = true;
                }
                DepthFirstSearchA3(a + 1, node.right, labelNode, packedItemArr, packedItemNum);
                if (newNode) exploredNodes++;
            } else {
                if (node.right == null) {
                    node.right = new Node();
                    node.right.opp = node.opp;
                }
                DepthFirstSearchA3(a + 1, node.right, labelNode, packedItemArr, packedItemNum);
            }
        }
    }

    private void redQuick() {
        redTime = System.currentTimeMillis();
        boolean relax = (long) S * curN > 1000000L;
        List<Integer> indexList = new ArrayList<>(curN);
        for (int i = 0; i < curN; i++) indexList.add(i);
        indexList.sort((o1, o2) -> Integer.compare(items[o1].value, items[o2].value));

        // 开始 dominate 缩减
        HashSet<Integer> pPie = new HashSet<>();
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
        for (IntValue_Item item : items) {
            int a = item.index;
            if (out[a]) {
                // 不打包
                I_out++;
                int pos1_a = a / LONG_SIZE;
                int pos2_a = a % LONG_SIZE;
                long val_a = 1L << pos2_a;
                if ((initLabelNode.itemUnEnableLabel[pos1_a] & val_a) == 0) {
                    for (int i : eachItemOutAfterOutList[a]) {
                        int pos1_i = i / LONG_SIZE;
                        int pos2_i = i % LONG_SIZE;
                        long val_i = 1L << pos2_i;
                        initLabelNode.itemUnEnableLabel[pos1_i] |= val_i;
                    }
                }
            }
        }

        // 必须打包
        for (int a : pPie) {
            for (int i : eachItemSameIndexList[a]) {
                int pos1_i = i / LONG_SIZE;
                int pos2_i = i % LONG_SIZE;
                long val_i = 1L << pos2_i;
                if ((initLabelNode.itemUnEnableLabel[pos1_i] & val_i) == 0) {
                    IntValue_Item itemA = items[i];
                    initLabelNode.remainingCapacity -= itemA.s;
                    if (initLabelNode.remainingCapacity < 0) throw new RuntimeException();
                    init_packedItems[initPackdNum++] = itemA;
                    initLabelNode.value += itemA.value;
                    initLabelNode.itemUnEnableLabel[pos1_i] |= val_i;
                    for (int j : eachItemDifIndexList[i]) {
                        int pos1_j = j / LONG_SIZE;
                        int pos2_j = j % LONG_SIZE;
                        long val_j = 1L << pos2_j;
                        initLabelNode.itemUnEnableLabel[pos1_j] |= val_j;
                    }
                }
            }
        }

        redTime = System.currentTimeMillis() - redTime;
        System.out.println("curN = " + curN + " , I_out = " + I_out + " , I_in = " + I_in + " => " + redTime + " ms");

        if (initLabelNode.value > LB) {
            // 进行 Opp check
            long oppStartTime = System.currentTimeMillis();
            TOPP_Safe_Solver oppSolver = new TOPP_Safe_Solver(random);
            Item[] oppItems = new Item[initPackdNum];
            for (int i = 0; i < initPackdNum; i++) oppItems[i] = init_packedItems[i].copy();
            List<PlaceItem> placeItemList = oppSolver.solve(W, H, n, oppItems);
            oppCnt++;
            exactOppCnt += oppSolver.exactOppCnt;
            exactOppTime += oppSolver.exactOppTime;
            oppTime += (System.currentTimeMillis() - oppStartTime);

            if (placeItemList == null) {
                UB = LB;
                System.out.println("Better UB: " + UB);
                throw new EarlyTerminationException();
            }
            LB = initLabelNode.value;
            bestPlaceItemList = placeItemList;
            System.out.println("Find better: " + LB);
            if (LB == UB) throw new EarlyTerminationException();
        }
    }

    Item[] init_packedItems;
    int initPackdNum;
    LabelNode initLabelNode;

    int[][] eachItemDifIndexList;
    int[][] eachItemSameIndexList;
    int[][] eachItemOutAfterOutList;

    private void branchAndBound(TKPC_LS_Heu_Solver labelSettingSolver) {

        eachItemDifIndexList = labelSettingSolver.eachItemDifIndexList;
        eachItemSameIndexList = labelSettingSolver.eachItemSameIndexList;
        eachItemOutAfterOutList = labelSettingSolver.eachItemOutAfterOutList;

        init_packedItems = new Item[curN];
        initLabelNode = new LabelNode();
        initLabelNode.remainingCapacity = S;
        initLabelNode.itemUnEnableLabel = new long[Math.max((curN + LONG_SIZE - 1) / LONG_SIZE, 1)];

        redQuick();

        int step = Integer.MAX_VALUE;
        for (IntValue_Item item : items) step = Math.min(step, item.value);
        step = Math.max(step, (UB - LB) / 6);

        try {
            if (Math.max(LB + 1, UB - step) == LB + 1) {
                System.out.println("A1");
                DepthFirstSearchA1(0, initLabelNode.copy(), init_packedItems, initPackdNum);
                if (!TimeUtil.isTimeLimit()) UB = LB;
            } else {
                System.out.println("A3");
                Node node = new Node();
                while (!TimeUtil.isTimeLimit() && UB > LB) {
                    objLB = UB - step;
                    if (objLB <= LB + 1) {
                        DepthFirstSearchA1(0, initLabelNode.copy(), init_packedItems, initPackdNum);
                        if (!TimeUtil.isTimeLimit()) UB = LB;
                        break;
                    }
                    DepthFirstSearchA3(0, node, initLabelNode.copy(), init_packedItems, initPackdNum);
                    if (!TimeUtil.isTimeLimit()) {
                        UB = objLB - 1;
                        System.out.println("Better UB: " + UB);
                    } else {
                        break;
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError: " + exploredNodes + " " + generatedNodes + " " + TimeUtil.getCurTime());
            System.out.println("Continue to A1");
            DepthFirstSearchA1(0, initLabelNode.copy(), init_packedItems, initPackdNum);
            if (!TimeUtil.isTimeLimit()) UB = LB;
        }
    }

    public int n, curN;
    public int UB, LB;
    public int UB0_KP, UB0_LS, UB0, LB0, objLB;
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
            branchAndBound(lsHeuSolver);
        } catch (EarlyTerminationException e) {
        }

    }

}