package com.wskh.solvers.GTKP;

import com.wskh.classes.*;
import com.wskh.utils.CommonUtil;
import com.wskh.utils.SkyLineUtil;
import com.wskh.utils.TimeUtil;

import java.util.*;

public class GTKP_LS_Heu_Solver {
    final int LONG_SIZE = 64;
    long minReducedCost;
    int maxLabelCnt = 400000;
    List<PlaceItem> bestPlaceItemList;
    public int[][] eachItemDifIndexList;
    public int[][] eachItemOutAfterOutList;
    public int[][] eachItemSameIndexList;
    public int[][] activatedSrConstraintIndexList;
    public HashSet<Integer>[] dominateArr1;
    public HashSet<Integer>[] dominateArr2;
    int[] fatWeightArr;

    int n, curN, W, H, C;
    LongValue_Item[] items;
    List<int[]> conflictList;
    int[] fat;
    SR_Cut[] srCutList;
    int srCutNum;

    public GTKP_LS_Heu_Solver(int n, int curN, int C, int W, int H, LongValue_Item[] items,
                              List<int[]> conflictList, int[] fat, SR_Cut[] srCutList) {
        this.n = n;
        this.curN = curN;
        this.W = W;
        this.H = H;
        this.C = C;
        this.items = items;
        this.conflictList = conflictList;
        this.fat = fat;
        this.srCutList = srCutList;
        srCutNum = srCutList.length;
    }

    static class LabelNode implements Comparable<LabelNode> {
        public long[] itemUnEnableLabel;
        public long[] srCutLabel;
        public long reducedCost;
        public long minReducedCost;
        public int remainingCapacity;
        public List<Item> packedItemList;

        @Override
        public int compareTo(LabelNode o) {
            if (reducedCost < o.reducedCost) {
                return -1;
            } else if (reducedCost > o.reducedCost) {
                return 1;
            } else if (remainingCapacity < o.remainingCapacity) {
                return 1;
            } else if (remainingCapacity > o.remainingCapacity) {
                return -1;
            } else if (minReducedCost < o.minReducedCost) {
                return -1;
            } else if (minReducedCost > o.minReducedCost) {
                return 1;
            }
            return 0;
        }
    }

    private boolean dominateJudge(LongValue_Item itemI, LongValue_Item itemJ, int i, int j, int[][] maximumConnectedComponents, HashSet<Integer>[] eachItemDifIndexList) {
        if (itemI.w <= itemJ.w && itemI.h <= itemJ.h && itemI.value >= itemJ.value && maximumConnectedComponents[i].length == 1) {
            long v = itemI.value;
            for (int jIndexPie : maximumConnectedComponents[j]) {
                v -= items[jIndexPie].value;
            }
            for (int iIndexPie : eachItemDifIndexList[i]) {
                if (iIndexPie != j) {
                    v -= items[iIndexPie].value;
                }
            }
            boolean[] out = new boolean[curN];
            out[i] = true;
            out[j] = true;
            for (int k : eachItemDifIndexList[i]) out[k] = true;
            for (SR_Cut srCut : srCutList) {
                boolean isIInSrCut = false;
                int cnt = 0;
                for (int srCutItemIndex : srCut.indexs) {
                    if (srCutItemIndex == -1) continue;
                    if (srCutItemIndex == i) {
                        isIInSrCut = true;
                    } else {
                        if (!out[srCutItemIndex]) cnt++;
                    }
                }
                if (isIInSrCut && cnt >= 1) {
                    v -= srCut.penalty;
                }
            }
            // itemI dominate itemJ
            return v >= 0;
        }
        return false;
    }

