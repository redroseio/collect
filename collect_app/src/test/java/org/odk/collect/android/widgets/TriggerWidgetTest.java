package com.redrosecps.collect.android.widgets;

import androidx.annotation.NonNull;
import android.widget.CheckBox;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.junit.Test;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.widgets.base.QuestionWidgetTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author James Knight
 */

public class TriggerWidgetTest extends QuestionWidgetTest<TriggerWidget, StringData> {
    @NonNull
    @Override
    public TriggerWidget createWidget() {
        return new TriggerWidget(activity, new QuestionDetails(formEntryPrompt, "formAnalyticsID"));
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(TriggerWidget.OK_TEXT);
    }

    @Override
    public void getAnswerShouldReturnExistingAnswerIfPromptHasExistingAnswer() {
        super.getAnswerShouldReturnExistingAnswerIfPromptHasExistingAnswer();
        assertTrue(getWidget().getTriggerButton().isChecked());
    }

    @Test
    public void checkingTheTriggerBoxShouldSetTheAnswer() {
        TriggerWidget widget = getWidget();
        assertNull(widget.getAnswer());

        CheckBox triggerButton = widget.getTriggerButton();
        assertFalse(triggerButton.isChecked());

        triggerButton.setChecked(true);
        triggerButton.callOnClick();

        IAnswerData answer = widget.getAnswer();
        assertEquals(answer.getDisplayText(), TriggerWidget.OK_TEXT);
    }
}
