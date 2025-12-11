package com.wskh.classes;

public class LongValue_Item extends Item {
    public long value;
    public double unitValue;

    public LongValue_Item(int id, int index, int w, int h, int s, long value, double unitValue) {
        this.id = id;
        this.index = index;
        this.w = w;
        this.h = h;
        this.s = s;
        this.value = value;
        this.unitValue = unitValue;
    }

    public LongValue_Item copy() {
        return new LongValue_Item(id, index, w, h, s, value, unitValue);
    }

    public static LongValue_Item[] copy(LongValue_Item[] in) {
        LongValue_Item[] items = new LongValue_Item[in.length];
        for (int i = 0; i < in.length; i++) {
            items[i] = in[i].copy();
        }
        return items;
    }

    @Override
    public String toString() {
        return "LongValue_Item{" +
                "id=" + id +
                ", indexs=" + index +
                ", w=" + w +
                ", h=" + h +
                ", s=" + s +
                ", value=" + value +
                ", unitValue=" + unitValue +
                '}';
    }
}
