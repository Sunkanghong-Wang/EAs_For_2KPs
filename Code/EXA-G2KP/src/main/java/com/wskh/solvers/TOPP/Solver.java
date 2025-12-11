package com.wskh.solvers.TOPP;

import com.wskh.classes.Item;

import java.util.Random;

public class Solver {

    public int W, H, curN;
    public Item[] items;

    public Random random;

    public Solver(Random random) {
        this.random = random;
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

                Item itemI = items[i];
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
                    b2 = true;
                }
            }

            if (!b1 && !b2) break;
        }
    }

}