package edu.ustc.ljx.dataStructure;

import edu.ustc.ljx.constant.BufferConstants;

import java.util.Arrays;

/**
 * @Author: ljx
 * @Date: 2024/1/17 14:34
 * 定义buffer中的frame的结构
 */
public class BFrame {
    public char[] filed;
    public BFrame() {
        filed = new char[BufferConstants.FRAMESIZE];
    }
    public BFrame(byte[] buffer) {
        this.filed = new char[BufferConstants.FRAMESIZE];
        System.arraycopy(Arrays.toString(buffer).toCharArray(),0,this.filed,0, BufferConstants.FRAMESIZE);
    }
}
