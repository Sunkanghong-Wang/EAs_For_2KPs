package com.wskh.classes;

import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Comparator;

@NoArgsConstructor
@ToString
public class Item {
    public int id, index, w, h, s;

    public Item(int id, int index, int w, int h, int s) {
        this.id = id;
        this.index = index;
        this.w = w;
        this.h = h;
        this.s = s;
    }

    public PlaceItem packed(int x, int y) {
        return new PlaceItem(id, index, x, y, w, h, s);
    }

    public Item copy() {
        return new Item(id, index, w, h, s);
    }

    public static Item[] copy(Item[] in) {
        Item[] items = new Item[in.length];
        for (int i = 0; i < in.length; i++) {
            items[i] = in[i].copy();
        }
        return items;
    }

    public static Comparator<Item> itemComparatorByDecreaseS = Comparator.comparingInt((Item o) -> -o.s);

    public static Comparator<Item> itemComparatorByDecreaseSWH = Comparator.comparingInt((Item o) -> -o.s).thenComparingInt(o -> -o.w).thenComparingInt(o -> -o.h);
    public static Comparator<Item> itemComparatorByIncreaseSWH = Comparator.comparingInt((Item o) -> o.s).thenComparingInt(o -> o.w).thenComparingInt(o -> o.h);

    public static Comparator<Item> itemComparatorByDecreaseHW = Comparator.comparingInt((Item o) -> -o.h).thenComparingInt(o -> -o.w);

    public static Comparator<Item> itemComparatorByDecreaseWH = Comparator.comparingInt((Item o) -> -o.w).thenComparingInt(o -> -o.h);

    public static Comparator<Item> itemComparatorByIncreaseW = Comparator.comparingInt(o -> o.w);
    public static Comparator<Item> itemComparatorByIncreaseH = Comparator.comparingInt(o -> o.h);
    public static Comparator<Item> itemComparatorByIncreaseS = Comparator.comparingInt(o -> o.s);

}