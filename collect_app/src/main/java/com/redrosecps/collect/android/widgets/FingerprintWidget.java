package com.redrosecps.collect.android.widgets;

import java.io.File;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.util.StringUtil;

import com.google.api.client.util.StringUtils;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.activities.FormEntryActivity;
import com.redrosecps.collect.android.application.Collect;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.utilities.ApplicationConstants;
import com.redrosecps.collect.android.utilities.FileUtils;
import com.redrosecps.collect.android.utilities.MediaUtils;
import com.redrosecps.collect.android.widgets.interfaces.BinaryWidget;

public class FingerprintWidget extends QuestionWidget implements BinaryWidget {
    private final static String t = "FingerprintWidget";

    private Button mCaptureButton;
    private ImageView mImageView;

    private String mBinaryName;
    private String mInstanceFolder;
    private TextView mErrorTextView;

    private static final int PADDING = 20;

    public FingerprintWidget(Context context, QuestionDetails prompt) {
        super(context, prompt);
        mInstanceFolder = Collect.getInstance().getFormController().getInstanceFile().getParent();

        LinearLayout buttonLayout = createButtonLayout(context);

        mErrorTextView = createErrorTextView(context);
        mCaptureButton = createCaptureButton(context, prompt.getPrompt().isReadOnly());
        setupCaptureButtonListener(mCaptureButton, mErrorTextView, prompt.getPrompt());

        buttonLayout.addView(mCaptureButton);
        buttonLayout.addView(mErrorTextView);

        if (prompt.getPrompt().isReadOnly()) {
            mCaptureButton.setVisibility(View.GONE);
        }
        mErrorTextView.setVisibility(View.GONE);

        mBinaryName = prompt.getPrompt().getAnswerText();
        setupImageView(buttonLayout);


        addAnswerView(buttonLayout);
    }

    private void setupImageView(LinearLayout buttonLayout) {

        mImageView = new ImageView(getContext());
        mImageView.setAdjustViewBounds(true);
        mImageView.setPadding(10, 10, 10, 10);

        setFingerPrintToImageView();
        mImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                File imageFile = new File(mInstanceFolder + File.separator + mBinaryName);
                showImageInDialog(imageFile);
            }
        });
        buttonLayout.addView(mImageView);
        if (mBinaryName == null) {
            mImageView.setVisibility(View.GONE);
        }
    }

    private void setFingerPrintToImageView() {
        File imageFile = new File(mInstanceFolder + File.separator + mBinaryName);
        if (imageFile.exists()) {
            Point p = new Point();
            Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            display.getSize(p);
            int screenWidth = p.x;
            int screenHeight = p.y;
            Bitmap bmp = FileUtils.getBitmapScaledToDisplay(imageFile, screenHeight, screenWidth);
            mImageView.setImageBitmap(bmp);
        }
    }


    private void showImageInDialog(File imageFile) {
        if (imageFile.exists()) {
            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_image);
            ImageView imageView = dialog.findViewById(R.id.dialogImageView);
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            imageView.setImageBitmap(bitmap);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.show();
        } else {
            Toast.makeText(getContext(), "NO IMAGE EXISTS", Toast.LENGTH_SHORT).show();
        }
    }


    private LinearLayout createButtonLayout(Context context) {
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return buttonLayout;
    }

    private TextView createErrorTextView(Context context) {
        TextView errorTextView = new TextView(context);
        errorTextView.setText(R.string.selected_file_is_not_a_valid_fingerprint);
        return errorTextView;
    }

    private Button createCaptureButton(Context context, boolean isReadOnly) {
        Button captureButton = new Button(context);
        captureButton.setText(context.getString(R.string.capture_fingerprint));
        captureButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getAnswerFontSize());
        captureButton.setPadding(PADDING, PADDING, PADDING, PADDING);
        captureButton.setEnabled(!isReadOnly);
        return captureButton;
    }

    private void setupCaptureButtonListener(Button button, TextView errorTextView, FormEntryPrompt prompt) {
        button.setOnClickListener(v -> {
            errorTextView.setVisibility(View.GONE);
            Intent intent = new Intent("com.maviucak.android.redrose.SCAN_FINGERPRINT");
            try {
                Collect.getInstance().getFormController().setIndexWaitingForData(prompt.getIndex());
                ((Activity) getContext()).startActivityForResult(intent, ApplicationConstants.RequestCodes.FINGERPRINT_CAPTURE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                        getContext(),
                        getContext().getString(R.string.activity_not_found,
                                "Fingerprint capture (RedRose One Biometrics Installation is required!)"),
                        Toast.LENGTH_SHORT).show();
                Collect.getInstance().getFormController().setIndexWaitingForData(null);
            }
        });
    }

    private void deleteMedia() {
        // get the file path and delete the file
        String name = mBinaryName;
        // clean up variables
        mBinaryName = null;
        // delete from media provider
        int del = MediaUtils.deleteImageFileFromMediaProvider(mInstanceFolder + File.separator + name);
        Log.i(t, "Deleted " + del + " rows from media content provider");
        mImageView.setVisibility(View.GONE);
    }

    @Override
    public void clearAnswer() {
        // remove the file
        deleteMedia();
        mImageView.setImageBitmap(null);
        mErrorTextView.setVisibility(View.GONE);
        // reset buttons
        mCaptureButton.setText(getContext().getString(R.string.capture_image));
    }

    @Override
    public IAnswerData getAnswer() {
        if (mBinaryName != null) {
            return new StringData(mBinaryName.toString());
        } else {
            return null;
        }
    }

    @Override
    public void setBinaryData(Object newImageObj) {
        // you are replacing an answer. delete the previous image using the
        // content provider.
        if (mBinaryName != null) {
            deleteMedia();
        }

        File newImage = (File) newImageObj;
        if (newImage.exists()) {
            mBinaryName = newImage.getName();
            setFingerPrintToImageView();
            mImageView.setVisibility(View.VISIBLE);
            Log.i(t, "Setting current answer to " + newImage.getName());
        } else {
            Log.e(t, "NO IMAGE EXISTS at: " + newImage.getAbsolutePath());
        }

        Collect.getInstance().getFormController().setIndexWaitingForData(null);
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mCaptureButton.setOnLongClickListener(l);
        if (mImageView != null) {
            mImageView.setOnLongClickListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mCaptureButton.cancelLongPress();
        if (mImageView != null) {
            mImageView.cancelLongPress();
        }
    }

    @Override
    public void onButtonClick(int buttonId) {

    }
}
