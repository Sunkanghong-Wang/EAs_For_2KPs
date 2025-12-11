package com.wskh.utils;

import com.wskh.classes.PlaceItem;

import java.util.*;

public class YCheckUtil {

    static class CombinedItem extends PlaceItem {
        List<CombinedItem> children;

        public void addChild(CombinedItem child) {
            id = 31 * id + child.id;
            children.add(child);
        }

        public CombinedItem(int id, int x, int y, int w, int h, List<CombinedItem> children) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.children = children;
        }

        @Override
        public String toString() {
            return "CombinedItem{" +
                    "id=" + id +
                    ", index=" + index +
                    ", x=" + x +
                    ", y=" + y +
                    ", w=" + w +
                    ", h=" + h +
                    ", s=" + s +
                    '}';
        }
    }

    int n;
    boolean xFirst;
    int W, H;
    Integer[] idIndexArr; // <id,index>,item的id和item在placeItemList中的索引

    public YCheckUtil(int W, int H, boolean xFirst, int n) {
        this.n = n;
        this.xFirst = xFirst;
        if (xFirst) {
            this.W = W;
            this.H = H;
        } else {
            this.W = H;
            this.H = W;
        }
    }

    void dfs(int subW, int subH, int[] hUse, int[] surplusHeightArr, List<CombinedItem> placedItems, List<CombinedItem> noPlacedItems) {
        if (TimeUtil.isTimeLimit()) return;

        // 第一种剪枝策略
        for (int x = 0; x < subW; x++) if (hUse[x] + surplusHeightArr[x] > subH) return;

        // 找到最下最左的天际线
        int l = -1, r = -1, min = Integer.MAX_VALUE;
        for (int i = 0; i < subW; i++) {
            if (hUse[i] < min) {
                l = i;
                r = i;
                min = hUse[i];
            } else if (hUse[i] == min && r == i - 1 && hUse[i - 1] == min) {
                r = i;
            }
        }
        int curY = hUse[l];
        int hl = (l == 0 ? subH : hUse[l - 1]);
        int hr = (r == subW - 1 ? subH : hUse[r + 1]);
        int minHlHr = Math.min(hl, hr);

        // 寻找可以被打包到当前天际线的物品集
        int noPlacedItemsSize = noPlacedItems.size();
        List<Integer> canPackItemIndexList = new ArrayList<>(noPlacedItemsSize);
        for (int i = 0; i < noPlacedItemsSize; i++) {
            CombinedItem item = noPlacedItems.get(i);
            int itemL = item.x;
            if (l <= itemL && r >= itemL + item.w - 1 && curY + item.h <= subH) {
                canPackItemIndexList.add(i);
            }
        }

        // 遍历每个可以打包到当前天际线上的物品
        int secCnt = 0;
        for (int i : canPackItemIndexList) {
            CombinedItem item = noPlacedItems.get(i);
            int itemL = item.x;
            // 第三种剪枝策略
            if (i > 0) {
                CombinedItem lastItem = noPlacedItems.get(i - 1);
                if (lastItem.x == itemL && lastItem.w == item.w && lastItem.h == item.h) continue;
            }
            // 第四种剪枝策略
            boolean fathomingItem = false;
            for (CombinedItem placedItem : placedItems) {
                if (placedItem.x == itemL && placedItem.w == item.w && placedItem.index > item.index && placedItem.y + placedItem.h == curY) {
                    fathomingItem = true;
                    break;
                }
            }
            if (fathomingItem) continue;
            // 第五种剪枝策略
            for (int k : canPackItemIndexList) {
                if (k != i) {
                    CombinedItem itemK = noPlacedItems.get(k);
                    if (itemK.x >= l && itemK.x + itemK.w <= itemL && itemK.h <= Math.min(hl - curY, item.h)) {
                        fathomingItem = true;
                        break;
                    }
                }
            }
            if (fathomingItem) continue;
            // 打包物品
            int itemR = itemL + item.w - 1;
            if (item.h + curY <= minHlHr) secCnt++; // 第二种剪枝策略
            item.y = curY;
            placedItems.add(noPlacedItems.remove(i));
            if (noPlacedItems.isEmpty()) return;
            for (int k = l; k < itemL; k++) hUse[k] = Math.min(hl, curY + item.h);
            for (int k = itemL; k <= itemR; k++) {
                hUse[k] += item.h;
                surplusHeightArr[k] -= item.h;
            }
            dfs(subW, subH, hUse, surplusHeightArr, placedItems, noPlacedItems);
            if (noPlacedItems.isEmpty()) return;
            // 回溯
            noPlacedItems.add(i, item);
            placedItems.removeLast();
            for (int k = l; k <= itemR; k++) hUse[k] = curY;
            for (int k = itemL; k <= itemR; k++) surplusHeightArr[k] += item.h;
        }

        // 第二种剪枝策略
        if (secCnt == 0) {
            // 合并天际线
            for (int i = l; i <= r; i++) hUse[i] = minHlHr;
            dfs(subW, subH, hUse, surplusHeightArr, placedItems, noPlacedItems);
            if (noPlacedItems.isEmpty()) return;
            // 回溯
            for (int i = l; i <= r; i++) hUse[i] = curY;
        }
    }

    List<CombinedItem> checkSub(int initX, int subW, int subH, List<CombinedItem> combinedItems) {
        if (subW <= 0) throw new RuntimeException();
        // 快速判断
        for (CombinedItem combinedItem : combinedItems) if (combinedItem.h > subH) return null;
        if (initX > 0) for (CombinedItem combinedItem : combinedItems) combinedItem.x -= initX;
        int[] surplusHeightArr = new int[subW];
        for (int i = 0; i < combinedItems.size(); i++) {
            CombinedItem combinedItem = combinedItems.get(i);
            combinedItem.index = i;
            for (int j = combinedItem.x; j < combinedItem.x + combinedItem.w; j++)
                surplusHeightArr[j] += combinedItem.h;
        }
        for (int i = 0; i < subW; i++) {
            if (surplusHeightArr[i] > subH) {
                for (CombinedItem combinedItem : combinedItems) combinedItem.x += initX;
                return null;
            }
        }
        // dfs
        combinedItems.sort((o1, o2) -> {
            int c = Integer.compare(o1.x, o2.x);
            if (c == 0) c = -Integer.compare(o1.w, o2.w);
            return c == 0 ? -Integer.compare(o1.h, o2.h) : c;
        });
        List<CombinedItem> resList = new ArrayList<>(combinedItems.size());
        dfs(subW, subH, new int[subW], surplusHeightArr, resList, new ArrayList<>(combinedItems));
        for (CombinedItem combinedItem : combinedItems) combinedItem.x += initX;
        return resList.size() == combinedItems.size() ? resList : null;
    }

    private List<CombinedItem> getNewCombinedItemsByResList(CombinedItem newCombinedItem, CombinedItem combinedItemJ, List<CombinedItem> combinedItems, List<CombinedItem> resList) {
        newCombinedItem.addChild(combinedItemJ);
        Set<Integer> set = new HashSet<>();
        for (CombinedItem combinedItem : resList) {
            set.add(combinedItem.id);
            newCombinedItem.addChild(combinedItem);
        }
        set.add(combinedItemJ.id);
        List<CombinedItem> newCombinedItems = new ArrayList<>(combinedItems.size());
        newCombinedItems.add(newCombinedItem);
        for (CombinedItem combinedItem : combinedItems)
            if (!set.contains(combinedItem.id)) newCombinedItems.add(combinedItem);
        return newCombinedItems;
    }

    List<CombinedItem> mergeItems(List<PlaceItem> placeItemList) {
        // 初始化，一个物品一个组合物品对象
        List<CombinedItem> combinedItems = new ArrayList<>(placeItemList.size());
        for (PlaceItem placeItem : placeItemList) {
            if (xFirst) {
                combinedItems.add(new CombinedItem(placeItem.id, placeItem.x, 0, placeItem.w, placeItem.h, new ArrayList<>()));
            } else {
                combinedItems.add(new CombinedItem(placeItem.id, placeItem.y, 0, placeItem.h, placeItem.w, new ArrayList<>()));
            }
        }

        // 右合并
        Comparator<CombinedItem> comparator = (o1, o2) -> {
            int c = Integer.compare(o1.x + o1.w, o2.x + o2.w);
            return c == 0 ? -Integer.compare(o1.x, o2.x) : c;
        };
        combinedItems.sort(comparator); // x+w 递增排序，x 递减打破平局
        // x+w 递增遍历
        for (int j = 0; j < combinedItems.size(); j++) {
            CombinedItem combinedItemJ = combinedItems.get(j);
            int rightJX = combinedItemJ.x + combinedItemJ.w;
            // 获取它的右集合
            List<CombinedItem> rightList = new ArrayList<>();
            for (int i = j + 1; i < combinedItems.size(); i++) {
                CombinedItem combinedItem = combinedItems.get(i);
                if (combinedItem.x >= rightJX) rightList.add(combinedItem);
            }
            // 尝试打包到子容器中
            int minLeftX = W;
            while (!rightList.isEmpty()) {
                List<CombinedItem> resList = checkSub(rightJX, minLeftX - rightJX, combinedItemJ.h, rightList);
                if (resList != null) {
                    // 打包到子容器成功，合并物品
                    combinedItems = getNewCombinedItemsByResList(new CombinedItem(1, combinedItemJ.x, 0, minLeftX - combinedItemJ.x, combinedItemJ.h, new ArrayList<>()), combinedItemJ, combinedItems, resList);
                    combinedItems.sort(comparator);
                    j = -1;
                    break;
                }
                // 到这说明右集合中不能合并或者存在hi>hj的情况
                minLeftX = rightList.getLast().x;
                List<CombinedItem> newRightList = new ArrayList<>(rightList.size());
                for (CombinedItem combinedItem : rightList) {
                    if (combinedItem.x < minLeftX && combinedItem.x + combinedItem.w > minLeftX) {
                        newRightList.clear();
                        break;
                    }
                    if (combinedItem.x + combinedItem.w <= minLeftX) newRightList.add(combinedItem);
                }
                rightList = newRightList;
            }
        }

        // 左合并
        comparator = (o1, o2) -> {
            int c = -Integer.compare(o1.x, o2.x);
            return c == 0 ? -Integer.compare(o1.x + o1.w, o2.x + o2.w) : c;
        };
        combinedItems.sort(comparator); // x 递减排序
        // x 非递增遍历
        for (int j = 0; j < combinedItems.size(); j++) {
            CombinedItem combinedItemJ = combinedItems.get(j);
            // 获取它的左集合
            List<CombinedItem> leftList = new ArrayList<>();
            for (int i = j + 1; i < combinedItems.size(); i++) {
                CombinedItem combinedItemI = combinedItems.get(i);
                if (combinedItemI.x + combinedItemI.w <= combinedItemJ.x) leftList.add(combinedItemI);
            }
            // 尝试打包到子容器中
            int maxRightX = 0;
            while (!leftList.isEmpty()) {
                List<CombinedItem> resList = checkSub(maxRightX, combinedItemJ.x - maxRightX, combinedItemJ.h, leftList);
                if (resList != null) {
                    // 打包到子容器成功，合并物品
                    combinedItems = getNewCombinedItemsByResList(new CombinedItem(1, maxRightX, 0, combinedItemJ.x - maxRightX + combinedItemJ.w, combinedItemJ.h, new ArrayList<>()), combinedItemJ, combinedItems, resList);
                    combinedItems.sort(Comparator.comparingInt(o -> o.x));
                    j = combinedItems.size();
                    break;
                }
                // 第二步，到这说明左集合中不能合并或者存在hi>hj的情况
                CombinedItem firstCombinedItem = leftList.getFirst();
                maxRightX = firstCombinedItem.x + firstCombinedItem.w;
                List<CombinedItem> newLeftList = new ArrayList<>(leftList.size());
                for (CombinedItem combinedItem : leftList) {
                    if (combinedItem.x < maxRightX && combinedItem.x + combinedItem.w > maxRightX) {
                        newLeftList.clear();
                        break;
                    }
                    if (combinedItem.x >= maxRightX) newLeftList.add(combinedItem);
                }
                leftList = newLeftList;
            }
        }

        return combinedItems;
    }

    List<CombinedItem> enlargeItems(List<CombinedItem> combinedItems) {
        combinedItems.sort((o1, o2) -> {
            int c = -Integer.compare(o1.w, o2.w);
            return c == 0 ? -Integer.compare(o1.h, o2.h) : c;
        });
        for (CombinedItem A : combinedItems) {
            int l = 0;
            int r = W;
            int rr = A.x + A.w;
            for (CombinedItem B : combinedItems) {
                int ll = B.x + B.w;
                if (ll <= A.x) {
                    l = Math.max(l, ll);
                } else if (B.x >= rr) {
                    r = Math.min(r, B.x);
                }
            }
            A.x = l;
            A.w = r - l;
        }
        return combinedItems;
    }

    int shrinkBins(List<CombinedItem> combinedItems) {
        int newW = W;

        int[] hashCodeArr = new int[W];
        Arrays.fill(hashCodeArr, 1);
        List<Integer>[] combinedItemIndexArr = new List[W];
        for (int i = 0; i < W; i++) {
            combinedItemIndexArr[i] = new ArrayList<>(combinedItems.size());
        }

        for (int i = 0; i < combinedItems.size(); i++) {
            CombinedItem combinedItem = combinedItems.get(i);
            for (int j = combinedItem.x; j < combinedItem.x + combinedItem.w; j++) {
                hashCodeArr[j] = hashCodeArr[j] * 31 + combinedItem.id;
                combinedItemIndexArr[j].add(i);
            }
        }

        int redCount = 0;
        boolean[] redBoolArr = new boolean[combinedItems.size()];
        for (int i : combinedItemIndexArr[0]) redBoolArr[i] = true;
        for (int x = 1; x < W; x++) {
            List<Integer> combinedItemIndexArrX = combinedItemIndexArr[x];
            for (int i : combinedItemIndexArrX) {
                if (!redBoolArr[i]) {
                    redBoolArr[i] = true;
                    combinedItems.get(i).x -= redCount;
                }
            }
            if (hashCodeArr[x] == hashCodeArr[x - 1]) {
                for (int i : combinedItemIndexArrX) combinedItems.get(i).w--;
                newW--;
                redCount++;
            }
        }

        return newW;
    }

    private void correctPlaceItemList(List<PlaceItem> placeItemList, CombinedItem combinedItem) {
        if (combinedItem.children.isEmpty()) {
            if (xFirst) {
                placeItemList.get(idIndexArr[combinedItem.id]).y = combinedItem.y;
            } else {
                // 如果是 yFirst 就要特殊处理
                placeItemList.get(idIndexArr[combinedItem.id]).x = combinedItem.y;
            }
        } else {
            List<CombinedItem> children = combinedItem.children;
            for (CombinedItem child : children) {
                child.y += combinedItem.y;
                correctPlaceItemList(placeItemList, child);
            }
        }
    }

    public boolean check(List<PlaceItem> placeItemList) {
        if (placeItemList.isEmpty()) return true;

        idIndexArr = new Integer[n];
        for (int i = 0; i < placeItemList.size(); i++) idIndexArr[placeItemList.get(i).id] = i;

        // 1. 合并物品
        List<CombinedItem> combinedItems = mergeItems(placeItemList);

        // 2. 扩大物品
        combinedItems = enlargeItems(combinedItems);

        // 3. 缩小容器
        int reducedW = shrinkBins(combinedItems);

        // 4. 枚举
        List<CombinedItem> resList = checkSub(0, reducedW, H, combinedItems);
        if (resList == null) return false;
        for (CombinedItem combinedItem : resList) correctPlaceItemList(placeItemList, combinedItem);
        return true;
    }

}