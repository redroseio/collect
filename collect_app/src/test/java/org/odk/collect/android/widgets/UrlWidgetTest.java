package com.redrosecps.collect.android.widgets;

import androidx.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.data.StringData;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.widgets.base.QuestionWidgetTest;

/**
 * @author James Knight
 */

public class UrlWidgetTest extends QuestionWidgetTest<UrlWidget, StringData> {
    @NonNull
    @Override
    public UrlWidget createWidget() {
        return new UrlWidget(activity, new QuestionDetails(formEntryPrompt, "formAnalyticsID"));
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(RandomString.make());
    }

    @Override
    public void callingClearShouldRemoveTheExistingAnswer() {
        // The widget is ReadOnly, clear shouldn't do anything.
    }
}
