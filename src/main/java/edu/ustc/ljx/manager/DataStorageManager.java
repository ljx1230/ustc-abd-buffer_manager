package edu.ustc.ljx.manager;

import edu.ustc.ljx.dataStructure.BFrame;
import lombok.Data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import static edu.ustc.ljx.constant.BufferConstants.FRAMESIZE;
import static edu.ustc.ljx.constant.BufferConstants.MAXPAGES;

/**
 * @Author: ljx
 * @Date: 2024/1/17 15:00
 */
@Data
public class DataStorageManager {
    private RandomAccessFile currFile;
    private int numPages;
    private int[] pages;

    public static int ICounter = 0;
    public static int OCounter = 0;

    public DataStorageManager() {
        this.numPages = 0;
        this.pages = new int[MAXPAGES];
        currFile = null;
        for(int i = 0;i < MAXPAGES;i++) {
            this.pages[i] = 0; // 初始时，page的useBit = 0,表示未被使用
        }
    }

    public int openFile(String fileName){
        try{
            this.currFile = new RandomAccessFile(fileName,"rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
        return 1;
    }
    public int closeFile() {
        try {
            if(currFile != null) {
                this.currFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        this.currFile = null;
        return 1;
    }
    public BFrame readPage(int pageId) throws IOException{
        byte[] buffer = new byte[FRAMESIZE];
        try {
            long pos = (long) pageId * FRAMESIZE;
            currFile.seek(pos);
            int length = currFile.read(buffer, 0, FRAMESIZE);
            if(length == 0)
                System.out.println("未读取到任何内容");
            else if (length == -1)
                System.out.println("文件已读取到末尾");
            ICounter++;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("文件读异常");
        }
        return new BFrame(buffer);
    }
    public int writePage(int frameId,BFrame frame){
        try {
            this.currFile.seek((long) frameId * FRAMESIZE);
            this.currFile.write(Arrays.toString(frame.filed).getBytes(), 0, FRAMESIZE);
            OCounter++;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("文件写异常");
        }
        return FRAMESIZE;
    }
    public RandomAccessFile getFile() {
        return this.currFile;
    }
    public void incNumPages() {
        this.numPages++;
    }
    public void setUse(int index,int useBit) {
        this.pages[index] = useBit;
    }
    public int getUse(int index) {
        return this.pages[index];
    }

}
