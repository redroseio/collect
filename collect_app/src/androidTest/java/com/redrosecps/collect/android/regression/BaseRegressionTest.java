package com.redrosecps.collect.android.regression;

import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import com.redrosecps.collect.android.activities.MainMenuActivity;

public class BaseRegressionTest {

    @Rule
    public ActivityTestRule<MainMenuActivity> main = new ActivityTestRule<>(MainMenuActivity.class);
}