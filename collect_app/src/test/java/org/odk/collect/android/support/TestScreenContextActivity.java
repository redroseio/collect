package com.redrosecps.collect.android.support;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

import com.redrosecps.collect.android.utilities.ScreenContext;

public class TestScreenContextActivity extends FragmentActivity implements ScreenContext {

    @Override
    public FragmentActivity getActivity() {
        return this;
    }

    @Override
    public LifecycleOwner getViewLifecycle() {
        return this;
    }
}
