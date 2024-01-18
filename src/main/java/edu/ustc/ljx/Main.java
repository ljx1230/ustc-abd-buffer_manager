package edu.ustc.ljx;

import edu.ustc.ljx.manager.BufferManager;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @Author: ljx
 * @Date: 2024/1/17 18:21
 */
public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Trace trace = new Trace();
        try {
            trace.createFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("创建文件失败");
        }
        try {
            double startTime = System.currentTimeMillis();
            trace.getStatistics();
            double endTime = System.currentTimeMillis();
            double runTime = endTime - startTime;
            System.out.printf("总I/O次数: %d\n" +
                    " 命中率: %%%.3f\n" +
                    " 运行时间: %.3fs\n", trace.getIOCounter(), trace.getHitRate() * 100, (double)runTime / 1000);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("测试程序异常!");
        }
    }
}
