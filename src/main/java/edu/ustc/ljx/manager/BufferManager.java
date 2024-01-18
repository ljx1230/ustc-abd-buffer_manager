package edu.ustc.ljx.manager;

import edu.ustc.ljx.dataStructure.BCB;
import edu.ustc.ljx.dataStructure.BFrame;
import edu.ustc.ljx.dataStructure.LRUle;

import java.io.IOException;

import static edu.ustc.ljx.constant.BufferConstants.*;

/**
 * @Author: ljx
 * @Date: 2024/1/17 15:02
 */
public class BufferManager {
    // 两个哈希表
    private final int[] ftop = new int[DEFBUFSIZE]; // frameId -> pageId
    private final BCB[] ptof = new BCB[DEFBUFSIZE]; // pageId -> frameId

    // 缓冲区结构
    public final BFrame[] buf = new BFrame[DEFBUFSIZE];
    // 数据存储管理器
    private final DataStorageManager dataStorageManager = new DataStorageManager();

    // LRU链表的头和尾部
    private LRUle head;
    private LRUle tail;
    public static int hitCounter = 0;

    public BufferManager(){
        for(int i = 0;i < DEFBUFSIZE;i++) {
            this.ptof[i] = null;
            this.ftop[i] = -1;
        }
        int success = this.dataStorageManager.openFile("data.dbf");
        if(success == 0) {
            System.out.println("打开文件异常");
        }
    }

    protected void finalize() throws Throwable {
        dataStorageManager.closeFile();
    }

    /**
     *
     * @param pageId
     * @param prot
     * @return
     * @description 查看页面是否已经在缓冲区中，如果是，
     * 则返回相应的frame_id。如果该页面还没有驻留在缓冲区中，
     * 则它会根据需要选择一个Victim Page，并加载到请求的页面中。
     */
    public int fixPage(int pageId, int prot) {
        BCB bcb = this.ptof[this.hash(pageId)];
        while(bcb != null && bcb.pageId != pageId) {
            bcb = bcb.next;
        }
        if(bcb != null) {
            // 页面在缓冲区中
            hitCounter++;
            LRUle p = this.getLRUEle(bcb.frameId);
            if(p == null) {
                throw new RuntimeException("buffer命中了，但是LRU链表中找不到对应的结点");
            } else{
                // 只有p不在表尾时才要调整
                if(p.next != null) {
                    if(p.pre == null) {
                        // 在开头，特殊处理
                        this.head = p.next;
                        this.head.pre = null;
                        p.next = null;
                        this.tail.next = p;
                        p.pre = this.tail;
                        p.next = null;
                        this.tail = p;
                    } else {
                        // 1.删除该结点
                        p.pre.next = p.next;
                        p.next.pre = p.pre;

                        // 2.将该节点放到表尾
                        this.tail.next = p;
                        p.pre = this.tail;
                        p.next = null;
                        this.tail = p;
                    }
                }
            }
            bcb.count++;
            return bcb.frameId;
        }
        // 缓存未命中
        int victimFrameId = this.selectVictim();
        BCB nowBcb = new BCB();
        nowBcb.pageId = pageId;
        nowBcb.frameId = victimFrameId;
        nowBcb.count++;

        if(ftop[victimFrameId] != -1) {
            // 如果这个frame已经被使用了，则先移除这个frame
            BCB victimBcb = ptof[hash(ftop[victimFrameId])];
            while(victimBcb != null && victimBcb.frameId != victimFrameId) {
                victimBcb = victimBcb.next;
            }
            if(victimBcb == null) {
                throw new RuntimeException("selectVictim未找到对应的页帧");
            }
            // System.out.println(victimBcb);
            // 移除LRU链表中的该元素，并且修改hash表
            this.removeBCB(victimBcb,victimBcb.pageId);
            this.ftop[victimBcb.frameId] = -1;
            this.removeLRUEle(victimBcb.frameId);
        }

        // 给新调入的页面分配BCB,并且修改哈希表以及LRU链表
        this.ftop[nowBcb.frameId] = nowBcb.pageId;
        BCB tmpBcb = this.ptof[this.hash(nowBcb.pageId)];
        if(tmpBcb == null) {
            this.ptof[this.hash(nowBcb.pageId)] = nowBcb;
        } else {
            while(tmpBcb.next != null) {
                tmpBcb = tmpBcb.next;
            }
            tmpBcb.next = nowBcb;
        }

        LRUle node = new LRUle();
        node.bcb = nowBcb;
        if(this.head == null && this.tail == null) {
            this.head = node;
            this.tail = node;
//            System.out.println(head);
//            System.out.println(tail);
        } else {
            this.tail.next = node;
            node.pre = this.tail;
            node.next = null;
            this.tail = node;
        }


//        if(x == 2) {
//            System.out.println(head);
//            System.out.println(tail);
//            System.exit(0);
//        }
        // 最后读/写入调入的页面
        try {
            if(prot == 0) {
                this.buf[nowBcb.frameId] = dataStorageManager.readPage(nowBcb.pageId);
            } else {
                this.buf[nowBcb.frameId] = new BFrame(new byte[FRAMESIZE]);
            }
        } catch (IOException e) {
            throw new RuntimeException("读入调入页面异常");
        }
        return nowBcb.frameId;
    }

