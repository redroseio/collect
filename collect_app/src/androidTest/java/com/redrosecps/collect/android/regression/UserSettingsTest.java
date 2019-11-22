package com.redrosecps.collect.android.regression;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.espressoutils.pages.AdminSettingsPage;
import com.redrosecps.collect.android.espressoutils.pages.GeneralSettingsPage;
import com.redrosecps.collect.android.espressoutils.pages.MainMenuPage;
import com.redrosecps.collect.android.support.ResetStateRule;

//Issue NODK-241
@RunWith(AndroidJUnit4.class)
public class UserSettingsTest extends BaseRegressionTest {

    @Rule
    public RuleChain ruleChain = RuleChain
            .outerRule(new ResetStateRule());

    @Test
    public void typeOption_ShouldNotBeVisible() {
        //TestCase1
        new MainMenuPage(main)
                .clickOnMenu()
                .clickAdminSettings()
                .openUserSettings()
                .checkIfTextDoesNotExist("Type")
                .checkIfTextDoesNotExist("Submission transport")
                .checkIsStringDisplayed(R.string.server);
    }

    @Test
    public void uncheckedSettings_ShouldNotBeVisibleInGeneralSettings() {
        //TestCase4
        new MainMenuPage(main)
                .clickOnMenu()
                .clickAdminSettings()
                .openUserSettings()
                .uncheckAllUsetSettings()
                .pressBack(new AdminSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .clickOnMenu()
                .clickGeneralSettings()
                .checkIfTextDoesNotExist(R.string.server)
                .checkIfTextDoesNotExist(R.string.client)
                .checkIfTextDoesNotExist(R.string.maps)
                .checkIfTextDoesNotExist(R.string.form_management_preferences)
                .checkIfTextDoesNotExist(R.string.user_and_device_identity_title)
                .pressBack(new MainMenuPage(main))
                .clickOnMenu()
                .clickAdminSettings()
                .clickGeneralSettings()
                .checkIfAreaWithKeyIsDisplayed("protocol")
                .checkIfAreaWithKeyIsDisplayed("user_interface")
                .checkIfAreaWithKeyIsDisplayed("maps")
                .checkIfAreaWithKeyIsDisplayed("form_management")
                .checkIfAreaWithKeyIsDisplayed("user_and_device_identity");
    }

    @Test
    public void showGuidance_shouldBehidden() {
        //TestCase5
        new MainMenuPage(main)
                .clickOnMenu()
                .clickAdminSettings()
                .openUserSettings()
                .uncheckUserSettings("guidance_hint")
                .pressBack(new AdminSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .clickOnMenu()
                .clickGeneralSettings()
                .openFormManagement()
                .checkIfTextDoesNotExist(R.string.guidance_hint_title)
                .pressBack(new GeneralSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .clickOnMenu()
                .clickAdminSettings()
                .openUserSettings()
                .uncheckAllUsetSettings()
                .pressBack(new AdminSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .clickOnMenu()
                .clickGeneralSettings()
                .openFormManagement()
                .checkIsStringDisplayed(R.string.guidance_hint_title);
    }
}
