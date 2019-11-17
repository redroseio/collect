package com.redrosecps.collect.android.widgets;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import net.bytebuddy.utility.RandomString;

import org.javarosa.core.model.data.StringData;
import org.junit.Test;
import org.mockito.Mock;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.audio.AudioControllerView;
import com.redrosecps.collect.android.formentry.questions.QuestionDetails;
import com.redrosecps.collect.android.utilities.FileUtil;
import com.redrosecps.collect.android.utilities.MediaUtil;
import com.redrosecps.collect.android.widgets.base.FileWidgetTest;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author James Knight
 */
public class AudioWidgetTest extends FileWidgetTest<AudioWidget> {

    @Mock
    Uri uri;

    @Mock
    MediaUtil mediaUtil;

    @Mock
    FileUtil fileUtil;

    @Mock
    AudioControllerView audioController;

    private String destinationName;

    @NonNull
    @Override
    public AudioWidget createWidget() {
        return new AudioWidget(activity, new QuestionDetails(formEntryPrompt, "formAnalyticsID"), fileUtil, mediaUtil, audioController);
    }

    @NonNull
    @Override
    public StringData getNextAnswer() {
        return new StringData(destinationName);
    }

    @Override
    public Object createBinaryData(StringData answerData) {
        return uri;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        destinationName = RandomString.make();
    }

    @Override
    protected void prepareForSetAnswer() {
        when(formEntryPrompt.isReadOnly()).thenReturn(false);

        when(mediaUtil.getPathFromUri(any(Context.class), any(Uri.class), any(String.class)))
                .thenReturn(String.format("%s.mp3", RandomString.make()));

        when(fileUtil.getRandomFilename()).thenReturn(destinationName);

        File firstFile = mock(File.class);

        when(fileUtil.getFileAtPath(String.format("/%s.mp3", destinationName)))
                .thenReturn(firstFile);

        when(firstFile.exists()).thenReturn(true);
        when(firstFile.getName()).thenReturn(destinationName);
    }

    @Test
    public void buttonsShouldLaunchCorrectIntents() {
        stubAllRuntimePermissionsGranted(true);

        Intent intent = getIntentLaunchedByClick(R.id.capture_audio);
        assertActionEquals(MediaStore.Audio.Media.RECORD_SOUND_ACTION, intent);

        intent = getIntentLaunchedByClick(R.id.choose_sound);
        assertActionEquals(Intent.ACTION_GET_CONTENT, intent);
    }

    @Test
    public void buttonsShouldNotLaunchIntentsWhenPermissionsDenied() {
        stubAllRuntimePermissionsGranted(false);

        assertIntentNotStarted(activity, getIntentLaunchedByClick(R.id.capture_audio));
    }
}