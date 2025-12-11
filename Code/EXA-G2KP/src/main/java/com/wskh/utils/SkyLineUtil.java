package com.wskh.utils;

import com.wskh.classes.Item;
import com.wskh.classes.PlaceItem;
import com.wskh.classes.SkyLine;

import java.util.*;

public class SkyLineUtil {
    public static void combineSkylines(ArrayList<SkyLine> skyLineList, SkyLine leftSkyLine, SkyLine rightSkyLine, SkyLine skyLine) {
        if (leftSkyLine != null && rightSkyLine != null) {
            if (leftSkyLine.y < rightSkyLine.y) {
                leftSkyLine.len += skyLine.len;
            } else if (rightSkyLine.y < leftSkyLine.y) {
                rightSkyLine.len += skyLine.len;
                rightSkyLine.x = skyLine.x;
            } else {
                leftSkyLine.len += skyLine.len;
                leftSkyLine.len += rightSkyLine.len;
                skyLineList.remove(rightSkyLine);
            }
        } else if (leftSkyLine != null) {
            leftSkyLine.len += skyLine.len;
        } else if (rightSkyLine != null) {
            rightSkyLine.len += skyLine.len;
            rightSkyLine.x = skyLine.x;
        }
    }

    public static PlaceItem placeLeft(ArrayList<SkyLine> skyLineList, Item item, SkyLine leftSkyLine, SkyLine rightSkyLine, SkyLine skyLine) {
        // 生成PlaceItem对象
        PlaceItem placeItem = new PlaceItem(item.id, item.index, skyLine.x, skyLine.y, item.w, item.h, item.s);
        // 将新天际线加入队列
        SkyLine newSkyLine1 = new SkyLine(skyLine.x + placeItem.w, skyLine.y, skyLine.len - placeItem.w);
        if (newSkyLine1.len > 0) {
            skyLineList.addFirst(newSkyLine1);
        }
        SkyLine newSkyLine2 = new SkyLine(skyLine.x, skyLine.y + placeItem.h, placeItem.w);
        boolean isCombine = false;
        if (leftSkyLine != null && rightSkyLine != null) {
            if (leftSkyLine.y == newSkyLine2.y) {
                if (leftSkyLine.y == rightSkyLine.y && leftSkyLine.x + newSkyLine2.len == rightSkyLine.x) {
                    // 三线合一
                    leftSkyLine.len += newSkyLine2.len;
                    leftSkyLine.len += rightSkyLine.len;
                    skyLineList.remove(rightSkyLine);
                    isCombine = true;
                } else {
                    leftSkyLine.len += newSkyLine2.len;
                    isCombine = true;
                }
            }
        } else if (leftSkyLine != null) {
            if (leftSkyLine.y == newSkyLine2.y) {
                leftSkyLine.len += newSkyLine2.len;
                isCombine = true;
            }
        }
        if (!isCombine) {
            skyLineList.add(newSkyLine2);
            Collections.sort(skyLineList);
        }
        // 返回PlaceItem对象
        return placeItem;
    }

    public static PlaceItem placeRight(ArrayList<SkyLine> skyLineList, Item item, SkyLine leftSkyLine, SkyLine rightSkyLine, SkyLine skyLine) {
        // 生成PlaceItem对象
        PlaceItem placeItem = new PlaceItem(item.id, item.index, skyLine.x + skyLine.len - item.w, skyLine.y, item.w, item.h, item.s);
        // 将新天际线加入队列
        SkyLine newSkyLine1 = new SkyLine(skyLine.x, skyLine.y, skyLine.len - placeItem.w);
        if (newSkyLine1.len > 0) {
            skyLineList.addFirst(newSkyLine1);
        }
        SkyLine newSkyLine2 = new SkyLine(placeItem.x, skyLine.y + placeItem.h, placeItem.w);
        boolean isCombine = false;
        if (leftSkyLine != null && rightSkyLine != null) {
            if (rightSkyLine.y == newSkyLine2.y) {
                if (rightSkyLine.y == leftSkyLine.y && leftSkyLine.x + newSkyLine2.len == rightSkyLine.x) {
                    // 三线合一
                    leftSkyLine.len += newSkyLine2.len;
                    leftSkyLine.len += rightSkyLine.len;
                    skyLineList.remove(rightSkyLine);
                    isCombine = true;
                } else {
                    rightSkyLine.x = newSkyLine2.x;
                    rightSkyLine.len += newSkyLine2.len;
                    isCombine = true;
                }
            }
        } else if (rightSkyLine != null) {
            if (rightSkyLine.y == newSkyLine2.y) {
                rightSkyLine.x = newSkyLine2.x;
                rightSkyLine.len += newSkyLine2.len;
                isCombine = true;
            }
        }
        if (!isCombine) {
            skyLineList.add(newSkyLine2);
            Collections.sort(skyLineList);
        }
        // 返回PlaceItem对象
        return placeItem;
    }

