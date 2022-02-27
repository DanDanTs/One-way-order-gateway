package com.clsa.mktconn.dannytct.ordergateway;

import java.util.HashMap;


/**
 *
 * @author Danny Tsoi
 */
public class OrderValidationThread extends Thread {
    private volatile boolean isRunning = true;
    private final double priceRaiseLimit;
    private long procCount = 0;
    private long procBytes = 0;
    private long invalidPriceCount = 0;

    private HashMap<Short, Long> symLastPrices  = null;

    public OrderValidationThread(double priceRaiseLimit, int initSymCount) {
	this.symLastPrices = new HashMap<>(initSymCount);
        this.priceRaiseLimit = priceRaiseLimit;
    }

    public long getProcCount() { return procCount; }
    public long getProcBytes() { return procBytes; }
    public long getInvalidPriceCount() { return invalidPriceCount; }

    private boolean validateSymPrice(short symbol, long price) {
        Long lastPrice = symLastPrices.get(symbol);
        if ( null == lastPrice ) {   // first order
            symLastPrices.put(symbol, price);
            return true;
        }
        if ( price > lastPrice * priceRaiseLimit ) {
            return false;
        }
        symLastPrices.put(symbol, price);
        return true;
    }

    @Override
    public void run() {
	OrderMessageMapper msgMapper = new OrderMessageMapper();
	while ( isRunning )
	{
	    byte[] queuedData = OrderGateway.inboundQ.Dequeue();
	    if ( null != queuedData ) {
		msgMapper.Decode(queuedData);
		procCount++; procBytes += queuedData.length;

		if ( 'F' == msgMapper.MsgType ) {               // cancel order
		    OrderGateway.cancelClientOIDs.add(msgMapper.ClOrdID);
		}
		else if ( 'D' == msgMapper.MsgType ) {          // new order
		    if ( false == validateSymPrice(msgMapper.Symbol, msgMapper.Price) ) {
			invalidPriceCount++; continue;
		    }
		} 

		// enqueue valid msg for sending
		OrderGateway.outboundQ.Enqueue(queuedData);
	    } 
	}   // end while
    }
}
