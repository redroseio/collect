package com.redrosecps.collect.android.injection.config;

import android.app.Application;
import android.content.Context;
import android.telephony.SmsManager;
import android.webkit.MimeTypeMap;

import org.javarosa.core.reference.ReferenceManager;
import com.redrosecps.collect.android.analytics.Analytics;
import com.redrosecps.collect.android.analytics.FirebaseAnalytics;
import com.redrosecps.collect.android.application.Collect;
import com.redrosecps.collect.android.dao.FormsDao;
import com.redrosecps.collect.android.dao.InstancesDao;
import com.redrosecps.collect.android.events.RxEventBus;
import com.redrosecps.collect.android.formentry.media.AudioHelperFactory;
import com.redrosecps.collect.android.formentry.media.ScreenContextAudioHelperFactory;
import com.redrosecps.collect.android.http.CollectServerClient;
import com.redrosecps.collect.android.http.CollectThenSystemContentTypeMapper;
import com.redrosecps.collect.android.http.openrosa.OpenRosaHttpInterface;
import com.redrosecps.collect.android.http.openrosa.okhttp.OkHttpConnection;
import com.redrosecps.collect.android.http.openrosa.okhttp.OkHttpOpenRosaServerClientProvider;
import com.redrosecps.collect.android.tasks.sms.SmsSubmissionManager;
import com.redrosecps.collect.android.tasks.sms.contracts.SmsSubmissionManagerContract;
import com.redrosecps.collect.android.utilities.ActivityAvailability;
import com.redrosecps.collect.android.utilities.DownloadFormListUtils;
import com.redrosecps.collect.android.utilities.PermissionUtils;
import com.redrosecps.collect.android.utilities.WebCredentialsUtils;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

/**
 * Add dependency providers here (annotated with @Provides)
 * for objects you need to inject
 */
@Module
public class AppDependencyModule {

    @Provides
    public SmsManager provideSmsManager() {
        return SmsManager.getDefault();
    }

    @Provides
    SmsSubmissionManagerContract provideSmsSubmissionManager(Application application) {
        return new SmsSubmissionManager(application);
    }

    @Provides
    Context context(Application application) {
        return application;
    }

    @Provides
    public InstancesDao provideInstancesDao() {
        return new InstancesDao();
    }

    @Provides
    public FormsDao provideFormsDao() {
        return new FormsDao();
    }

    @Provides
    @Singleton
    RxEventBus provideRxEventBus() {
        return new RxEventBus();
    }

    @Provides
    MimeTypeMap provideMimeTypeMap() {
        return MimeTypeMap.getSingleton();
    }

    @Provides
    @Singleton
    OpenRosaHttpInterface provideHttpInterface(MimeTypeMap mimeTypeMap) {
        return new OkHttpConnection(
                new OkHttpOpenRosaServerClientProvider(new OkHttpClient()),
                new CollectThenSystemContentTypeMapper(mimeTypeMap),
                Collect.getInstance().getUserAgentString()
        );
    }

    @Provides
    CollectServerClient provideCollectServerClient(OpenRosaHttpInterface httpInterface, WebCredentialsUtils webCredentialsUtils) {
        return new CollectServerClient(httpInterface, webCredentialsUtils);
    }

    @Provides
    WebCredentialsUtils provideWebCredentials() {
        return new WebCredentialsUtils();
    }

    @Provides
    DownloadFormListUtils provideDownloadFormListUtils(
            Application application,
            CollectServerClient collectServerClient,
            WebCredentialsUtils webCredentialsUtils,
            FormsDao formsDao) {
        return new DownloadFormListUtils(
                application,
                collectServerClient,
                webCredentialsUtils,
                formsDao
        );
    }

    @Provides
    @Singleton
    public Analytics providesAnalytics(Application application) {
        com.google.firebase.analytics.FirebaseAnalytics firebaseAnalyticsInstance = com.google.firebase.analytics.FirebaseAnalytics.getInstance(application);
        FirebaseAnalytics firebaseAnalytics = new FirebaseAnalytics(firebaseAnalyticsInstance);

        return new Analytics() {
            @Override
            public void logEvent(String category, String action) {
                firebaseAnalytics.logEvent(category, action);
            }

            @Override
            public void logEvent(String category, String action, String label) {
                firebaseAnalytics.logEvent(category, action, label);
            }
        };
    }

    @Provides
    public PermissionUtils providesPermissionUtils() {
        return new PermissionUtils();
    }

    @Provides
    public ReferenceManager providesReferenceManager() {
        return ReferenceManager.instance();
    }

    @Provides
    public AudioHelperFactory providesAudioHelperFactory() {
        return new ScreenContextAudioHelperFactory();
    }

    @Provides
    public ActivityAvailability providesActivityAvailability(Context context) {
        return new ActivityAvailability(context);
    }
}
