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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.app.Activity;
import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.integration.android.IntentIntegrator;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import java.util.ArrayList;
import java.util.List;
import com.redrosecps.collect.android.application.Collect;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.activities.ScannerWithFlashlightActivity;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.listeners.PermissionListener;
import com.redrosecps.collect.android.utilities.CameraUtils;
import com.redrosecps.collect.android.utilities.ToastUtils;
import com.redrosecps.collect.android.utilities.WidgetAppearanceUtils;
import com.redrosecps.collect.android.widgets.interfaces.BinaryWidget;

/**
 * Widget that allows user to scan barcodes and add them to the form.
 *
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class FormattedBarcodeWidget extends QuestionWidget implements BinaryWidget {
	private final Button getBarcodeButton;
	private final TextView stringAnswer;
	private List<SelectChoice> mItems = new ArrayList<SelectChoice>();

	public FormattedBarcodeWidget(Context context, QuestionDetails questionDetails) {
		super(context, questionDetails);

		getBarcodeButton = getSimpleButton(getContext().getString(R.string.get_barcode));

		stringAnswer = getCenteredAnswerTextView();

		List<Selection> ve = new ArrayList<Selection>();
		mItems = new ArrayList<SelectChoice>();

		if (questionDetails.getPrompt().getAnswerValue() != null && questionDetails.getPrompt().getAnswerValue().getValue() instanceof List)
			ve = (List<Selection>)questionDetails.getPrompt().getAnswerValue().getValue();
		StringBuilder sb = new StringBuilder();
		if (ve != null)
		{
			for (int i = 0; i < ve.size(); i++)
			{
				sb.append(ve.get(i).choice.getValue());
				if (i < ve.size() - 1)
					sb.append("\n");
				mItems.add(ve.get(i).choice);
			}
		}
		stringAnswer.setText(sb.toString());

		if (mItems != null && mItems.size() > 0)
			getBarcodeButton.setText(getContext().getString(R.string.replace_barcode));
		// finish complex layout
		LinearLayout answerLayout = new LinearLayout(getContext());
		answerLayout.setOrientation(LinearLayout.VERTICAL);
		answerLayout.addView(getBarcodeButton);
		answerLayout.addView(stringAnswer);
		addAnswerView(answerLayout);
	}

	private static String extractTextID(FormEntryPrompt prompt)
	{
		try
		{
			Method method = FormEntryCaption.class.getDeclaredMethod("getTextID", new Class[] {});
			method.setAccessible(true);
			String result = (String)method.invoke(prompt, new Object[] {});
			return result;
		}
		catch (NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
		catch (IllegalArgumentException e)
		{
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void setAnswerText(FormEntryPrompt prompt, String answerText)
	{
		String separator = prompt.getAppearanceHint().replace("formatted:", "");

		if (answerText != null)
		{
			String[] parts = answerText.split(separator);
			this.mItems = new ArrayList<SelectChoice>();
			String textIDBase = extractTextID(prompt).replace(":label", "");
			for (String part : parts)
			{
				if (part == null || part.length() == 0)
					part = "-";

				part = part.replaceAll(" ", " ");
				int index = mItems.size();
				String barcodeId = "barcode" + index;
				String textID = textIDBase + "/" + barcodeId + ":label";
				SelectChoice choice = new SelectChoice(textID, null, part, false);
				choice.setIndex(index);
				mItems.add(choice);
			}
		}
	}

	@Override
	public void clearAnswer() {
		stringAnswer.setText(null);
		getBarcodeButton.setText(getContext().getString(R.string.get_barcode));
		mItems = new ArrayList<SelectChoice>();
		widgetValueChanged();
	}

	@Override
	public IAnswerData getAnswer()
	{
		List<Selection> vc = new ArrayList<Selection>();
		for (int i = 0; i < mItems.size(); ++i)
		{
			Selection selection = new Selection(mItems.get(i));
			//selection.xmlValue = mItems.get(i).getLabelInnerText();
			vc.add(selection);
		}

		if (vc.size() == 0)
		{
			return null;
		}
		else
		{
			return new SelectMultiData(vc);
		}
	}

	@Override
	public void setBinaryData(Object answer)
	{
		setAnswerText(getFormEntryPrompt(), (String)answer);
		Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		stringAnswer.setOnLongClickListener(l);
		getBarcodeButton.setOnLongClickListener(l);
	}

	@Override
	public void cancelLongPress() {
		super.cancelLongPress();
		getBarcodeButton.cancelLongPress();
		stringAnswer.cancelLongPress();
	}

	@Override
	public void onButtonClick(int buttonId) {
		getPermissionUtils().requestCameraPermission((Activity) getContext(), new PermissionListener() {
			@Override
			public void granted() {
				waitForData();

				IntentIntegrator intent = new IntentIntegrator((Activity) getContext())
						.setCaptureActivity(ScannerWithFlashlightActivity.class)
						.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
						.setOrientationLocked(false)
						.setPrompt(getContext().getString(R.string.barcode_scanner_prompt));

				setCameraIdIfNeeded(intent);
				intent.initiateScan();
			}

			@Override
			public void denied() {
			}
		});
	}

	private void setCameraIdIfNeeded(IntentIntegrator intent) {
		String appearance = getFormEntryPrompt().getAppearanceHint();
		if (appearance != null && appearance.equalsIgnoreCase(WidgetAppearanceUtils.FRONT)) {
			if (CameraUtils.isFrontCameraAvailable()) {
				intent.setCameraId(CameraUtils.getFrontCameraId());
				intent.addExtra(WidgetAppearanceUtils.FRONT, true);
			} else {
				ToastUtils.showLongToast(R.string.error_front_camera_unavailable);
			}
		}
	}
}
