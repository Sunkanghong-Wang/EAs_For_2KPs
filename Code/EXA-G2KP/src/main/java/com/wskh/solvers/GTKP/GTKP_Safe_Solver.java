package com.wskh.solvers.GTKP;

import com.wskh.classes.*;
import com.wskh.solvers.TOPP.TOPP_Safe_Solver;
import com.wskh.utils.CommonUtil;
import com.wskh.utils.DffUtil;
import com.wskh.utils.TimeUtil;

import java.util.*;

public class GTKP_Safe_Solver {

    final int LONG_SIZE = 64;

    static class Node {
        byte opp; // -1 不可行，1 可行，0 未探索
        //        boolean opp;
        Node left;
        Node right;
    }

    static class LabelNode {
        public long value;
        public int remainingCapacity;
        public long[] itemUnEnableLabel;
        public long[] srCutLabel;

        public LabelNode copy() {
            LabelNode copy = new LabelNode();
            copy.itemUnEnableLabel = itemUnEnableLabel.clone();
            copy.srCutLabel = srCutLabel.clone();
            copy.value = value;
            copy.remainingCapacity = remainingCapacity;
            return copy;
        }
    }

    private void getReducedItemList() {
        // 根据对偶值缩减项目集合
        List<LongValue_Item> reducedItemList = new ArrayList<>(curN);
        for (int i = 0; i < curN; i++) {
            LongValue_Item itemI = items[i];
            boolean canBeRemove = true;
            if (itemI.value > 0) {
                canBeRemove = false;
            } else {
                int iId = itemI.id;
                for (int j = 0; j < curN; j++) {
                    LongValue_Item itemJ = items[j];
                    if (fat[iId] == fat[itemJ.id] && itemJ.value > 0) {
                        canBeRemove = false;
                        break;
                    }
                }
            }
            if (!canBeRemove) reducedItemList.add(itemI.copy());
//            reducedItemList.add(itemI.copy());
        }
        curN = reducedItemList.size();
        items = new LongValue_Item[curN];
        for (int i = 0; i < curN; i++) items[i] = reducedItemList.get(i);
    }

    public void getReducedActivateSrCutList(List<SR_Cut> init_srCutList) {
        List<SR_Cut> reducedActivateSrCutList = new ArrayList<>(init_srCutList.size());
        boolean[] used = new boolean[n];
        for (Item item : items) used[item.id] = true;
        for (SR_Cut sr : init_srCutList) {
            int c = 0;
            for (int index : sr.indexs) if (used[index]) c++;
            if (c >= 2) reducedActivateSrCutList.add(sr);
        }
        SR_Pie = reducedActivateSrCutList.size();
        srCutList = new SR_Cut[SR_Pie];
        for (int i = 0; i < SR_Pie; i++) {
            srCutList[i] = reducedActivateSrCutList.get(i);
        }
    }

