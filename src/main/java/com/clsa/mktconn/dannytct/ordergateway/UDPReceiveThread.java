package com.clsa.mktconn.dannytct.ordergateway;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 *
 * @author Danny Tsoi
 */
public class UDPReceiveThread extends Thread {
    private volatile boolean isRunning = true;
    private final int recvPort;
    private DatagramSocket recvSocket = null;

    // statistics
    private long recvCount = 0;
    private long recvBytes = 0;
    private long bufFullCount = 0;

    public UDPReceiveThread(int port) {
        recvPort = port;
    }
    public long getRecvCount() { return recvCount; }
    public long getRecvBytes() { return recvBytes; }
    public long getBufFullCount() { return bufFullCount; } 

    @Override
    public void run() {    
	try {
	    byte[] receiveData = new byte[OrderMessageMapper.MessageLength];
	    DatagramPacket receivePacket = 
                new DatagramPacket(receiveData, receiveData.length);

            recvSocket = new DatagramSocket(recvPort);
            System.out.printf("Listening on UDP port: %d\n", recvPort);

	    while ( isRunning )
	    {
		recvSocket.receive(receivePacket);         // blocking
                int nBytes = receivePacket.getLength();

                if(nBytes == OrderMessageMapper.MessageLength) {
                    while(false == OrderGateway.inboundQ.Enqueue(receiveData))
                        bufFullCount++;
                    recvCount++; recvBytes += nBytes; 
                } else {
                    System.out.printf("Unexpected msg length received: %d\n", nBytes);
                }
	    }
	} catch (IOException e) {
	    System.out.println("UR: " + e);
	} finally {
	    recvSocket.close();
	}
    }
}
