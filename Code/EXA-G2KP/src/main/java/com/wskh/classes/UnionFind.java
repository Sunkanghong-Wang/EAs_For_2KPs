package com.wskh.classes;

public class UnionFind {
    public int[] fat;
    int[] rank;

    public UnionFind(int n) {
        fat = new int[n];
        rank = new int[n]; // 用于按秩合并
        for (int i = 0; i < n; i++) {
            fat[i] = i;
            rank[i] = 1;
        }
    }

    // 查找操作，带路径压缩
    public int find(int x) {
        if (fat[x] != x) {
            fat[x] = find(fat[x]); // 路径压缩
        }
        return fat[x];
    }

    // 合并操作，按秩合并
    public void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX != rootY) {
            // 按秩合并
            if (rank[rootX] > rank[rootY]) {
                fat[rootY] = rootX;
            } else if (rank[rootX] < rank[rootY]) {
                fat[rootX] = rootY;
            } else {
                fat[rootY] = rootX;
                rank[rootX]++;
            }
        }
    }
}