    public void preprocessing() {
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

                LongValue_Item itemI = items[i];
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

    public GTKP_Safe_Solver(Random random) {
        this.random = random;
    }

    long[] dpArr;

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
            long[] dp = new long[S + 1];
            for (int u = 0; u < conservativeScales.length; u++) {
                int w = conservativeScales[u];
                long v = items[u].value;
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
            long localUB = labelNode.value;
            int tempRemainingCapacity = labelNode.remainingCapacity;
            for (; tempIdx < curN; tempIdx++) {
                if ((labelNode.itemUnEnableLabel[tempIdx / LONG_SIZE] & (1L << (tempIdx % LONG_SIZE))) == 0) {
                    LongValue_Item tempItem = items[tempIdx];
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
            if (canBePacked && labelNode.remainingCapacity >= fatWeightArr[a]) {

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
                        LongValue_Item itemA = items[i];
                        newLabelNode.remainingCapacity -= itemA.s;
                        if (newLabelNode.remainingCapacity < 0) break;
                        // 更新 SR 不等式
                        for (int r : activatedSrConstraintIndexList[i]) {
                            SR_Cut srCut = srCutList[r];
                            int pos1_r = r / LONG_SIZE;
                            int pos2_r = r % LONG_SIZE;
                            long val_r = 1L << pos2_r;
                            if ((newLabelNode.srCutLabel[pos1_r] & val_r) == 0) {
                                newLabelNode.srCutLabel[pos1_r] |= val_r;
                            } else {
                                newLabelNode.srCutLabel[pos1_r] &= (~val_r);
                                newLabelNode.value -= srCut.penalty;
                            }
                        }
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
            long localUB = labelNode.value;
            int tempRemainingCapacity = labelNode.remainingCapacity;
            for (; tempIdx < curN; tempIdx++) {
                if ((labelNode.itemUnEnableLabel[tempIdx / LONG_SIZE] & (1L << (tempIdx % LONG_SIZE))) == 0) {
                    LongValue_Item tempItem = items[tempIdx];
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
            if (canBePacked && labelNode.remainingCapacity >= fatWeightArr[a]
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
                        LongValue_Item itemA = items[i];
                        newLabelNode.remainingCapacity -= itemA.s;
                        if (newLabelNode.remainingCapacity < 0) break;
                        // 更新 SR 不等式
                        for (int r : activatedSrConstraintIndexList[i]) {
                            SR_Cut srCut = srCutList[r];
                            int pos1_r = r / LONG_SIZE;
                            int pos2_r = r % LONG_SIZE;
                            long val_r = 1L << pos2_r;
                            if ((newLabelNode.srCutLabel[pos1_r] & val_r) == 0) {
                                newLabelNode.srCutLabel[pos1_r] |= val_r;
                            } else {
                                newLabelNode.srCutLabel[pos1_r] &= (~val_r);
                                newLabelNode.value -= srCut.penalty;
                            }
                        }
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
        indexList.sort((o1, o2) -> Long.compare(items[o1].value, items[o2].value));

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
            long cost = 0;
            for (int j : mustPackedItemIndexSet) cost += items[j].value;
            for (SR_Cut srCut : srCutList) {
                int cnt = 0;
                for (int index : srCut.indexs) {
                    if (index != -1 && mustBolArray[index]) cnt++;
                }
                if (cnt >= 2) cost -= srCut.penalty;
            }
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

                long[] dp = new long[remainS + 1];
                for (LongValue_Item item : items) {
                    if (!out[item.index] && !bs[item.index]) {
                        int w = item.s;
                        long v = item.value;
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

            for (SR_Cut srCut : srCutList) {
                int cnt = 0;
                for (int index : srCut.indexs) {
                    if (index != -1 && tempOut[index]) cnt++;
                }
                if (cnt >= 2) cost -= srCut.penalty;
            }

            if (relax) {
                cost += dpArr[remainS];
            } else {

                for (int j : eachItemOutAfterOutList[i]) tempOut[j] = true;

                long[] dp = new long[remainS + 1];
                for (int j = 0; j < curN; j++) {
                    if (!out[j] && !tempOut[j]) {
                        LongValue_Item itemJ = items[j];
                        int w = itemJ.s;
                        long v = itemJ.value;
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
        for (LongValue_Item item : items) {
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
                    LongValue_Item itemA = items[i];
                    initLabelNode.remainingCapacity -= itemA.s;
                    if (initLabelNode.remainingCapacity < 0) throw new RuntimeException();
                    // 更新 SR 不等式
                    for (int r : activatedSrConstraintIndexList[i]) {
                        SR_Cut srCut = srCutList[r];
                        int pos1_r = r / LONG_SIZE;
                        int pos2_r = r % LONG_SIZE;
                        long val_r = 1L << pos2_r;
                        if ((initLabelNode.srCutLabel[pos1_r] & val_r) == 0) {
                            initLabelNode.srCutLabel[pos1_r] |= val_r;
                        } else {
                            initLabelNode.srCutLabel[pos1_r] &= (~val_r);
                            initLabelNode.value -= srCut.penalty;
                        }
                    }
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

    int[] fatWeightArr;
    int[][] eachItemDifIndexList;
    int[][] eachItemSameIndexList;
    int[][] eachItemOutAfterOutList;
    int[][] activatedSrConstraintIndexList;
    int srCutNum;

    private void branchAndBound(GTKP_LS_Heu_Solver labelSettingSolver) {

        fatWeightArr = labelSettingSolver.fatWeightArr;
        eachItemDifIndexList = labelSettingSolver.eachItemDifIndexList;
        eachItemSameIndexList = labelSettingSolver.eachItemSameIndexList;
        eachItemOutAfterOutList = labelSettingSolver.eachItemOutAfterOutList;
        activatedSrConstraintIndexList = labelSettingSolver.activatedSrConstraintIndexList;
        srCutNum = labelSettingSolver.srCutNum;

        init_packedItems = new Item[curN];
        initLabelNode = new LabelNode();
        initLabelNode.remainingCapacity = S;
        initLabelNode.itemUnEnableLabel = new long[Math.max((curN + LONG_SIZE - 1) / LONG_SIZE, 1)];
        initLabelNode.srCutLabel = new long[Math.max((srCutNum + LONG_SIZE - 1) / LONG_SIZE, 1)];

        redQuick();

        long step = Long.MAX_VALUE;
        for (LongValue_Item item : items) step = Math.min(step, item.value);
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
    public long UB, LB;
    public long UB0_KP, UB0_LS, UB0, LB0;
    public long lb0Time, ub0kpTime, ub0lsTime, redTime;
    LongValue_Item[] items;
    int W, H, S;
    long objLB;
    public List<PlaceItem> bestPlaceItemList;
    public int I_in, I_out;
    public int Ed_Pie, Es_Pie, SR_Pie;
    int[] fat;
    SR_Cut[] srCutList;
    List<int[]> conflictList;

    public void solve(int n, int initW, int initH, LongValue_Item[] init_items,
                      int[] init_fat, List<int[]> init_conflictList, List<int[]> bindList, List<SR_Cut> init_srCutList) {

        this.n = n;
        this.items = init_items;
        this.curN = init_items.length;
        this.W = initW;
        this.H = initH;
        this.fat = init_fat;
        this.conflictList = init_conflictList;

        // 问题缩减
        getReducedItemList();
        getReducedActivateSrCutList(init_srCutList);
        preprocessing();

        // 排序
        Arrays.sort(items, (o1, o2) -> {
            int c = -Double.compare(o1.unitValue, o2.unitValue);
            return c == 0 ? -Long.compare(o1.value, o2.value) : c;
        });
        for (int i = 0; i < curN; i++) items[i].index = i;

        boolean[] have = new boolean[n];
        Integer[] idIndexMap = new Integer[n];
        for (LongValue_Item item : items) {
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
        Ed_Pie = conflictList.size();

        // 修改绑定约束
        List<int[]> newBindList = new ArrayList<>();
        for (int[] bind : bindList) {
            if (have[bind[0]] && have[bind[1]]) {
                bind[0] = idIndexMap[bind[0]];
                bind[1] = idIndexMap[bind[1]];
                newBindList.add(bind);
            }
        }
        bindList = newBindList;
        UnionFind uf = new UnionFind(curN);
        for (int[] bind : bindList) uf.union(bind[0], bind[1]);
        fat = uf.fat.clone();
        for (int i = 0; i < curN; i++) fat[i] = uf.find(i);
        Es_Pie = bindList.size();

        // 修改SR约束
        for (SR_Cut srCut : srCutList) {
            int[] indexs = srCut.indexs;
            for (int i = 0; i < 3; i++) {
                Integer j = idIndexMap[indexs[i]];
                if (j == null) {
                    indexs[i] = -1;
                } else {
                    indexs[i] = j;
                }
            }
        }

        // Compute UB0
        ub0kpTime = System.currentTimeMillis();
        computeUpperBound();
        ub0kpTime = System.currentTimeMillis() - ub0kpTime;

        // Compute LB0
        lb0Time = System.currentTimeMillis();
        GTKP_LS_Heu_Solver lsHeuSolver = new GTKP_LS_Heu_Solver(n, curN, S, W, H, items, conflictList, fat, srCutList);
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
            UB0_LS = new GTKP_LS_UB_Solver(lsHeuSolver).solve();
            UB0 = Math.min(UB0, UB0_LS);
            UB = UB0;
        } catch (EarlyTerminationException e) {
        }
        ub0lsTime = System.currentTimeMillis() - ub0lsTime;

        if (UB == LB) return;

        // 运行 BB 算法
        try {
            branchAndBound(lsHeuSolver);
        } catch (EarlyTerminationException e) {
        }
    }

}