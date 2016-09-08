/**
 * IntelHexParser.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class IntelHexParser {
    private byte[] mData;
    private int mEndAddr;
    private int mStartAddr;

    public byte[] getData() {
        return mData;
    }

    public int getStartAddr() {
        return mStartAddr;
    }

    public int getEndAddr() {
        return mEndAddr;
    }

    public IntelHexParser() {
    }

    public void parseIntelHex(String path) throws IOException {
        parseIntelHex(new File(path));
    }

    public void parseIntelHex(File file) throws IOException {
        parseIntelHex(new FileInputStream(file));
    }

    public void parseIntelHex(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(input.available());
        while (0 <= parseUntilColon(input)) {
            int lineLen = parseHex(input, 2);
            int sum = lineLen;
            int address = parseHex(input, 4);
            int base = address;
            sum += address >> 8;
            sum += address;
            int segment = parseHex(input, 2);
            sum += segment;
            if (0 != segment) {
                continue;
            }
            for (int i = 0; i < lineLen; address++, i++) {
                int d = parseHex(input, 2);
                output.write((byte)(0xff & d));
                sum += d;
            }
            sum += parseHex(input, 2);
            if (0 != (0xff & sum)) {
                Logger.i("Warning: Checksum error between address 0x" + Integer.toHexString(base) + "and 0x" + Integer.toHexString(address));
            }
            if (base < mStartAddr) {
                mStartAddr = base;
            }
            if (mEndAddr < address) {
                mEndAddr = address;
            }
        }
        mData = output.toByteArray();
    }
    
    private int parseUntilColon(InputStream input) throws IOException {
        int value;
        do {
            value = input.read();
        } while (':' != value && 0 < value);
        return value;
    }

    private int parseHex(InputStream input, int numDigits) throws IOException, NumberFormatException {
        byte[] buf = new byte[numDigits];
        for (int offset = 0; offset < numDigits; /**/) {
            int readLen = input.read(buf, offset, buf.length - offset);
            if (readLen < 0) {
                break;
            }
            offset += readLen;
        }
        return Integer.parseInt(new String(buf), 16);
    }

}
