package com.redrosecps.collect.android.widgets;

import androidx.annotation.NonNull;

import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.widgets.base.GeneralSelectOneWidgetTest;

/**
 * @author James Knight
 */

public class SelectOneWidgetTest extends GeneralSelectOneWidgetTest<AbstractSelectOneWidget> {

    @NonNull
    @Override
    public SelectOneWidget createWidget() {
        return new SelectOneWidget(activity, new QuestionDetails(formEntryPrompt, "formAnalyticsID"), false);
    }
}