    private void init() {

        // 基础绑定（same图的最大连通分量）
        int[][] maximumConnectedComponents = new int[curN][];
        for (int i = 0; i < curN; i++) {
            List<Integer> list = new ArrayList<>(curN);
            for (int j = 0; j < curN; j++) if (fat[i] == fat[j]) list.add(j);
            int[] arr = new int[list.size()];
            int a = 0;
            for (int index : list) arr[a++] = index;
            maximumConnectedComponents[i] = arr;
        }

        // 基础冲突
        HashSet<Integer>[] itemDifferentSetArr = new HashSet[curN];
        for (int i = 0; i < curN; i++) itemDifferentSetArr[i] = new HashSet<>();
        for (int[] conflict : conflictList) {
            int i = conflict[0];
            int j = conflict[1];
            itemDifferentSetArr[i].add(j);
            itemDifferentSetArr[j].add(i);
        }
        for (int i = 0; i < curN; i++) {
            while (true) {
                HashSet<Integer> set = itemDifferentSetArr[i];
                int oldSize = set.size();
                for (int index : new ArrayList<>(set)) {
                    for (int j : maximumConnectedComponents[index]) set.add(j);
                }
                if (oldSize == set.size()) break;
            }
        }

        // 支配关系判断
        dominateArr1 = new HashSet[curN];
        dominateArr2 = new HashSet[curN];
        for (int i = 0; i < curN; i++) {
            dominateArr1[i] = new HashSet<>();
            dominateArr2[i] = new HashSet<>();
        }
        for (int i = 0; i < curN; i++) {
            LongValue_Item itemI = items[i];
            for (int j = i + 1; j < curN; j++) {
                LongValue_Item itemJ = items[j];
                if (dominateJudge(itemI, itemJ, i, j, maximumConnectedComponents, itemDifferentSetArr)) {
                    dominateArr1[i].add(j);
                    dominateArr2[j].add(i);
                } else if (dominateJudge(itemJ, itemI, j, i, maximumConnectedComponents, itemDifferentSetArr)) {
                    dominateArr1[j].add(i);
                    dominateArr2[i].add(j);
                }
            }
        }

        // 计算放了i之后必须放的物品index
        eachItemSameIndexList = new int[curN][];
        for (int i = 0; i < curN; i++) {
            Set<Integer> set = new HashSet<>();
            set.add(i);
            while (true) {
                int size = set.size();
                for (int index : new ArrayList<>(set)) {
                    set.addAll(dominateArr2[index]);
                    for (int k : maximumConnectedComponents[index]) set.add(k);
                }
                if (set.size() == size) break;
            }
            int[] arr = new int[set.size()];
            int a = 0;
            for (int index : set) arr[a++] = index;
            eachItemSameIndexList[i] = arr;
        }

        // 计算不放i之后不能放的物品
        eachItemOutAfterOutList = new int[curN][];
        for (int i = 0; i < curN; i++) {
            Set<Integer> set = new HashSet<>();
            set.add(i);
            while (true) {
                int size = set.size();
                ArrayList<Integer> copy = new ArrayList<>(set);
                for (int index : copy) {
                    set.addAll(dominateArr1[index]);
                    for (int k : maximumConnectedComponents[index]) set.add(k);
                }
                if (set.size() == size) break;
            }
            int[] arr = new int[set.size()];
            int a = 0;
            for (int index : set) arr[a++] = index;
            eachItemOutAfterOutList[i] = arr;
        }

        // 计算放了i之后不能放的物品index
        for (int i = 0; i < curN; i++) {
            Item itemI = items[i];
            for (int j = i + 1; j < curN; j++) {
                Item itemJ = items[j];
                if (itemJ.w + itemI.w > W && itemJ.h + itemI.h > H) {
                    itemDifferentSetArr[i].add(j);
                    itemDifferentSetArr[j].add(i);
                }
            }
        }
        eachItemDifIndexList = new int[curN][];
        for (int i = 0; i < curN; i++) {
            Set<Integer> set = new HashSet<>();
            for (int j : eachItemSameIndexList[i]) set.addAll(itemDifferentSetArr[j]);
            while (true) {
                int oldSize = set.size();
                for (int index : new ArrayList<>(set)) {
                    for (int k : eachItemOutAfterOutList[index]) set.add(k);
                }
                if (set.size() == oldSize) break;
            }
            int[] arr = new int[set.size()];
            int a = 0;
            for (int index : set) arr[a++] = index;
            eachItemDifIndexList[i] = arr;
        }

        fatWeightArr = new int[curN];
        for (int i = 0; i < curN; i++) {
            for (int j : maximumConnectedComponents[i]) fatWeightArr[i] += items[j].s;
        }

        // SR cuts
        List<List<Integer>> temp_activatedSrConstraintIndexList = new ArrayList<>(curN);
        for (int i = 0; i < curN; i++) temp_activatedSrConstraintIndexList.add(new ArrayList<>());
        for (int r = 0; r < srCutNum; r++) {
            for (int index : srCutList[r].indexs) {
                if (index != -1) temp_activatedSrConstraintIndexList.get(index).add(r);
            }
        }
        activatedSrConstraintIndexList = new int[curN][];
        for (int i = 0; i < curN; i++) {
            List<Integer> list = temp_activatedSrConstraintIndexList.get(i);
            int[] arr = new int[list.size()];
            int a = 0;
            for (int index : list) arr[a++] = index;
            activatedSrConstraintIndexList[i] = arr;
        }

    }

