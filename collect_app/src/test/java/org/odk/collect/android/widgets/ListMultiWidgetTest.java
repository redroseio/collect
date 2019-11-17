package com.redrosecps.collect.android.widgets;

import androidx.annotation.NonNull;

import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.widgets.base.GeneralSelectMultiWidgetTest;

/**
 * @author James Knight
 */

public class ListMultiWidgetTest extends GeneralSelectMultiWidgetTest<ListMultiWidget> {
    @NonNull
    @Override
    public ListMultiWidget createWidget() {
        return new ListMultiWidget(activity, new QuestionDetails(formEntryPrompt, "formAnalyticsID"), true);
    }
}
