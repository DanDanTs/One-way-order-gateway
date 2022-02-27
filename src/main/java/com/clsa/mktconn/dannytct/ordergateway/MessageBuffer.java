package com.clsa.mktconn.dannytct.ordergateway;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Danny Tsoi
 */
public class MessageBuffer {
    private byte[][] byteData = null;
    private volatile int readIdx  = 0;
    private volatile int writeIdx = 0;

    private int capacity;   // scalable
    private final int msgSize;

    private final ConcurrentLinkedQueue<byte[]> queue;
    
    public MessageBuffer(int capacity, int msgSize) {
        this.capacity = capacity;
        this.msgSize = msgSize;
        queue = new ConcurrentLinkedQueue<byte[]>();

        byteData = new byte[capacity][];
        for(int i=0; i<capacity; i++) byteData[i] = new byte[msgSize];
    }

    // expensive operation
    private void raiseBuffer() {
        synchronized(this) {
            int origCapacity = capacity;
            capacity *= 2;
            byte[][] tmp = byteData;

            // create a byte array with doubled size
            byteData = new byte[capacity][];
            for(int i=0; i<capacity; i++) {
                byteData[i] = new byte[msgSize];
            }
            int lenPart2 = origCapacity - readIdx - 1;

            // copy bytes after read index to new extended array
            for(int i=0; i<=lenPart2; i++)   // readidx -> part2 end
                System.arraycopy(tmp[i+readIdx], 0, byteData[i], 0, msgSize);

            // copy bytes before read index to new extended array
            for(int i=0; i<readIdx; i++)     // 0 -> readidx-1
                System.arraycopy(tmp[i], 0, byteData[i+lenPart2+1], 0, msgSize);

            readIdx = 0;   // reset to zero position
            System.out.println("Raise Buffer: new capacity = " + capacity);
        }
    }

    public boolean Enqueue(byte[] msg) {
        // copy to local buffer
        System.arraycopy(msg, 0, byteData[writeIdx], 0, msgSize);
        int nextIdx = writeIdx + 1;
        nextIdx = (nextIdx == capacity ? 0 : nextIdx);
        if (nextIdx == readIdx) {
            nextIdx = capacity;   // move to the end
            raiseBuffer(); 
        }

	queue.add(byteData[writeIdx]);
	writeIdx = nextIdx;   // move to next position
	return true;
    }

    public byte[] Dequeue() {
        synchronized(this) {
            int nextIdx = readIdx + 1;
            nextIdx = nextIdx == capacity ? 0 : nextIdx;
            if(queue.isEmpty()) return null;
            readIdx = nextIdx;
        }
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int Size() {
        return queue.size();
    }
}
