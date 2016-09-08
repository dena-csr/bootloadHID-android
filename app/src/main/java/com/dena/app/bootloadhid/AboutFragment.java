/**
 * AboutFragment.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends Fragment {

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    public AboutFragment() {
    }

    public View onCreateView(LayoutInflater layoutinflater, ViewGroup viewgroup, Bundle bundle) {
        View view = layoutinflater.inflate(R.layout.fragment_about, null);
        mActionBar = getActivity().getActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle(getString(R.string.title_about));
        }
        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        String version = BuildConfig.VERSION_NAME + " (" + BuildConfig.GIT_HASH + ")";
        ((TextView)view.findViewById(R.id.textVersion)).setText(version);
    }

    public void onDestroyView() {
        super.onDestroyView();
        if (mActionBar != null) {
            mActionBar.setTitle(getString(R.string.app_name));
        }
    }

    public void onStart() {
        super.onStart();
    }

    private ActionBar mActionBar;
}
