package com.redrosecps.collect.android.fakes;

import android.app.Activity;
import androidx.annotation.NonNull;

import com.redrosecps.collect.android.listeners.PermissionListener;
import com.redrosecps.collect.android.utilities.PermissionUtils;

/**
 * Mocked implementation of {@link PermissionUtils}.
 * The runtime permissions can be stubbed for unit testing
 *
 * @author Shobhit Agarwal
 */
public class FakePermissionUtils extends PermissionUtils {

    private boolean isPermissionGranted;

    @Override
    protected void requestPermissions(Activity activity, @NonNull PermissionListener listener, String... permissions) {
        if (isPermissionGranted) {
            listener.granted();
        } else {
            listener.denied();
        }
    }

    @Override
    protected void showAdditionalExplanation(Activity activity, int title, int message, int drawable, @NonNull PermissionListener action) {
        action.denied();
    }

    public void setPermissionGranted(boolean permissionGranted) {
        isPermissionGranted = permissionGranted;
    }
}
