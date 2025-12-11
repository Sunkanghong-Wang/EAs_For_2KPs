package com.wskh.solvers.GTKP;

import com.wskh.classes.*;
import com.wskh.utils.CommonUtil;
import com.wskh.utils.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GTKP_LS_UB_Solver {
    final int LONG_SIZE = 64;
    long minReducedCost;
    public int[][] eachItemDifIndexList;
    public int[][] eachItemOutAfterOutList;
    public int[][] eachItemSameIndexList;
    public int[][] activatedSrConstraintIndexList;
    int[] fatWeightArr;

    int n, curN, W, H, C;
    LongValue_Item[] items;
    SR_Cut[] srCutList;
    int srCutNum;
    int[] fat;

    public GTKP_LS_UB_Solver(GTKP_LS_Heu_Solver lsHeuSolver) {
        this.n = lsHeuSolver.n;
        this.curN = lsHeuSolver.curN;
        this.W = lsHeuSolver.W;
        this.H = lsHeuSolver.H;
        this.C = lsHeuSolver.C;

        this.items = lsHeuSolver.items;
        this.srCutList = lsHeuSolver.srCutList;
        this.srCutNum = lsHeuSolver.srCutNum;
        this.fat = lsHeuSolver.fat;
        this.fatWeightArr = lsHeuSolver.fatWeightArr;
        this.eachItemDifIndexList = lsHeuSolver.eachItemDifIndexList;
        this.eachItemSameIndexList = lsHeuSolver.eachItemSameIndexList;
        this.eachItemOutAfterOutList = lsHeuSolver.eachItemOutAfterOutList;
        this.activatedSrConstraintIndexList = lsHeuSolver.activatedSrConstraintIndexList;
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
                if (newLabelSetNodeList.size() == 400000) {
                    System.out.println("Exceed 400000");
                    throw new EarlyTerminationException();
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
        if (firstNode.minReducedCost < minReducedCost) currentLabelNodeList.add(firstNode);

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
                            calcMinReducedCostAndMinWasteByRelaxKnapsack(a + 1, newLabelNode);
                            // addNode
                            if (newLabelNode.minReducedCost < minReducedCost) {
                                newLabelNode.packedItemList = newPackedItemList;
                                nextLabelNodeList.add(newLabelNode);
                                if (newLabelNode.reducedCost < minReducedCost) {
                                    minReducedCost = newLabelNode.reducedCost;
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
                    if (labelNode.minReducedCost < minReducedCost)
                        nextLabelNodeList.add(labelNode);
                }

                currentLabelNodeList = nextLabelNodeList;
                nextLabelNodeList = new ArrayList<>();

            }
            a++;
        }

        System.out.println();

    }

    public long solve() {
        labelSetting();
        return -minReducedCost;
    }

}