/**
 * MainFragment.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

public class MainFragment extends Fragment {

    public static MainFragment newInstance(Uri uri) {
        MainFragment fragment = new MainFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("uri", uri);
        fragment.setArguments(bundle);
        return fragment;
    }

    public MainFragment() {
    }

    private Uri mUri;
    private String mFilePath;
    private Handler mHandler;
    private UsbWriter mWriter;
    private ProgressBar mProgressBar;
    private TextView mFileNameView;
    private TextView mLogcatTextView;
    private TextView mInstructions1View;
    private ScrollView mLogcatScrollView;
    private SharedPreferences mSharedPref;
    private AlertDialog mAlertDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mUri = getArguments().getParcelable("uri");
        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mHandler = new Handler();
        mProgressBar = (ProgressBar)rootView.findViewById(R.id.progressBar);
        mProgressBar.setScaleY(3f);
        rootView.findViewById(R.id.buttonPick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, 0);
            }
        });
        mLogcatTextView = (TextView)rootView.findViewById(R.id.logcat);
        mLogcatScrollView = (ScrollView)rootView.findViewById(R.id.scrollView);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mFileNameView = (TextView)rootView.findViewById(R.id.textFile);
        mInstructions1View = (TextView)rootView.findViewById(R.id.instructions1);
        mAlertDialog = new AlertDialog.Builder(getContext()).setCancelable(false).setTitle(getResources().getString(R.string.app_name)).create();
        if (null != mUri) {
            setFilePath(mUri.getPath());
        }
        startLogcat();
        createWriter();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        destroyWriter();
        stopLogcat();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWriter.unregister(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        mWriter.register(getContext());
        mWriter.setVendorId(getIntPref(R.string.preference_vendor_id, getResources().getInteger(R.integer.default_vendor_id)));
        mWriter.setProductId(getIntPref(R.string.preference_product_id, getResources().getInteger(R.integer.default_product_id)));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.write, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                clearLogcat();
                setFilePath(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK == resultCode && null != data) {
            Uri uri = data.getData();
            if (null != uri && "file".equalsIgnoreCase(uri.getScheme())) {
                setFilePath(uri.getPath());
            }
        }
    }

    public void setFilePath(String path) {
        mFilePath = path;
        if (null != path && 0 < path.length()) {
            Logger.i("file=" + path);
            mInstructions1View.setTextColor(Color.GRAY);
            mFileNameView.setText(new File(path).getName());
        } else {
            mInstructions1View.setTextColor(Color.BLACK);
            mFileNameView.setText("");
        }
    }

    private int getIntPref(int resId, int defaultValue) {
        try {
            return Integer.parseInt(mSharedPref.getString(getString(resId), ""));
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    private void writeDataAsync() {
        if (null == mFilePath) {
            return;
        }
        new AsyncTask<Object,Object,Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                try {
                    mWriter.writeData(mFilePath, new UsbWriter.OnProgressListener() {
                        @Override
                        public void onStart(final int max) {
                            mProgressBar.setMax(max);
                        }
                        @Override
                        public void onProgress(final int progress) {
                            mProgressBar.setProgress(progress);
                        }
                        @Override
                        public void onComplete() {
                            alert(getResources().getString(R.string.write_done));
                        }
                    });
                } catch (final Exception e) {
                    Logger.e(e.getMessage(), e);
                    alert(getResources().getString(R.string.write_failed));
                }
                return null;
            }
            @Override
            protected void onPostExecute(Object result) {
            }
        }.execute();
    }

    private void alert(final String message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mAlertDialog.isShowing()) {
                    mAlertDialog.dismiss();
                }
                mWriter.unregister(getContext());
                mAlertDialog.setMessage(message);
                mAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mWriter.register(getContext());
                    }
                });
                mAlertDialog.show();
            }
        });
    }

    private void createWriter() {
        if (null != mWriter) {
            return;
        }
        mWriter = new UsbWriter();
        mWriter.setDeviceListener(new UsbWriter.OnDeviceListener() {
            @Override
            public void onAttached() {
                writeDataAsync();
            }
            @Override
            public void onDetached() {
            }
        });
    }

    private void destroyWriter() {
        if (null == mWriter) {
            return;
        }
        mWriter = null;
    }

    private void startLogcat() {
        flushLogcat(Logger.getInstance());
        Logger.getInstance().setListener(new Logger.OnLogListener() {
            @Override
            public void onLog(Logger logger) {
                flushLogcat(logger);
            }
        });
    }

    private void stopLogcat() {
        Logger.getInstance().setListener(null);
    }

    private void clearLogcat() {
        flushLogcat(Logger.getInstance());
        mLogcatTextView.post(new Runnable() {
            @Override
            public void run() {
                mLogcatTextView.setText("");
            }
        });
    }

    private void flushLogcat(Logger logger) {
        while (!logger.isEmpty()) {
            appendLogcat(logger.dequeue());
        }
    }

    private void appendLogcat(final String message) {
        mLogcatTextView.post(new Runnable() {
            @Override
            public void run() {
                mLogcatTextView.append(message + "\n");
            }
        });
        mLogcatScrollView.post(new Runnable() {
            @Override
            public void run() {
                mLogcatScrollView.smoothScrollTo(0, mLogcatScrollView.getBottom());
            }
        });
    }

}
