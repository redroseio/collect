package com.redrosecps.collect.android.widgets;

import androidx.annotation.NonNull;

import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.widgets.base.GeneralSelectOneWidgetTest;

/**
 * @author James Knight
 */

public class ListWidgetTest extends GeneralSelectOneWidgetTest<ListWidget> {
    @NonNull
    @Override
    public ListWidget createWidget() {
        return new ListWidget(activity, new QuestionDetails(formEntryPrompt, "formAnalyticsID"), false, false);
    }
}
