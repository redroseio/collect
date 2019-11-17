package com.redrosecps.collect.android.widgets;

import androidx.annotation.NonNull;

import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.widgets.base.GeneralSelectOneWidgetTest;

/**
 * @author James Knight
 */

public class SpinnerWidgetTest extends GeneralSelectOneWidgetTest<SpinnerWidget> {
    @NonNull
    @Override
    public SpinnerWidget createWidget() {
        return new SpinnerWidget(activity, new QuestionDetails(formEntryPrompt, "formAnalyticsID"), false);
    }
}
