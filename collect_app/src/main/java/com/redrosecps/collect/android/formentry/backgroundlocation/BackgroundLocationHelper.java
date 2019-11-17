package com.redrosecps.collect.android.formentry.backgroundlocation;

import android.location.Location;

import com.redrosecps.collect.android.application.Collect;
import com.redrosecps.collect.android.logic.AuditConfig;
import com.redrosecps.collect.android.logic.AuditEvent;
import com.redrosecps.collect.android.preferences.GeneralSharedPreferences;
import com.redrosecps.collect.android.utilities.PermissionUtils;
import com.redrosecps.collect.android.utilities.PlayServicesUtil;

import static com.redrosecps.collect.android.preferences.GeneralKeys.KEY_BACKGROUND_LOCATION;

/**
 * Wrapper on resources needed by {@link BackgroundLocationManager} to make testing easier.
 *
 * Ideally this would be replaced by more coherent abstractions in the future.
 *
 * The methods on the {@link com.redrosecps.collect.android.logic.FormController} are wrapped here rather
 * than the form controller being passed in when constructing the {@link BackgroundLocationManager}
 * because the form controller isn't set until
 * {@link com.redrosecps.collect.android.activities.FormEntryActivity}'s onCreate.
 */
public class BackgroundLocationHelper {
    boolean isAndroidLocationPermissionGranted() {
        return PermissionUtils.areLocationPermissionsGranted(Collect.getInstance().getApplicationContext());
    }

    boolean isBackgroundLocationPreferenceEnabled() {
        return GeneralSharedPreferences.getInstance().getBoolean(KEY_BACKGROUND_LOCATION, true);
    }

    boolean arePlayServicesAvailable() {
        return PlayServicesUtil.isGooglePlayServicesAvailable(Collect.getInstance().getApplicationContext());
    }

    /**
     * @return true if the global form controller has been initialized.
     */
    boolean isCurrentFormSet() {
        return Collect.getInstance().getFormController() != null;
    }

    /**
     * @return true if the current form definition requests any kind of background location.
     *
     * Precondition: the global form controller has been initialized.
     */
    boolean currentFormCollectsBackgroundLocation() {
        return Collect.getInstance().getFormController().currentFormCollectsBackgroundLocation();
    }

    /**
     * @return true if the current form definition requests a background location audit, false
     * otherwise.
     *
     * Precondition: the global form controller has been initialized.
     */
    boolean currentFormAuditsLocation() {
        return Collect.getInstance().getFormController().currentFormAuditsLocation();
    }

    /**
     * @return the configuration for the audit requested by the current form definition.
     *
     * Precondition: the global form controller has been initialized.
     */
    AuditConfig getCurrentFormAuditConfig() {
        return Collect.getInstance().getFormController().getSubmissionMetadata().auditConfig;
    }

    /**
     * Logs an audit event of the given type.
     *
     * Precondition: the global form controller has been initialized.
     */
    void logAuditEvent(AuditEvent.AuditEventType eventType) {
        Collect.getInstance().getFormController().getAuditEventLogger().logEvent(eventType, false);
    }

    /**
     * Provides the location to the global audit event logger.
     *
     * Precondition: the global form controller has been initialized.
     */
    void provideLocationToAuditLogger(Location location) {
        Collect.getInstance().getFormController().getAuditEventLogger().addLocation(location);
    }
}
