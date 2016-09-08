/**
 * SettingFragment.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class SettingFragment extends PreferenceFragmentCompat {

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    public SettingFragment() {
    }

    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preference);
        onCreateEditTextPreference(R.string.preference_vendor_id);
        onCreateEditTextPreference(R.string.preference_product_id);
    }

    private void onCreateEditTextPreference(int resId) {
        EditTextPreference pref = (EditTextPreference)getPreferenceManager().findPreference(getString(resId));
        pref.setSummary(pref.getText());
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue.toString());
                return true;
            }
        });
    }

}
