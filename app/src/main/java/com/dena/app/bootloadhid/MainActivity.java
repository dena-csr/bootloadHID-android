/**
 * MainActivity.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.app.ActionBar;

public class MainActivity extends AppCompatActivity {

    private CharSequence mTitle;

    protected void onCreate(Bundle savedInstanceState) {
        Logger.getInstance().setLevel(Logger.LEVEL_I);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTitle = getTitle();
        pushFragment(MainFragment.newInstance(getIntent().getData()));
    }

    public void pushFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment, fragment.getClass().getName())
                .addToBackStack(fragment.getClass().getName())
                .commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        restoreActionBar();
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_settings:
            pushFragment(SettingFragment.newInstance());
            return true;
        case R.id.action_about:
            pushFragment(AboutFragment.newInstance());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed() {
        if (1 < getSupportFragmentManager().getBackStackEntryCount()) {
            super.onBackPressed();
        } else {
            finish();
        }
    }
}
