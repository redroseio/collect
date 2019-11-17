/*
 * Copyright (C) 2012 University of Washington
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.activities.FormEntryActivity;
import com.redrosecps.collect.android.external.ExternalAppsUtils;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.widgets.utilities.StringWidgetUtils;
import com.redrosecps.collect.android.utilities.ToastUtils;

import timber.log.Timber;

import static com.redrosecps.collect.android.utilities.ApplicationConstants.RequestCodes;

/**
 * Launch an external app to supply an integer value. If the app
 * does not launch, enable the text area for regular data entry.
 * <p>
 * See {@link com.redrosecps.collect.android.widgets.ExStringWidget} for usage.
 */
public class ExIntegerWidget extends ExStringWidget {

    public ExIntegerWidget(Context context, QuestionDetails questionDetails) {
        super(context, questionDetails);
        StringWidgetUtils.adjustEditTextAnswerToIntegerWidget(answerText, questionDetails.getPrompt());
    }

    @Override
    protected void fireActivity(Intent i) throws ActivityNotFoundException {
        i.putExtra(DATA_NAME, StringWidgetUtils.getIntegerAnswerValueFromIAnswerData(getFormEntryPrompt().getAnswerValue()));
        try {
            ((Activity) getContext()).startActivityForResult(i, RequestCodes.EX_INT_CAPTURE);
        } catch (SecurityException e) {
            Timber.i(e);
            ToastUtils.showLongToast(R.string.not_granted_permission);
        }
    }

    @Override
    public IAnswerData getAnswer() {
        return StringWidgetUtils.getIntegerData(answerText.getText().toString(), getFormEntryPrompt());
    }

    /**
     * Allows answer to be set externally in {@link FormEntryActivity}.
     */
    @Override
    public void setBinaryData(Object answer) {
        IntegerData integerData = ExternalAppsUtils.asIntegerData(answer);
        answerText.setText(integerData == null ? null : integerData.getValue().toString());
        widgetValueChanged();
    }
}