    public static int score(int H, int w, int h, SkyLine skyLine, int hl, int hr) {
        // 当前天际线长度小于当前矩形宽度，放不下
        // 如果超出上界，也不能放
        if (skyLine.len < w || skyLine.y + h > H) return -1;
        // 左边墙高于等于右边墙
        if (hl >= hr) {
            if (w == skyLine.len) {
                if (h < hr) {
                    return 1;
                } else if (h < hl && h > hr) {
                    return 3;
                } else if (h > hl) {
                    return 5;
                } else if (h == hl) {
                    return 7;
                } else {
                    return 6;
                }
            } else {
                if (h == hl) {
                    return 4;
                } else if (h == hr) {
                    // 靠右
                    return 2;
                }
            }
        } else {
            if (w == skyLine.len) {
                if (h < hl) {
                    return 1;
                } else if (h > hl && h < hr) {
                    return 3;
                } else if (h > hr) {
                    return 5;
                } else if (h == hl) {
                    return 6;
                } else {
                    return 7;
                }
            } else {
                if (h == hl) {
                    return 2;
                } else if (h == hr) {
                    // 靠右
                    return 4;
                }
            }
            // score = 0 (靠右)
        }
        return 0;
    }

    public static List<PlaceItem> skyLineIteration(int H, ArrayList<SkyLine> skyLineList, List<Item> itemList) {
        int n = itemList.size();
        // 用来存储已经放置的矩形
        List<PlaceItem> placeItemList = new ArrayList<>(n);
        // 开始天际线启发式迭代
        while (!skyLineList.isEmpty() && placeItemList.size() < n) {
            // 获取当前最下最左的天际线（取出队首元素）
            SkyLine skyLine = skyLineList.removeFirst();
            // 初始化hl和hr
            int hl = H - skyLine.y;
            int hr = H - skyLine.y;
            // 提前跳出计数器(如果hl和hr都获取到了就可以提前跳出，节省时间)
            int c = 0;
            SkyLine leftSkyLine = null;
            SkyLine rightSkyLine = null;
            // 顺序遍历天际线队列，根据skyline和skyline队列获取hl和hr
            for (SkyLine line : skyLineList) {
                // 由于skyLine是队首元素，所以它的Y肯定最小，所以line.y - skyLine.y肯定都大于等于0
                if (line.x + line.len == skyLine.x) {
                    // 尾头相连，是hl
                    hl = line.y - skyLine.y;
                    c++;
                    leftSkyLine = line;
                    // hl和hr都获取到了，就没必要继续遍历了，可以提前跳出节省时间
                    if (c == 2) {
                        break;
                    }
                } else if (line.x == skyLine.x + skyLine.len) {
                    // 头尾相连，是hr
                    hr = line.y - skyLine.y;
                    c++;
                    rightSkyLine = line;
                    // hl和hr都获取到了，就没必要继续遍历了，可以提前跳出节省时间
                    if (c == 2) {
                        break;
                    }
                }
            }
            // 记录最大评分矩形的索引
            int maxItemIndex = -1;
            // 记录最大评分
            int maxScore = -1;
            // 遍历每一个矩形，选取评分最大的矩形进行放置
            for (int i = 0; i < itemList.size(); i++) {
                Item item = itemList.get(i);
                // 不旋转的情况
                int score = score(H, item.w, item.h, skyLine, hl, hr);
                if (score > maxScore) {
                    // 更新最大评分
                    maxScore = score;
                    maxItemIndex = i;
                }
            }
            // 如果当前最大得分大于等于0，则说明有矩形可以放置，则按照规则对其进行放置
            if (maxScore >= 0) {
                Item item = itemList.remove(maxItemIndex);
                // 左墙高于等于右墙
                if (hl >= hr) {
                    // 评分为2时，矩形靠天际线右边放，否则靠天际线左边放
                    if (maxScore == 2) {
                        placeItemList.add(placeRight(skyLineList, item, leftSkyLine, rightSkyLine, skyLine));
                    } else {
                        placeItemList.add(placeLeft(skyLineList, item, leftSkyLine, rightSkyLine, skyLine));
                    }
                } else {
                    // 左墙低于右墙
                    // 评分为4或0时，矩形靠天际线右边放，否则靠天际线左边放
                    if (maxScore == 4 || maxScore == 0) {
                        placeItemList.add(placeRight(skyLineList, item, leftSkyLine, rightSkyLine, skyLine));
                    } else {
                        placeItemList.add(placeLeft(skyLineList, item, leftSkyLine, rightSkyLine, skyLine));
                    }
                }
            } else {
                // 如果当前天际线一个矩形都放不下，那就上移天际线，与其他天际线合并
                combineSkylines(skyLineList, leftSkyLine, rightSkyLine, skyLine);
            }
        }
        return placeItemList;
    }

