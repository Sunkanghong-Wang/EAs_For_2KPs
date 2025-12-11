package com.wskh.classes;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;

@AllArgsConstructor
@ToString
public class SkyLine implements Comparable<SkyLine> {
    public int x, y, len;

    public SkyLine copy() {
        return new SkyLine(x, y, len);
    }

    public static ArrayList<SkyLine> copy(ArrayList<SkyLine> list) {
        ArrayList<SkyLine> copy = new ArrayList<>(list.size());
        for (SkyLine skyLine : list) copy.add(skyLine.copy());
        return copy;
    }

    // 天际线排序规则，y越小越优先，y一样时，x越小越优先
    @Override
    public int compareTo(SkyLine o) {
        int c = Integer.compare(y, o.y);
        return c == 0 ? Integer.compare(x, o.x) : c;
    }
}