    public int fixNewPage() {
        int[] pages = dataStorageManager.getPages();
        if(dataStorageManager.getNumPages() == pages.length) {
            return -1;
        }
        for(int pageId = 0; pageId < pages.length; pageId++) {
            if(pages[pageId] == 0) {
                dataStorageManager.setUse(pageId,MAXPAGES);
                dataStorageManager.incNumPages();
                fixPage(pageId,0);
                return pageId;
            }
        }
        return -1;
    }

    public int unFixPage(int pageId) {
        BCB bcb = ptof[this.hash(pageId)];
        while(bcb != null && bcb.pageId != pageId) {
            bcb = bcb.next;
        }
        if(bcb == null) {
            return -1;
        }
        else {
            bcb.count --;
            return bcb.frameId;
        }
    }

    public int numFreeFrames() { // 返回第一个可用的frameId
        int i = 0;
        while(i < DEFBUFSIZE && ftop[i] != -1) {
            ++i;
        }
        if(i == DEFBUFSIZE) {
            return -1;
        } else {
            return i;
        }
    }

    public int selectVictim() {
//        x++;
//        System.out.println(x);
        if(this.numFreeFrames() != -1) {
            return this.numFreeFrames();
        } else {
            LRUle p = this.head;
            // System.out.println(p);
            while(p.bcb.count != 0) {
                p = p.next;
            }
            return p.bcb.frameId;
        }

    }

    public int hash(int pageId) {
        return pageId % DEFBUFSIZE;
    }

    public void removeBCB(BCB ptr, int pageId) {
//        System.out.println("ptr:" + ptr);
//        System.out.println(pageId);
        BCB bcb = this.ptof[this.hash(pageId)];
        // System.out.println("1:" + bcb);
        if(bcb == null) {
            return;
        }
        if(bcb == ptr) {
            this.ptof[this.hash(pageId)] = bcb.next;
        } else {
            while(bcb.next != null && bcb.next != ptr) {
                // System.out.println(bcb);
                bcb = bcb.next;
            }
            if(bcb.next == null) {
                throw new RuntimeException("未找到指定的BCB");
            }
            bcb.next = ptr.next;
        }
        ptr.next = null;
        if(ptr.dirty == 1) {
            this.dataStorageManager.writePage(pageId,buf[ptr.frameId]);
            this.unSetDirty(ptr.frameId);
        }
    }

    public LRUle getLRUEle(int frameId) {
        LRUle p = this.tail;
        while(p != null && p.bcb.frameId != frameId) {
            p = p.pre;
        }
        if(p == null) {
            System.out.println("获取失败：LRU链表中找不到对应的frame");
        }
        return p;
    }

    public void removeLRUEle(int frameId) {
        if(this.head != null && this.head.bcb.frameId == frameId) {
            this.head = this.head.next;
            this.head.pre = null;
        }
        else if(this.tail != null && this.tail.bcb.frameId == frameId) {
            this.tail = this.tail.pre;
            this.tail.next = null;
        } else {
            LRUle p = this.head;
            while(p != null && p.bcb.frameId != frameId) {
                p = p.next;
            }
            if(p == null) {
                System.out.println("删除失败：LRU链表中找不到对应的frame");
            } else {
                p.pre.next = (p.next);
                p.next.pre = p.pre;
            }
        }
    }

    public void setDirty(int frameId) {
        int pid = this.ftop[frameId];
        int fid = this.hash(pid);
        BCB bcb = ptof[fid];
        while(bcb != null && bcb.pageId != pid) {
            bcb = bcb.next;
        }
        if(bcb != null) {
            bcb.dirty = 1;
        }
    }

    public void unSetDirty(int frameId) {
        int pid = this.ftop[frameId];
        int fid = this.hash(pid);
        BCB bcb = ptof[fid];
        while(bcb != null && bcb.pageId != pid) {
            bcb = bcb.next;
        }
        if(bcb != null) {
            bcb.dirty = 0;
        }
    }

    public void writeDirtys() throws IOException {
        int count = 0;
        for(BCB bcb : this.ptof) {
            while(bcb != null) {
                if(bcb.dirty == 1) {
                    count++;
                    dataStorageManager.writePage(bcb.frameId, buf[bcb.frameId]);
                    this.unSetDirty(bcb.frameId);
                }
                bcb = bcb.next;
            }
        }
        System.out.println("最后系统结束全部写回了" + count + "块");
    }

    public void printFrame(int frameId) {
        System.out.println(buf[frameId].filed);
    }

}
