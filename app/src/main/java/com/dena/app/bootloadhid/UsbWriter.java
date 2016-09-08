/**
 * UsbWriter.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class UsbWriter {
    private static final String ACTION_USB_PERMISSION = "com.dena.app.bootloadhid.USB_PERMISSION";

    public interface OnDeviceListener {
        void onAttached();
        void onDetached();
    }

    public interface OnProgressListener {
        void onStart(int max);
        void onProgress(int progress);
        void onComplete();
    }

    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private PendingIntent mPermissionIntent;
    private int mVendorId;
    private int mProductId;
    private OnDeviceListener mOnDeviceListener;

    public void setDeviceListener(OnDeviceListener listener) {
        mOnDeviceListener = listener;
    }

    public void setVendorId(int vendorId) {
        mVendorId = vendorId;
    }
    public void setProductId(int productId) {
        mProductId = productId;
    }

    public void register(Context context) {
        mUsbManager = (UsbManager)context.getSystemService(Context.USB_SERVICE);
        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentfilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentfilter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(mReceiver, intentfilter);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(mReceiver);
    }

    private void clearDevice() {
        if (null != mConnection) {
            mConnection.close();
            mConnection = null;
            if (null != mOnDeviceListener) {
                mOnDeviceListener.onDetached();
            }
        }
        mDevice = null;
    }

    public void checkDevice() {
        setDevice(null);
        Map<String,UsbDevice> map = mUsbManager.getDeviceList();
        for (String key : map.keySet()) {
            UsbDevice device = map.get(key);
            if (setDevice(device)) {
                mOnDeviceListener.onAttached();
            }
            break;
        }
    }

    private void printDevice(UsbDevice device) {
        Logger.i("Vendor ID: " + device.getVendorId());
        Logger.i("Product ID: " + device.getProductId());
        Logger.d("  Name: " + device.getDeviceName());
        Logger.d("  ID: " + device.getDeviceId());
        Logger.d("  Class: " + device.getDeviceClass());
        Logger.d("  Protocol: " + device.getDeviceProtocol());
        Logger.d("  Interfaces: " + device.getInterfaceCount());
    }

    private boolean setDevice(UsbDevice device) {
        Logger.d("setDevice " + device);
        clearDevice();
        if (null == device) {
            return false;
        }
        if (device.getVendorId() != mVendorId) {
            printDevice(device);
            Logger.i("Not a target vendor: expecting %d", mVendorId);
            return false;
        }
        if (device.getProductId() != mProductId) {
            printDevice(device);
            Logger.i("Not a target product: expecting %d", mProductId);
            return false;
        }
        if (!mUsbManager.hasPermission(device)) {
            Logger.d("request permission");
            mUsbManager.requestPermission(device, mPermissionIntent);
            return false;
        }
        printDevice(device);
        try {
            UsbInterface usbinterface = device.getInterface(0);
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (!connection.claimInterface(usbinterface, true)) {
                return false;
            }
            mDevice = device;
            mConnection = connection;
            Logger.d("open SUCCESS");
            if (null != mOnDeviceListener) {
                mOnDeviceListener.onAttached();
            }
            return true;
        } catch (Exception e) {
            Logger.e(e, e.getLocalizedMessage());
        }
        return false;
    }

    public void writeData(String path, OnProgressListener listener) throws IOException {
        byte[] buffer = new byte[7];
        int result = getReport(HID_REPORT_TYPE_FEATURE, 1, buffer, buffer.length);
        if (result != buffer.length) {
            throw new IOException(String.format(Locale.US, "Not enough bytes in device info report (%d instead of %d)", result, buffer.length));
        }
        //dumpBytes(buffer, 0, buffer.length);
        int pageSize = getInt(buffer, 1, 2);
        int flashSize = getInt(buffer, 3, 4);
        Logger.i("PageSize = " + pageSize);
        Logger.i("FlashSize = " + flashSize);
        IntelHexParser hexData = new IntelHexParser();
        hexData.parseIntelHex(path);
        byte[] data = hexData.getData();
        //Logger.i("BinSize = " + data.length);
        if (flashSize - 2048 < hexData.getEndAddr()) {
            throw new IOException(String.format(Locale.US, "Data (%d bytes) exceeds remaining flash size!", hexData.getEndAddr()));
        }
        int mask = 127;
        int startAddr = hexData.getStartAddr() & ~mask;
        int endAddr = (hexData.getEndAddr() + mask) & ~mask;
        Logger.i("Writing %d (0x%x) bytes starting at %d (0x%x)", endAddr - startAddr, endAddr - startAddr, startAddr, startAddr);
        int progress = 0;
        listener.onStart(endAddr - startAddr);
        listener.onProgress(progress);
        int chunkHeaderSize = 4;
        int chunkBodySize = 128;
        byte[] chunk = new byte[chunkHeaderSize+chunkBodySize];
        while (startAddr < endAddr) {
            chunk[0] = HID_REPORT_TYPE_OUTPUT;
            setInt(chunk, 1, 3, startAddr);
            if (data.length < startAddr + chunkBodySize) {
                int len = data.length - startAddr;
                if (len < 0) {
                    break;
                }
                System.arraycopy(data, startAddr, chunk, chunkHeaderSize, len);
                Arrays.fill(chunk, chunkHeaderSize+len, chunk.length, (byte)0);
            } else {
                System.arraycopy(data, startAddr, chunk, chunkHeaderSize, chunkBodySize);
            }
            //dumpBytes(chunk, 1, chunkHeaderSize-1);
            //dumpBytes(chunk, 4, chunkBodySize);
            if (chunk.length != setReport(HID_REPORT_TYPE_FEATURE, chunk, chunk.length)) {
                throw new IOException("Failed at " + startAddr);
            }
            startAddr += chunkBodySize;
            progress += chunk.length;
            listener.onProgress(progress);
        }
        Logger.i("done");
        listener.onComplete();
        reboot();
    }

    private void reboot() {
        byte[] buffer = new byte[7];
        buffer[0] = 1;
        setReport(HID_REPORT_TYPE_FEATURE, buffer, buffer.length);
    }

    private int getInt(byte[] buffer, int offset, int len) {
        int value = 0;
        for (int i = 0; i < len; i++) {
            value |= (0xff & buffer[offset + i]) << (i * 8);
        }
        return value;
    }

    private void setInt(byte[] buffer, int offset, int len, int value) {
        for (int i = 0; i < len; i++) {
            buffer[offset + i] = (byte)(value & 0xff);
            value >>= 8;
        }
    }

    private int getReport(int reportType, int reportNumber, byte[] buffer, int len) {
        int result = mConnection.controlTransfer(USB_TYPE_CLASS|USB_DIR_IN|USB_RECIPIENT_INTERFACE, HID_REPORT_GET, reportType<<8|reportNumber, 0, buffer, len, 5000);
        if (result < 0) {
            Logger.e("Error receiving message");
        }
        return result;
    }

    private int setReport(int reportType, byte[] buffer, int len) {
        int result = mConnection.controlTransfer(USB_TYPE_CLASS|USB_DIR_OUT|USB_RECIPIENT_INTERFACE, HID_REPORT_SET, reportType<<8|buffer[0], 0, buffer, len, 5000);
        if (result < 0) {
            Logger.e("Error sending message");
        }
        return result;
    }

    private String bytesToHex(byte[] buf, int offset, int len) {
        StringBuilder builder = new StringBuilder();
        for (int i = offset, n = offset+len; i < n; i++) {
            builder.append(String.format(Locale.US, "%02x ", buf[i]));
        }
        return builder.toString();
    }

    private void dumpBytes(byte[] buf, int offset, int len) {
        for (int i = offset, n = Math.min(offset+len, buf.length), bytesPerLine = 16; /**/; i+=bytesPerLine) {
            if (n < i+bytesPerLine) {
                Logger.v(bytesToHex(buf, i, n-i));
                break;
            }
            Logger.v(bytesToHex(buf, i, bytesPerLine));
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Logger.d("onReceive " + action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        setDevice(device);
                    } else {
                        Logger.d("permission denied for device " + device);
                    }
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    setDevice(device);
                }
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (mDevice != null && mDevice.equals(device)) {
                    setDevice(null);
                }
            }
        }
    };

    private static final int USB_DIR_IN = 1<<7;
    private static final int USB_DIR_OUT = 0<<7;
    private static final int USB_TYPE_CLASS = 1<<5;
    private static final int USB_RECIPIENT_INTERFACE = 1;

    private static final int HID_REPORT_GET = 1;
    private static final int HID_REPORT_SET = 9;
    private static final int HID_REPORT_TYPE_FEATURE = 3;
    private static final int HID_REPORT_TYPE_OUTPUT = 2;
}
