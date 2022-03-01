package com.clsa.mktconn.dannytct.ordergateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 *
 * @author Danny Tsoi
 */
public class OrderGateway {
    // program settings
    final static String TCP_SEND_IP = "127.0.0.1";
    final static int TCP_SEND_PORT = 10001;
    final static int UDP_RECV_PORT = 10002;
    final static double PRICE_RAISE_LIMIT = 1.05;
    final static int CONSOLE_LOG_INTERVAL = 5;   // 5 secs
    final static int INIT_SYMBOL_COUNT = 20000;
    final static double EXTRA_BUFF_RAISE_FACTOR = 2.5;

    // shared members
    static volatile long localTimestampSec = 0;           // shared timestamp in application
    static MessageBuffer inboundQ  = null;
    static MessageBuffer outboundQ  = null;
    static Set cancelClientOIDs = null;          // for send thread to try dropping new orders

    public static void main(String [] args) throws IOException {
        int Outbound_Throttle = 1000;   // default 1000 msg/sec (configurable during start up)

        // assumption: avg inbound message rate not more than throttle (1000 TPS)
        //             peak traffic 10x => 10,000 TPS (x 10 secs)
        //             2.5x more than estimated as extra buffer
        int EstimatedPeakTraffic = (int) (10 * 10 * Outbound_Throttle * EXTRA_BUFF_RAISE_FACTOR);

        // get user input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            System.out.println(">> Please enter outbound throttle (press Enter use default): ");
            String s = br.readLine();
            if(false == s.trim().equals("")) {
                Outbound_Throttle = Integer.parseInt(s);
                EstimatedPeakTraffic = (int) (10 * 10 * Outbound_Throttle * EXTRA_BUFF_RAISE_FACTOR);
            }
        } catch(NumberFormatException | IOException e) {
            System.err.println("Main err:" + e);
            System.exit(0);
        }
        System.out.printf("Throttle: %d  Est.Peak: %d\n", Outbound_Throttle, EstimatedPeakTraffic);

        inboundQ  = new MessageBuffer(EstimatedPeakTraffic, OrderMessageMapper.MessageLength);
        outboundQ = new MessageBuffer(EstimatedPeakTraffic, OrderMessageMapper.MessageLength);
        cancelClientOIDs = ConcurrentHashMap.newKeySet(EstimatedPeakTraffic);        // get a concurrent hash set

        // starting threads
	ThrottleSendThread    sendThread = new ThrottleSendThread(TCP_SEND_IP, TCP_SEND_PORT, Outbound_Throttle);
	UDPReceiveThread      recvThread = new UDPReceiveThread(UDP_RECV_PORT);                             
	OrderValidationThread valdThread = new OrderValidationThread(PRICE_RAISE_LIMIT, INIT_SYMBOL_COUNT);

	recvThread.start();   // receive msg and enqueue to inbound queue
	valdThread.start();   // dequeue from inbound queue, validate, enqueue to outbound queue
	sendThread.start();   // connect server to trigger msg sending at last, send msg in outbound queue at defined throttle rate

        // main thread loop
        StringBuffer sb = new StringBuffer();
        long lastPrintTime = 0;
        while(true) {
            localTimestampSec = System.currentTimeMillis() / 1000;   // get local timestamp
        
            // print out system statistics on main thread every CONSOLE_LOG_INTERVAL secs
            if ( lastPrintTime + CONSOLE_LOG_INTERVAL < localTimestampSec) {
                sb.append("In=").append(recvThread.getRecvCount()).append(" (");
                sb.append(recvThread.getRecvBytes()).append("B), Proc=");
                sb.append(valdThread.getProcCount()).append(" (");
                sb.append(valdThread.getProcBytes()).append("B), Out=");
                sb.append(sendThread.getSendCount()).append(" (");
                sb.append(sendThread.getSendBytes()).append("B) ");
                sb.append("[Rule1=").append(valdThread.getInvalidPriceCount());
                sb.append(" Rule3=").append(sendThread.getCancelOutCount()).append("|").append(sendThread.getCancelDropCount()).append("]");
                System.out.println(sb);
                sb.delete(0, sb.length());
                lastPrintTime = localTimestampSec;
            }

            // manually quit application
            if ( br.ready() && 'q'==br.read() ) {
		System.out.println("Quit Application!"); 
		System.exit(0); 
            }
            try { Thread.sleep(1); } catch(InterruptedException e) { System.out.println(e); }
        }   // end while
    }   // end main()
}