    private void calcMinReducedCostAndMinWasteByRelaxKnapsack(int a, LabelNode node) {
        int remainingCapacity = node.remainingCapacity;
        long minReducedCost = node.reducedCost;
        for (; a < curN; a++) {
            int pos1_a = a / LONG_SIZE;
            int pos2_a = a % LONG_SIZE;
            long val_a = 1L << pos2_a;
            if ((node.itemUnEnableLabel[pos1_a] & val_a) == 0) {
                LongValue_Item item = items[a];
                if (item.s <= remainingCapacity) {
                    minReducedCost -= item.value;
                    remainingCapacity -= item.s;
                } else {
                    minReducedCost -= CommonUtil.ceilToLong(item.unitValue * remainingCapacity);
                    break;
                }
            }
        }
        node.minReducedCost = minReducedCost;
    }

    private List<PlaceItem> opp(List<Item> itemList) {
        ArrayList<SkyLine> skyLines = new ArrayList<>();
        skyLines.add(new SkyLine(0, 0, W));
        itemList.sort(Item.itemComparatorByDecreaseSWH);
        List<PlaceItem> placeItemList = SkyLineUtil.skyLineIteration(H, skyLines, new ArrayList<>(itemList));
        if (placeItemList.size() == itemList.size()) return placeItemList;
        return null;
    }

    private int cmpNodeDominate(int idx, LabelNode a, LabelNode b) {
        if (a.reducedCost <= b.minReducedCost) return 1;
        if (b.reducedCost <= a.minReducedCost) return -1;

        int remainingCapacityCmpAB = Long.compare(a.remainingCapacity, b.remainingCapacity);
        int reducedCostCmpAB = Long.compare(a.reducedCost, b.reducedCost);

        if (remainingCapacityCmpAB >= 0 && reducedCostCmpAB <= 0) {
            long v = a.reducedCost - b.reducedCost;
            for (int r = 0; r < srCutNum; r++) {
                int pos1_r = r / LONG_SIZE;
                int pos2_r = r % LONG_SIZE;
                long val_r = 1L << pos2_r;
                if ((a.srCutLabel[pos1_r] & val_r) == val_r && (b.srCutLabel[pos1_r] & val_r) == 0)
                    v += srCutList[r].penalty;
//                if (a.getCut(r) == 1 && b.getCut(r) == 0) v += srCutList.get(r).penalty;
            }
            for (int i = idx; i < curN; i++) {
                int pos1_i = i / LONG_SIZE;
                int pos2_i = i % LONG_SIZE;
                long val_i = 1L << pos2_i;
                if ((b.itemUnEnableLabel[pos1_i] & val_i) == 0 && (a.itemUnEnableLabel[pos1_i] & val_i) == val_i)
                    v += items[i].value;
//                if (b.getCons(i) == 0 && a.getCons(i) == 1) v += items[i].value;
            }
            if (v <= 0) return 1;
        }

        if (remainingCapacityCmpAB <= 0 && reducedCostCmpAB >= 0) {
            long v = b.reducedCost - a.reducedCost;
            for (int r = 0; r < srCutNum; r++) {
                int pos1_r = r / LONG_SIZE;
                int pos2_r = r % LONG_SIZE;
                long val_r = 1L << pos2_r;
                if ((b.srCutLabel[pos1_r] & val_r) == val_r && (a.srCutLabel[pos1_r] & val_r) == 0)
                    v += srCutList[r].penalty;
            }
            for (int i = idx; i < curN; i++) {
                int pos1_i = i / LONG_SIZE;
                int pos2_i = i % LONG_SIZE;
                long val_i = 1L << pos2_i;
                if ((a.itemUnEnableLabel[pos1_i] & val_i) == 0 && (b.itemUnEnableLabel[pos1_i] & val_i) == val_i)
                    v += items[i].value;
            }
            if (v <= 0) return -1;
        }

        return 0;
    }

