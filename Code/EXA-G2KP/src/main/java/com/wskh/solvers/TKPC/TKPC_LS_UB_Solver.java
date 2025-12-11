package com.wskh.solvers.TKPC;

import com.wskh.classes.*;
import com.wskh.solvers.GTKP.GTKP_LS_Heu_Solver;
import com.wskh.utils.CommonUtil;
import com.wskh.utils.TimeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TKPC_LS_UB_Solver {
    final int LONG_SIZE = 64;
    int minReducedCost;
    public int[][] eachItemDifIndexList;
    public int[][] eachItemOutAfterOutList;
    public int[][] eachItemSameIndexList;

    int n, curN, W, H, C;
    IntValue_Item[] items;

    public TKPC_LS_UB_Solver(TKPC_LS_Heu_Solver lsHeuSolver) {
        this.n = lsHeuSolver.n;
        this.curN = lsHeuSolver.curN;
        this.W = lsHeuSolver.W;
        this.H = lsHeuSolver.H;
        this.C = lsHeuSolver.C;

        this.items = lsHeuSolver.items;
        this.eachItemDifIndexList = lsHeuSolver.eachItemDifIndexList;
        this.eachItemSameIndexList = lsHeuSolver.eachItemSameIndexList;
        this.eachItemOutAfterOutList = lsHeuSolver.eachItemOutAfterOutList;
    }

    static class LabelNode implements Comparable<LabelNode> {
        public long[] itemUnEnableLabel;
        public int reducedCost;
        public int minReducedCost;
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
        int minReducedCost = node.reducedCost;
        for (; a < curN; a++) {
            int pos1_a = a / LONG_SIZE;
            int pos2_a = a % LONG_SIZE;
            long val_a = 1L << pos2_a;
            if ((node.itemUnEnableLabel[pos1_a] & val_a) == 0) {
                IntValue_Item item = items[a];
                if (item.s <= remainingCapacity) {
                    minReducedCost -= item.value;
                    remainingCapacity -= item.s;
                } else {
                    minReducedCost -= CommonUtil.ceilToInt(item.unitValue * remainingCapacity);
                    break;
                }
            }
        }
        node.minReducedCost = minReducedCost;
    }

    private int cmpNodeDominate(int idx, LabelNode a, LabelNode b) {
        if (a.reducedCost <= b.minReducedCost) return 1;
        if (b.reducedCost <= a.minReducedCost) return -1;

        int remainingCapacityCmpAB = Integer.compare(a.remainingCapacity, b.remainingCapacity);
        int reducedCostCmpAB = Integer.compare(a.reducedCost, b.reducedCost);

        if (remainingCapacityCmpAB >= 0 && reducedCostCmpAB <= 0) {
            int v = a.reducedCost - b.reducedCost;
            for (int i = idx; i < curN; i++) {
                int pos1_i = i / LONG_SIZE;
                int pos2_i = i % LONG_SIZE;
                long val_i = 1L << pos2_i;
                if ((b.itemUnEnableLabel[pos1_i] & val_i) == 0 && (a.itemUnEnableLabel[pos1_i] & val_i) == val_i)
                    v += items[i].value;
            }
            if (v <= 0) return 1;
        }

        if (remainingCapacityCmpAB <= 0 && reducedCostCmpAB >= 0) {
            int v = b.reducedCost - a.reducedCost;
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
        int minReducedCost = labelNodeList.getFirst().reducedCost;
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
                    if (labelNode.remainingCapacity >= items[a].s && (labelNode.itemUnEnableLabel[pos1_a] & val_a) == 0) {
                        List<Item> newPackedItemList = new ArrayList<>(labelNode.packedItemList);
                        LabelNode newLabelNode = new LabelNode();
                        newLabelNode.itemUnEnableLabel = labelNode.itemUnEnableLabel.clone();
                        newLabelNode.reducedCost = labelNode.reducedCost;
                        newLabelNode.remainingCapacity = labelNode.remainingCapacity;
                        // 将与 idx 绑定的物品 i 全部打包
                        for (int i : eachItemSameIndexList[a]) {
                            int pos1_i = i / LONG_SIZE;
                            int pos2_i = i % LONG_SIZE;
                            long val_i = 1L << pos2_i;
                            if ((newLabelNode.itemUnEnableLabel[pos1_i] & val_i) == 0) {
                                IntValue_Item itemI = items[i];
                                newLabelNode.remainingCapacity -= itemI.s;
                                if (newLabelNode.remainingCapacity < 0) break;
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

    public int solve() {
        labelSetting();
        return -minReducedCost;
    }

}