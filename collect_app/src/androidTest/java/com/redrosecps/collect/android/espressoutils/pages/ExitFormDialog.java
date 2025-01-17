package com.redrosecps.collect.android.espressoutils.pages;

import androidx.test.rule.ActivityTestRule;

import com.redrosecps.collect.android.R;

public class ExitFormDialog extends Page<ExitFormDialog> {

    private final String formName;

    public ExitFormDialog(String formName, ActivityTestRule rule) {
        super(rule);
        this.formName = formName;
    }

    @Override
    public ExitFormDialog assertOnPage() {
        String title = getTranslatedString(R.string.exit) + " " + formName;
        checkIsTextDisplayed(title);
        return this;
    }
}