    private static List<PlaceItem> evaluate(int W, int H, ArrayList<Item> itemList) {
        ArrayList<SkyLine> skyLineList = new ArrayList<>();
        skyLineList.add(new SkyLine(0, 0, W));
        return skyLineIteration(H, skyLineList, itemList);
    }

    public static List<PlaceItem> skyLineBasedTabuSearchInOneBin(int W, int H, Item[] curItems, Random random) {
        int n = curItems.length;
        List<PlaceItem> bestPlaceItemList = evaluate(W, H, new ArrayList<>(Arrays.asList(curItems)));
        if (bestPlaceItemList.size() == n) return bestPlaceItemList;
        int bestS = 0;
        for (PlaceItem placeItem : bestPlaceItemList) bestS += placeItem.s;

        int beta = 4 * n;
        int tabuLen = 2 * n;

        int allCombineCnt = n * (n - 1) / 2;
        int iter = 5 * n;
        LinkedList<Integer> tabuList = new LinkedList<>();
        Set<Integer> tabuSet = new HashSet<>(tabuLen + 1);

        for (int it = 0; it < iter; it++) {
            List<PlaceItem> localBestPlaceItemList = null;
            Item[] localBestItems = null;
            int localBestS = 0;
            int bestHashCode = -1;
            // beta
            for (int r = 0; r < beta; r++) {
                // 获取没被禁忌的 (epoch,j)
                int i = random.nextInt(n);
                int j = random.nextInt(n);
                while (i == j) j = random.nextInt(n);
                if (i > j) {
                    j = i ^ j;
                    i = i ^ j;
                    j = i ^ j;
                }
                int hashCode = i + 31 * j;
                if (tabuSet.contains(hashCode)) continue;
                // 交换 sequence 中 i 和 j 位置上的元素
                Item[] localItems = curItems.clone();
                Item tempItem = localItems[i];
                localItems[i] = localItems[j];
                localItems[j] = tempItem;
                // 重新评价
                List<PlaceItem> localPlaceItemList = evaluate(W, H, new ArrayList<>(Arrays.asList(localItems)));
                if (localPlaceItemList.size() == n) return localPlaceItemList;
                int localS = 0;
                for (PlaceItem placeItem : localPlaceItemList) localS += placeItem.s;
                if (localBestS < localS) {
                    localBestPlaceItemList = localPlaceItemList;
                    localBestS = localS;
                    localBestItems = localItems;
                    bestHashCode = hashCode;
                }
            }
            if (localBestPlaceItemList != null) {
                // 更新全局最优解
                if (bestS < localBestS) bestPlaceItemList = localBestPlaceItemList;
                // 更新序列
                curItems = localBestItems;
                // 更新禁忌表
                if (tabuList.size() == tabuLen) tabuSet.remove(tabuList.removeFirst());
                tabuList.add(bestHashCode);
                tabuSet.add(bestHashCode);
                if (tabuList.size() == allCombineCnt) break;
            }
        }
        return bestPlaceItemList;
    }

}