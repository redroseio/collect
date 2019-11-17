package com.redrosecps.collect.android.formentry.questions;

import org.javarosa.form.api.FormEntryPrompt;
import com.redrosecps.collect.android.widgets.QuestionWidget;

/**
 * Data class representing a "question" for use with {@link QuestionWidget}
 * and its subclasses
 */
public class QuestionDetails {

    private final FormEntryPrompt prompt;
    private final String formAnalyticsID;

    public QuestionDetails(FormEntryPrompt prompt, String formAnalyticsID) {
        this.prompt = prompt;
        this.formAnalyticsID = formAnalyticsID;
    }

    public FormEntryPrompt getPrompt() {
        return prompt;
    }

    public String getFormAnalyticsID() {
        return formAnalyticsID;
    }
}
