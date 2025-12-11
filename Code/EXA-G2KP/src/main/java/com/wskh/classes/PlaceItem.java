package com.wskh.classes;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PlaceItem {
    public int id, index, x, y, w, h, s;

    public PlaceItem copy() {
        return new PlaceItem(id, index, x, y, w, h, s);
    }

    public static List<PlaceItem> copy(List<PlaceItem> in) {
        List<PlaceItem> placeItemList = new ArrayList<>(in.size());
        for (PlaceItem placeItem : in) {
            placeItemList.add(placeItem.copy());
        }
        return placeItemList;
    }
}