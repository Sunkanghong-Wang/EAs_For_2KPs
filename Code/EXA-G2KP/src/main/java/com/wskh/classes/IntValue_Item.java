package com.wskh.classes;

public class IntValue_Item extends Item {
    public int value;
    public double unitValue;

    public IntValue_Item(int id, int index, int w, int h, int s, int value, double unitValue) {
        this.id = id;
        this.index = index;
        this.w = w;
        this.h = h;
        this.s = s;
        this.value = value;
        this.unitValue = unitValue;
    }

    public IntValue_Item copy() {
        return new IntValue_Item(id, index, w, h, s, value,unitValue);
    }

    public static IntValue_Item[] copy(IntValue_Item[] in) {
        IntValue_Item[] items = new IntValue_Item[in.length];
        for (int i = 0; i < in.length; i++) {
            items[i] = in[i].copy();
        }
        return items;
    }

    @Override
    public String toString() {
        return "IntValue_Item{" +
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
