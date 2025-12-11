package com.wskh.solvers.TOPP;

import com.wskh.classes.EarlyTerminationException;
import com.wskh.classes.Item;
import com.wskh.classes.PlaceItem;
import com.wskh.utils.CommonUtil;
import com.wskh.utils.TimeUtil;
import lombok.AllArgsConstructor;

import java.util.*;

public class PPP_Solver {
    @AllArgsConstructor
    static class StairLine {
        int x, y, len, hLimit;
    }

    // 定义双向链表的节点类
    static class DoublyLinkedNode<T> {
        T value;         // 节点存储的值
        DoublyLinkedNode<T> prev;    // 指向前一个节点
        DoublyLinkedNode<T> next;    // 指向后一个节点

        public DoublyLinkedNode(T initValue) {
            value = initValue;
        }
    }

    static class DoublyLinkedList<T> implements Iterable<DoublyLinkedNode<T>> {
        private DoublyLinkedNode<T> head; // 链表头节点
        private DoublyLinkedNode<T> tail; // 链表尾节点
        private int size;     // 链表长度

        public DoublyLinkedList(T value) {
            head = new DoublyLinkedNode<>(value);
            tail = head;
            size = 1;
        }

        public DoublyLinkedList(T[] values) {
            size = values.length;
            head = new DoublyLinkedNode<>(values[0]);
            tail = head;
            for (int i = 1; i < values.length; i++) {
                DoublyLinkedNode<T> newNode = new DoublyLinkedNode<>(values[i]);
                newNode.prev = tail;
                tail.next = newNode;
                tail = newNode;
            }
        }

        // 删除传入的元素
        public void remove(DoublyLinkedNode<T> current) {
            if (size == 1) {
                // 头尾节点
                head = null;
                tail = null;
            } else if (current.prev == null) {
                // 头节点
                current.next.prev = null;
                head = current.next;
            } else if (current.next == null) {
                // 尾节点
                current.prev.next = null;
                tail = current.prev;
            } else {
                // 中间节点
                current.prev.next = current.next;
                current.next.prev = current.prev;
            }
            size--;
        }

        // 在current前面插入一个元素
        public DoublyLinkedNode<T> insert(T value, DoublyLinkedNode<T> current) {
            DoublyLinkedNode<T> newNode = new DoublyLinkedNode<>(value);
            if (current.prev == null) {
                // 头节点
                current.prev = newNode;
                newNode.next = current;
                head = newNode;
            } else {
                // 中间节点
                newNode.prev = current.prev;
                newNode.next = current;
                current.prev.next = newNode;
                current.prev = newNode;
            }
            size++;
            return newNode;
        }

        // 把newNode插入到原本的位置
        public void insert(DoublyLinkedNode<T> newNode) {
            if (newNode.prev == null) {
                // 头节点
                head.prev = newNode;
                newNode.next = head;
                head = newNode;
            } else if (newNode.next == null) {
                // 尾节点
                tail.next = newNode;
                newNode.prev = tail;
                tail = newNode;
            } else {
                // 中间节点
                newNode.prev.next = newNode;
                newNode.next.prev = newNode;
            }
            size++;
        }

        // 实现 Iterable 接口的 iterator() 方法
        @Override
        public Iterator<DoublyLinkedNode<T>> iterator() {
            return new DoublyLinkedListIterator();
        }

        // 自定义迭代器
        private class DoublyLinkedListIterator implements Iterator<DoublyLinkedNode<T>> {
            private DoublyLinkedNode<T> current = head; // 当前节点

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public DoublyLinkedNode<T> next() {
                if (current == null) throw new NoSuchElementException();
                DoublyLinkedNode<T> node = current;
                current = current.next;
                return node;
            }
        }

    }

