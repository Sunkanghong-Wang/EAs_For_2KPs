package com.wskh.utils;

import com.wskh.classes.PlaceItem;

import java.util.List;

public class CheckUtil {
    public static void checkOverlapAndOutBin(int W, int H, List<PlaceItem> placeItemList) {
        int C = W * H;
        int S = 0;
        for (int i = 0; i < placeItemList.size(); i++) {
            PlaceItem placeItemI = placeItemList.get(i);
            S += placeItemI.s;
            if (S > C) {
                throw new RuntimeException("Exceeding capacity: " + S + " > " + (C));
            }
            if (placeItemI.x + placeItemI.w > W || placeItemI.y + placeItemI.h > H) {
                throw new RuntimeException("Beyond boundaries: " + placeItemI);
            }
            for (int j = i + 1; j < placeItemList.size(); j++) {
                PlaceItem placeItemJ = placeItemList.get(j);
                if (CommonUtil.isOverlap(placeItemI, placeItemJ)) {
                    throw new RuntimeException("Item overlap");
                }
            }
        }
    }
}