package edu.ustc.ljx.dataStructure;

import lombok.Data;

/**
 * @Author: ljx
 * @Date: 2024/1/17 16:45
 */
public class LRUle {
    public BCB bcb;
    public LRUle pre;
    public LRUle next;

    @Override
    public String toString() {
        return "LRUle{" +
                "bcb=" + bcb +
                ", pre=" + pre +
                ", next=" + next +
                '}';
    }
}