    private void packing(DoublyLinkedNode<Item> itemNode, DoublyLinkedNode<StairLine> stairLineNode, DoublyLinkedList<Item> unPackedItemList, DoublyLinkedList<StairLine> stairLineList, PlaceItem[] placeItems, int packedItemNum) {
        StairLine stairLine = stairLineNode.value;
        int stairLineX = stairLine.x;
        int stairLineY = stairLine.y;
        int stairLineLen = stairLine.len;
        int stairLineHLimit = stairLine.hLimit;
        Item item = itemNode.value;
        int itemW = item.w;
        int itemH = item.h;
        if (itemH <= stairLineHLimit && itemW <= stairLineLen) {
            // 打包物品到该阶梯线上
            if (itemH < stairLineHLimit && itemW < stairLineLen) {

                placeItems[packedItemNum++] = item.packed(stairLineX, stairLineY);
                unPackedItemList.remove(itemNode);

                DoublyLinkedNode<StairLine> newNode = stairLineList.insert(new StairLine(stairLineX, stairLineY + itemH, itemW, stairLineHLimit - itemH), stairLineNode);
                stairLine.x += itemW;
                stairLine.hLimit = itemH;
                stairLine.len -= itemW;

                dfs(stairLineList, placeItems, packedItemNum, unPackedItemList);

                stairLine.x -= itemW;
                stairLine.hLimit = stairLineHLimit;
                stairLine.len += itemW;
                stairLineList.remove(newNode);

                unPackedItemList.insert(itemNode);

            } else if (itemH == stairLineHLimit && itemW == stairLineLen) {

                placeItems[packedItemNum++] = item.packed(stairLineX, stairLineY);
                unPackedItemList.remove(itemNode);

                if (stairLineNode.prev != null) stairLineNode.prev.value.len += itemW;
                if (stairLineNode.next != null) stairLineNode.next.value.hLimit += itemH;
                stairLineList.remove(stairLineNode);

                dfs(stairLineList, placeItems, packedItemNum, unPackedItemList);

                stairLineList.insert(stairLineNode);
                if (stairLineNode.prev != null) stairLineNode.prev.value.len -= itemW;
                if (stairLineNode.next != null) stairLineNode.next.value.hLimit -= itemH;

                unPackedItemList.insert(itemNode);

            } else if (itemH == stairLineHLimit) {

                placeItems[packedItemNum++] = item.packed(stairLineX, stairLineY);
                unPackedItemList.remove(itemNode);

                if (stairLineNode.prev != null) stairLineNode.prev.value.len += itemW;
                stairLine.x += itemW;
                stairLine.len -= itemW;

                dfs(stairLineList, placeItems, packedItemNum, unPackedItemList);

                if (stairLineNode.prev != null) stairLineNode.prev.value.len -= itemW;
                stairLine.x -= itemW;
                stairLine.len += itemW;

                unPackedItemList.insert(itemNode);

            } else {
                // item.w == stairLine.len

                placeItems[packedItemNum++] = item.packed(stairLineX, stairLineY);
                unPackedItemList.remove(itemNode);

                stairLine.hLimit -= itemH;
                stairLine.y += itemH;
                if (stairLineNode.next != null) stairLineNode.next.value.hLimit += itemH;

                dfs(stairLineList, placeItems, packedItemNum, unPackedItemList);

                stairLine.hLimit += itemH;
                stairLine.y -= itemH;
                if (stairLineNode.next != null) stairLineNode.next.value.hLimit -= itemH;

                unPackedItemList.insert(itemNode);

            }
        }
    }

    public long generatedNodes;
    public long exploredNodes;
    int maxStairLineCnt;
    Set<String> set;
    int beta;
    int delta = 1, q = 4;
    int dpCutFailCnt;

