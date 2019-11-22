package com.redrosecps.collect.android.espressoutils.pages;

import androidx.test.rule.ActivityTestRule;

import com.redrosecps.collect.android.R;

public class ErrorDialog extends OkDialog {
    ErrorDialog(ActivityTestRule rule) {
        super(rule);
    }

    @Override
    public ErrorDialog assertOnPage() {
        super.assertOnPage();
        checkIsStringDisplayed(R.string.error_occured);
        return this;
    }
}
