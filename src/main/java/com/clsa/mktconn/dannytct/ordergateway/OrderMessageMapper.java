/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.clsa.mktconn.dannytct.ordergateway;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Danny Tsoi
 */
public class OrderMessageMapper {
    public static int MessageLength = 21;

    // data fields
    public long MsgLength = 0;
    public char  MsgType   = ' ';   // 2 bytes, but 1 byte in msg
    public long  ClOrdID   = 0;
    public short Symbol    = 0;
    public long  Price     = 0;
    private ByteBuffer bbuf = null;

    public void Decode(byte [] bMsg) {
        bbuf = ByteBuffer.wrap(bMsg);
        bbuf.order(ByteOrder.LITTLE_ENDIAN);

        MsgLength = bbuf.getShort();
        MsgType   = (char)bbuf.get();
        ClOrdID   = bbuf.getLong();
        Symbol    = bbuf.getShort();
        Price     = bbuf.getLong();
    }
}