    private List<LabelNode> removeDominatedNode(int a, List<LabelNode> labelNodeList) {
        int size = labelNodeList.size();
        Collections.sort(labelNodeList);
        boolean[] delFlag = new boolean[size];
        long minReducedCost = labelNodeList.getFirst().reducedCost;
        List<LabelNode> newLabelSetNodeList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            LabelNode labelNodeI = labelNodeList.get(i);
            if (!delFlag[i]) {
                if (labelNodeI.minReducedCost >= minReducedCost) {
                    delFlag[i] = true;
                    continue;
                }

                int j = i + 1;
                if (j < size && !delFlag[j]) {
                    int cmp = cmpNodeDominate(a, labelNodeI, labelNodeList.get(j));
                    if (cmp == -1) {
                        delFlag[i] = true;
                    } else if (cmp == 1) {
                        delFlag[j] = true;
                    }
                }

            }
            if (!delFlag[i]) {
                newLabelSetNodeList.add(labelNodeI);
                // 800000、400000*、200000、100000、30000、300000
                if (newLabelSetNodeList.size() == maxLabelCnt) {
                    maxLabelCnt = Math.max(1, (int) (maxLabelCnt * 0.8));
                    break;
                }
            }
        }
        return newLabelSetNodeList;
    }

    private void labelSetting() {
        List<LabelNode> currentLabelNodeList = new ArrayList<>();
        List<LabelNode> nextLabelNodeList = new ArrayList<>();

        LabelNode firstNode = new LabelNode();
        firstNode.remainingCapacity = C;
        firstNode.reducedCost = 0;
        firstNode.itemUnEnableLabel = new long[Math.max((curN + LONG_SIZE - 1) / LONG_SIZE, 1)];
        firstNode.srCutLabel = new long[Math.max((srCutNum + LONG_SIZE - 1) / LONG_SIZE, 1)];
        firstNode.packedItemList = new ArrayList<>(curN);

        // 检验是否有矛盾
        for (int a = 0; a < curN; a++) {
            boolean[] bs = new boolean[curN];
            for (int j : eachItemSameIndexList[a]) bs[j] = true;
            for (int j : eachItemDifIndexList[a]) {
                if (bs[j]) {
                    for (int i : eachItemOutAfterOutList[a]) {
                        int pos1_i = i / LONG_SIZE;
                        int pos2_i = i % LONG_SIZE;
                        long val_i = 1L << pos2_i;
                        firstNode.itemUnEnableLabel[pos1_i] |= val_i;
                    }
                }
            }
        }

        calcMinReducedCostAndMinWasteByRelaxKnapsack(0, firstNode);
        // addNode
        if (bestPlaceItemList == null || firstNode.minReducedCost < minReducedCost) currentLabelNodeList.add(firstNode);

        int a = 0;
        while (a < curN) {
            if (!currentLabelNodeList.isEmpty()) {

                if (TimeUtil.isTimeLimit()) throw new EarlyTerminationException();

                currentLabelNodeList = removeDominatedNode(a, currentLabelNodeList);

                System.out.print(currentLabelNodeList.size() + ",");

                int pos1_a = a / LONG_SIZE;
                int pos2_a = a % LONG_SIZE;
                long val_a = 1L << pos2_a;

                for (LabelNode labelNode : currentLabelNodeList) {
                    // 打包
                    if (labelNode.remainingCapacity >= fatWeightArr[a] && (labelNode.itemUnEnableLabel[pos1_a] & val_a) == 0) {
                        List<Item> newPackedItemList = new ArrayList<>(labelNode.packedItemList);
                        LabelNode newLabelNode = new LabelNode();
                        newLabelNode.itemUnEnableLabel = labelNode.itemUnEnableLabel.clone();
                        newLabelNode.srCutLabel = labelNode.srCutLabel.clone();
                        newLabelNode.reducedCost = labelNode.reducedCost;
                        newLabelNode.remainingCapacity = labelNode.remainingCapacity;
                        // 将与 idx 绑定的物品 i 全部打包
                        for (int i : eachItemSameIndexList[a]) {
                            int pos1_i = i / LONG_SIZE;
                            int pos2_i = i % LONG_SIZE;
                            long val_i = 1L << pos2_i;
                            if ((newLabelNode.itemUnEnableLabel[pos1_i] & val_i) == 0) {
                                LongValue_Item itemI = items[i];
                                newLabelNode.remainingCapacity -= itemI.s;
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
                                        newLabelNode.reducedCost += srCut.penalty;
                                    }
                                }
                                newPackedItemList.add(itemI);
                                newLabelNode.reducedCost -= itemI.value;
                                newLabelNode.itemUnEnableLabel[pos1_i] |= val_i;
                                for (int j : eachItemDifIndexList[i]) {
                                    int pos1_j = j / LONG_SIZE;
                                    int pos2_j = j % LONG_SIZE;
                                    long val_j = 1L << pos2_j;
                                    newLabelNode.itemUnEnableLabel[pos1_j] |= val_j;
                                }
                            }
                        }
                        if (newLabelNode.remainingCapacity >= 0) {
                            List<PlaceItem> placeItemList = opp(newPackedItemList);
                            if (placeItemList != null) {
                                calcMinReducedCostAndMinWasteByRelaxKnapsack(a + 1, newLabelNode);
                                // addNode
                                if (bestPlaceItemList == null || newLabelNode.minReducedCost < minReducedCost) {
                                    newLabelNode.packedItemList = newPackedItemList;
                                    nextLabelNodeList.add(newLabelNode);
                                    if (bestPlaceItemList == null || newLabelNode.reducedCost < minReducedCost) {
                                        bestPlaceItemList = placeItemList;
                                        minReducedCost = newLabelNode.reducedCost;
                                    }
                                }
                            }
                        }
                    }

                    // 不打包
                    if ((labelNode.itemUnEnableLabel[pos1_a] & val_a) == 0) {
                        for (int i : eachItemOutAfterOutList[a]) {
                            int pos1_i = i / LONG_SIZE;
                            int pos2_i = i % LONG_SIZE;
                            long val_i = 1L << pos2_i;
                            labelNode.itemUnEnableLabel[pos1_i] |= val_i;
                        }
                    }
                    calcMinReducedCostAndMinWasteByRelaxKnapsack(a + 1, labelNode);
                    // addNode
                    if (bestPlaceItemList == null || labelNode.minReducedCost < minReducedCost)
                        nextLabelNodeList.add(labelNode);
                }

                currentLabelNodeList = nextLabelNodeList;
                nextLabelNodeList = new ArrayList<>();

            }
            a++;
        }

        System.out.println();

    }

    public void solve() {
        init();
        labelSetting();
    }

}