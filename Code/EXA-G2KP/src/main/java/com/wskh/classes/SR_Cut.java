package com.wskh.classes;

import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@ToString
public class SR_Cut {
    public int[] indexs;
    public long penalty;

    public boolean containsIndex(int index) {
        return indexs[0] == index || indexs[1] == index || indexs[2] == index;
    }

    public SR_Cut copy() {
        return new SR_Cut(indexs.clone(), penalty);
    }
}