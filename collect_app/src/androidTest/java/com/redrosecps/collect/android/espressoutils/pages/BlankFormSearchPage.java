package com.redrosecps.collect.android.espressoutils.pages;

import androidx.test.rule.ActivityTestRule;

import com.redrosecps.collect.android.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;

public class BlankFormSearchPage extends Page<BlankFormSearchPage> {

    public BlankFormSearchPage(ActivityTestRule rule) {
        super(rule);
    }

    @Override
    public BlankFormSearchPage assertOnPage() {
        onView(withHint(getTranslatedString(R.string.search))).check(matches(isDisplayed()));
        return this;
    }
}
