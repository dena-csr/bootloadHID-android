/**
 * MainActivityTest.java
 * Copyright (c) 2016 DeNA Co., Ltd.
 *
 * This software is licensed under the GNU General Public License version 2.
 */
package com.dena.app.bootloadhid;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


public class MainActivityTest {
    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        activityTestRule.getActivity();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testButtons() throws Exception {
        //Espresso.onView(withId(R.id.buttonPick)).perform(click());
        //Espresso.onView(withId(R.id.buttonWrite)).perform(click());
    }
}
