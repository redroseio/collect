package com.redrosecps.collect.android.support;

import androidx.test.platform.app.InstrumentationRegistry;

import com.redrosecps.collect.android.application.Collect;
import com.redrosecps.collect.android.injection.config.AppDependencyComponent;
import com.redrosecps.collect.android.injection.config.AppDependencyModule;
import com.redrosecps.collect.android.injection.config.DaggerAppDependencyComponent;
import com.redrosecps.collect.android.logic.FormController;

public final class CollectHelpers {

    private CollectHelpers() {
    }

    public static FormController waitForFormController() throws InterruptedException {
        if (Collect.getInstance().getFormController() == null) {
            do {
                Thread.sleep(1);
            } while (Collect.getInstance().getFormController() == null);
        }

        return Collect.getInstance().getFormController();
    }

    public static AppDependencyComponent getAppDependencyComponent() {
        Collect application = getApplication();
        return application.getComponent();
    }

    private static Collect getApplication() {
        return (Collect) InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext()
                .getApplicationContext();
    }

    public static void overrideAppDependencyModule(AppDependencyModule appDependencyModule) {
        Collect application = getApplication();

        AppDependencyComponent testComponent = DaggerAppDependencyComponent.builder()
                .application(application)
                .appDependencyModule(appDependencyModule)
                .build();
        application.setComponent(testComponent);
    }
}
