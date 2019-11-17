package com.redrosecps.collect.android.formentry.media;

import android.content.Context;

import com.redrosecps.collect.android.audio.AudioHelper;

public interface AudioHelperFactory {

    AudioHelper create(Context context);
}
