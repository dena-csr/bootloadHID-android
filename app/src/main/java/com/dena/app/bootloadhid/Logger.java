/**
 * Logger.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

public class Logger {

    public static final String TAG = "bootloadHID";
    public static final int LEVEL_E = 0;
    public static final int LEVEL_W = 1;
    public static final int LEVEL_I = 2;
    public static final int LEVEL_D = 3;
    public static final int LEVEL_V = 4;

    public interface OnLogListener {
        void onLog(Logger logger);
    }

    private static Logger sInstance;

    private int mLevel;
    private Queue<String> mQueue;
    private OnLogListener mListener;

    public static Logger getInstance() {
        if (null == sInstance) {
            sInstance = new Logger();
        }
        return sInstance;
    }

    public Logger() {
        mLevel = LEVEL_I;
        mQueue = new ConcurrentLinkedQueue<>();
    }

    public static void v(String format, Object... args) {
        if (LEVEL_V <= getInstance().getLevel()) {
            Log.v(TAG, getInstance().enqueue(format, args));
        }
    }

    public static void i(String format, Object... args) {
        if (LEVEL_I <= getInstance().getLevel()) {
            Log.i(TAG, getInstance().enqueue(format, args));
        }
    }

    public static void d(String format, Object... args) {
        if (LEVEL_D <= getInstance().getLevel()) {
            Log.d(TAG, getInstance().enqueue(format, args));
        }
    }

    public static void w(String format, Object... args) {
        if (LEVEL_W <= getInstance().getLevel()) {
            Log.w(TAG, getInstance().enqueue(format, args));
        }
    }

    public static void e(String format, Object... args) {
        if (LEVEL_E <= getInstance().getLevel()) {
            Log.e(TAG, getInstance().enqueue(format, args));
        }
    }

    public static void e(Throwable tr, String format, Object... args) {
        if (LEVEL_E <= getInstance().getLevel()) {
            Log.e(TAG, getInstance().enqueue(format, args), tr);
        }
    }

    private int getLevel() {
        return mLevel;
    }

    public void setLevel(int level) {
        mLevel = level;
    }

    public void setListener(OnLogListener listener) {
        mListener = listener;
    }

    private String enqueue(String format, Object... args) {
        return enqueue((null == args) ? format : String.format(Locale.US, format, args));
    }

    private String enqueue(String message) {
        mQueue.offer(message);
        if (null != mListener) {
            mListener.onLog(this);
        }
        return message;
    }

    public boolean isEmpty() {
        return mQueue.isEmpty();
    }

    public String dequeue() {
        return mQueue.poll();
    }

}
