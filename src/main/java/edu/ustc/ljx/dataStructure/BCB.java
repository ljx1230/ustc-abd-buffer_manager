package edu.ustc.ljx.dataStructure;

/**
 * @Author: ljx
 * @Date: 2024/1/17 14:55
 * 缓冲区控制块
 */
public class BCB {
    public int pageId;
    public int frameId;
    public int count;
    public int dirty;
    public BCB next;

    @Override
    public String toString() {
        return "BCB{" +
                "pageId=" + pageId +
                ", frameId=" + frameId +
                ", count=" + count +
                ", dirty=" + dirty +
                ", next=" + next +
                '}';
    }
}
