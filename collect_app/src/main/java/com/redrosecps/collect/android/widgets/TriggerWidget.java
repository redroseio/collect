/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.redrosecps.collect.android.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.util.TypedValue;
import android.widget.CheckBox;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.utilities.ViewIds;

@SuppressLint("ViewConstructor")
public class TriggerWidget extends QuestionWidget {

    public static final String OK_TEXT = "OK";

    private AppCompatCheckBox triggerButton;

    public TriggerWidget(Context context, QuestionDetails prompt) {
        super(context, prompt);

        setUpWidget();
    }

    @Override
    public void clearAnswer() {
        triggerButton.setChecked(false);
        widgetValueChanged();
    }

    @Override
    public IAnswerData getAnswer() {
        return triggerButton.isChecked() ? new StringData(OK_TEXT) : null;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        triggerButton.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        triggerButton.cancelLongPress();
    }

    public CheckBox getTriggerButton() {
        return triggerButton;
    }

    private void setUpWidget() {
        triggerButton = new AppCompatCheckBox(getContext());
        triggerButton.setId(ViewIds.generateViewId());
        triggerButton.setText(getContext().getString(R.string.trigger));
        triggerButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getAnswerFontSize());
        triggerButton.setEnabled(!getFormEntryPrompt().isReadOnly());
        triggerButton.setChecked(OK_TEXT.equals(getFormEntryPrompt().getAnswerText()));

        triggerButton.setOnCheckedChangeListener((buttonView, isChecked) -> widgetValueChanged());

        addAnswerView(triggerButton);
    }
}