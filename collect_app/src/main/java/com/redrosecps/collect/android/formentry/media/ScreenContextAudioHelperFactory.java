package com.redrosecps.collect.android.formentry.media;

import android.content.Context;

import com.redrosecps.collect.android.audio.AudioHelper;
import com.redrosecps.collect.android.utilities.ScreenContext;

public class ScreenContextAudioHelperFactory implements AudioHelperFactory {

    public AudioHelper create(Context context) {
        ScreenContext screenContext = (ScreenContext) context;
        return new AudioHelper(screenContext.getActivity(), screenContext.getViewLifecycle());
    }
}
