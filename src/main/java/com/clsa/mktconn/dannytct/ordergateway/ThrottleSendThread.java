package com.clsa.mktconn.dannytct.ordergateway;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


/**
 *
 * @author Danny Tsoi
 */
public class ThrottleSendThread extends Thread {
    private volatile boolean isRunning = true;
    private final String sendIP;
    private final int sendPort;
    private final int throttlePerSec;
    private int msgSendInSec = 0;   // local throttle send control, reset every sec
    private Socket sendSock = null;

    private long sendCount = 0;
    private long sendBytes = 0;
    private long cancelOutCount = 0;    // new order commands dropped
    private long cancelDropCount = 0;   // cancel order commands dropped

    public ThrottleSendThread(String ip, int port, int tps){
        sendIP = ip;
        sendPort = port;
        throttlePerSec = tps;
    }

    public long getSendCount() { return sendCount; }
    public long getSendBytes() { return sendBytes; }
    public long getCancelOutCount() { return cancelOutCount; }
    public long getCancelDropCount() { return cancelDropCount; }

    @Override
    public void run() {
        long lastTimeStamp = 0;
	try {
	    sendSock = new Socket(sendIP, sendPort);
            OutputStream ostream = sendSock.getOutputStream();
            OrderMessageMapper msgMapper = new OrderMessageMapper();

	    while ( isRunning )
	    {
                // update timestamp in sec
                if ( OrderGateway.localTimestampSec > lastTimeStamp ) {
                    msgSendInSec = 0;   // reset throttle every sec
                    lastTimeStamp = OrderGateway.localTimestampSec;
                }
                // throttle control
                if ( msgSendInSec >= throttlePerSec ) continue;

                byte[] queuedData = OrderGateway.outboundQ.Dequeue();
                if(queuedData != null) {
                    msgMapper.Decode(queuedData);

                    if ( 'D' == msgMapper.MsgType ) {
                        if ( OrderGateway.cancelClientOIDs.remove(msgMapper.ClOrdID) ) {
                            cancelOutCount++; continue;
                        }
                    } else if ( 'F' == msgMapper.MsgType ) { 
                        if ( false == OrderGateway.cancelClientOIDs.contains(msgMapper.ClOrdID) ) {
                            cancelDropCount++; continue;
                        } else { 
                            OrderGateway.cancelClientOIDs.remove(msgMapper.ClOrdID);
                        }
                    }

                    // valid to send out
                    ostream.write(queuedData);
                    msgSendInSec++;   // consume throttle
                    sendCount++; sendBytes += queuedData.length;
                }
            }   // end while
	}
        catch (IOException ex) { 
            System.out.println("TS: " + ex); 
        }
    }
}