    private void dfs(DoublyLinkedList<StairLine> stairLineList, PlaceItem[] placeItems, int packedItemNum, DoublyLinkedList<Item> unPackedItemList) {
        if (packedItemNum == curN) {
            feasiblePlaceItemList = new ArrayList<>(Arrays.asList(placeItems));
            throw new EarlyTerminationException();
        } else {

            // Fathoming

            // 简单规则 1 unPackedItemList.size < stairLineList.size ||
            if (stairLineList.size > maxStairLineCnt || stairLineList.size == 0)
                return;
            // 简单规则 3
            if (packedItemNum > 0) {
                boolean greater = false;
                int index = placeItems[0].index;
                for (DoublyLinkedNode<Item> itemNode : unPackedItemList) {
                    if (itemNode.value.index > index) {
                        greater = true;
                        break;
                    }
                }
                if (!greater) return;
            }
            // 简单规则 2
            for (DoublyLinkedNode<Item> itemNode : unPackedItemList) {
                boolean canPackAble = false;
                int h = itemNode.value.h;
                for (DoublyLinkedNode<StairLine> stairLineNode : stairLineList) {
                    if (h + stairLineNode.value.y <= H) {
                        canPackAble = true;
                        break;
                    }
                }
                if (!canPackAble) return;
            }

            // 去除重复方案
            StringBuilder stringBuilder = new StringBuilder();
            for (DoublyLinkedNode<StairLine> stairLineDoublyLinkedNode : stairLineList) {
                StairLine stairLine = stairLineDoublyLinkedNode.value;
                stringBuilder.append(stairLine.y).append("a").append(stairLine.len);
            }
            stringBuilder.append("b");
            for (DoublyLinkedNode<Item> itemNode : unPackedItemList) {
                Item item = itemNode.value;
                stringBuilder.append(item.w).append("a").append(item.h);
            }
            if (!set.add(stringBuilder.toString())) return;

            // DP-Cut
            if (packedItemNum >= beta) {
                // X-DP-Cut
                int maxW = W - stairLineList.head.value.x;
                int[] dp = new int[maxW + 1];
                for (DoublyLinkedNode<Item> itemNode : unPackedItemList) {
                    int w = itemNode.value.w;
                    // 遍历背包容量
                    for (int j = maxW; j >= w; j--) dp[j] = Math.max(dp[j], dp[j - w] + w);
                }
                for (DoublyLinkedNode<StairLine> stairLineNode : stairLineList) {
                    maxW = W - stairLineNode.value.x;
                    if (dp[maxW] != maxW) {
                        beta = Math.max(0, beta - delta);
                        dpCutFailCnt = 0;
                        return;
                    }
                }

                // Y-DP-Cut
                int maxH = H - stairLineList.tail.value.y;
                dp = new int[maxH + 1];
                for (DoublyLinkedNode<Item> itemNode : unPackedItemList) {
                    int h = itemNode.value.h;
                    // 遍历背包容量
                    for (int j = maxH; j >= h; j--) dp[j] = Math.max(dp[j], dp[j - h] + h);
                }
                for (DoublyLinkedNode<StairLine> stairLineNode : stairLineList) {
                    maxH = H - stairLineNode.value.y;
                    if (dp[maxH] != maxH) {
                        beta = Math.max(0, beta - delta);
                        dpCutFailCnt = 0;
                        return;
                    }
                }

                if (++dpCutFailCnt == q) {
                    beta++;
                    dpCutFailCnt = 0;
                }
            }

            if (TimeUtil.isTimeLimit()) throw new EarlyTerminationException();

            generatedNodes++;

            for (DoublyLinkedNode<Item> itemNode : unPackedItemList) {
                for (DoublyLinkedNode<StairLine> stairLineNode : stairLineList) {
                    packing(itemNode, stairLineNode, unPackedItemList, stairLineList, placeItems, packedItemNum);
                }
            }

            exploredNodes++;
        }
    }

    private void ExactOpp() {
        Arrays.sort(items, Item.itemComparatorByDecreaseSWH);
        for (int i = 0; i < items.length; i++) items[i].index = i;
        DoublyLinkedList<StairLine> stairLineList = new DoublyLinkedList<>(new StairLine(0, 0, W, H));
        try {
            DoublyLinkedList<Item> unPackedItemList = new DoublyLinkedList<>(items);
            maxStairLineCnt = 2;
            while (maxStairLineCnt <= 4) {
                set = new HashSet<>();
                beta = 0;
                dpCutFailCnt = 0;
                dfs(stairLineList, new PlaceItem[curN], 0, unPackedItemList);
                maxStairLineCnt++;
            }
            set = new HashSet<>();
            beta = 0;
            dpCutFailCnt = 0;
            maxStairLineCnt = CommonUtil.ceilToInt(curN / 2d);
            dfs(stairLineList, new PlaceItem[curN], 0, unPackedItemList);
        } catch (EarlyTerminationException e) {

        }
    }

    int W, H, n, curN;
    Item[] items;
    List<PlaceItem> feasiblePlaceItemList;

    public List<PlaceItem> solve(int initW, int initH, int n, Item[] initItems) {

        this.W = initW;
        this.H = initH;
        this.n = n;
        this.curN = initItems.length;
        this.items = initItems;

        // 精确OPP
        ExactOpp();

        if (feasiblePlaceItemList == null || feasiblePlaceItemList.size() < curN) {
            return null;
        } else {
            return feasiblePlaceItemList;
        }

    }
}