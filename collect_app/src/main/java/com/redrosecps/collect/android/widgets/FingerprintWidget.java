package com.redrosecps.collect.android.widgets;

import java.io.File;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.activities.FormEntryActivity;
import com.redrosecps.collect.android.application.Collect;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.utilities.ApplicationConstants;
import com.redrosecps.collect.android.utilities.FileUtils;
import com.redrosecps.collect.android.utilities.MediaUtils;

public class FingerprintWidget extends QuestionWidget implements IBinaryWidget
{
	private final static String t = "FingerprintWidget";

	private Button mCaptureButton;
	private ImageView mImageView;

	private String mBinaryName;
	private String mInstanceFolder;
	private TextView mErrorTextView;

	public FingerprintWidget(Context context, QuestionDetails prompt)
	{
		super(context, prompt);
		mInstanceFolder = Collect.getInstance().getFormController().getInstanceFile().getParent();

		TableLayout.LayoutParams params = new TableLayout.LayoutParams();
		params.setMargins(7, 5, 7, 5);

		mErrorTextView = new TextView(context);
		//mErrorTextView.setId(QuestionWidget.newUniqueId());
		mErrorTextView.setText("Selected file is not a valid fingerprint");

		// setup capture button
		mCaptureButton = new Button(getContext());
		//mCaptureButton.setId(QuestionWidget.newUniqueId());
		mCaptureButton.setText(getContext().getString(R.string.capture_fingerprint));
		mCaptureButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getAnswerFontSize());
		mCaptureButton.setPadding(20, 20, 20, 20);
		mCaptureButton.setEnabled(!prompt.getPrompt().isReadOnly());
		mCaptureButton.setLayoutParams(params);

		// launch capture intent on click
		mCaptureButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mErrorTextView.setVisibility(View.GONE);
				Intent i = new Intent("com.maviucak.android.redrose.SCAN_FINGERPRINT");
				//i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(Collect.TMPFILE_PATH)));
				try
				{
					Collect.getInstance().getFormController().setIndexWaitingForData(getFormEntryPrompt().getIndex());
					((Activity)getContext()).startActivityForResult(i, ApplicationConstants.RequestCodes.FINGERPRINT_CAPTURE);
				}
				catch (ActivityNotFoundException e)
				{
					Toast.makeText(
							getContext(),
							getContext().getString(R.string.activity_not_found,
									"Fingerprint capture (RedRose One App Installation is required!)"),
							Toast.LENGTH_SHORT).show();
					Collect.getInstance().getFormController().setIndexWaitingForData(null);
				}

			}
		});

		// finish complex layout
		addAnswerView(mCaptureButton);
		addAnswerView(mErrorTextView);

		// and hide the capture and choose button if read-only
		if (prompt.getPrompt().isReadOnly())
		{
			mCaptureButton.setVisibility(View.GONE);
		}
		mErrorTextView.setVisibility(View.GONE);

		// retrieve answer from data model and update ui
		mBinaryName = prompt.getPrompt().getAnswerText();

		// Only add the imageView if the user has taken a picture
		if (mBinaryName != null)
		{
			mImageView = new ImageView(getContext());
			//mImageView.setId(View.NO_ID);
			Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay();
			Point p = new Point();
			display.getSize(p);
			int screenWidth = p.x;
			int screenHeight = p.y;

			File f = new File(mInstanceFolder + File.separator + mBinaryName);

			if (f.exists())
			{
				Bitmap bmp = FileUtils.getBitmapScaledToDisplay(f, screenHeight, screenWidth);
				if (bmp == null)
				{
					mErrorTextView.setVisibility(View.VISIBLE);
				}
				mImageView.setImageBitmap(bmp);
			}
			else
			{
				mImageView.setImageBitmap(null);
			}

			mImageView.setPadding(10, 10, 10, 10);
			mImageView.setAdjustViewBounds(true);
			mImageView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Intent i = new Intent("android.intent.action.VIEW");
					Uri uri = MediaUtils.getImageUriFromMediaProvider(mInstanceFolder + File.separator + mBinaryName);
					if (uri != null)
					{
						Log.i(t, "setting view path to: " + uri);
						i.setDataAndType(uri, "image/*");
						try
						{
							getContext().startActivity(i);
						}
						catch (ActivityNotFoundException e)
						{
							Toast.makeText(getContext(),
									getContext().getString(R.string.activity_not_found, "view image"),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			});

			addView(mImageView);
		}
	}

	private void deleteMedia()
	{
		// get the file path and delete the file
		String name = mBinaryName;
		// clean up variables
		mBinaryName = null;
		// delete from media provider
		int del = MediaUtils.deleteImageFileFromMediaProvider(mInstanceFolder + File.separator + name);
		Log.i(t, "Deleted " + del + " rows from media content provider");
	}

	@Override
	public void clearAnswer()
	{
		// remove the file
		deleteMedia();
		mImageView.setImageBitmap(null);
		mErrorTextView.setVisibility(View.GONE);

		// reset buttons
		mCaptureButton.setText(getContext().getString(R.string.capture_image));
	}

	@Override
	public IAnswerData getAnswer()
	{
		if (mBinaryName != null)
		{
			return new StringData(mBinaryName.toString());
		}
		else
		{
			return null;
		}
	}

	@Override
	public void setBinaryData(Object newImageObj)
	{
		// you are replacing an answer. delete the previous image using the
		// content provider.
		if (mBinaryName != null)
		{
			deleteMedia();
		}

		File newImage = (File)newImageObj;
		if (newImage.exists())
		{
			// Add the new image to the Media content provider so that the
			// viewing is fast in Android 2.0+
			ContentValues values = new ContentValues(6);
			values.put(Images.Media.TITLE, newImage.getName());
			values.put(Images.Media.DISPLAY_NAME, newImage.getName());
			values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
			values.put(Images.Media.MIME_TYPE, "image/png");
			values.put(Images.Media.DATA, newImage.getAbsolutePath());

			Uri imageURI = getContext().getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
			Log.i(t, "Inserting image returned uri = " + imageURI.toString());

			mBinaryName = newImage.getName();
			Log.i(t, "Setting current answer to " + newImage.getName());
		}
		else
		{
			Log.e(t, "NO IMAGE EXISTS at: " + newImage.getAbsolutePath());
		}

		Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}

	@Override
	public void setFocus(Context context)
	{
		// Hide the soft keyboard if it's showing.
		InputMethodManager inputManager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
	}

	@Override
	public boolean isWaitingForBinaryData()
	{
		return getFormEntryPrompt().getIndex().equals(Collect.getInstance().getFormController().getIndexWaitingForData());
	}

	@Override
	public void cancelWaitingForBinaryData()
	{
		Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l)
	{
		mCaptureButton.setOnLongClickListener(l);
		if (mImageView != null)
		{
			mImageView.setOnLongClickListener(l);
		}
	}

	@Override
	public void cancelLongPress()
	{
		super.cancelLongPress();
		mCaptureButton.cancelLongPress();
		if (mImageView != null)
		{
			mImageView.cancelLongPress();
		}
	}

}
