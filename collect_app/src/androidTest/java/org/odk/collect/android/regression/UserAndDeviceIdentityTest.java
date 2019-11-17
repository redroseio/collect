package com.redrosecps.collect.android.regression;

import android.Manifest;

import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.espressoutils.pages.GeneralSettingsPage;
import com.redrosecps.collect.android.espressoutils.pages.MainMenuPage;
import com.redrosecps.collect.android.espressoutils.pages.UserAndDeviceIdentitySettingsPage;
import com.redrosecps.collect.android.support.CopyFormRule;
import com.redrosecps.collect.android.support.ResetStateRule;

// Issue number NODK-238
@RunWith(AndroidJUnit4.class)
public class UserAndDeviceIdentityTest extends BaseRegressionTest {

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(GrantPermissionRule.grant(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE)
            )
            .around(new ResetStateRule())
            .around(new CopyFormRule("Test.xml"));

    @Test
    public void setEmail_validatesEmail() {
        //TestCase1
        new MainMenuPage(main)
                .clickOnMenu()
                .clickGeneralSettings()
                .clickUserAndDeviceIdentity()
                .clickFormMetadata()
                .clickMetadataEmail()
                .inputText("aabb")
                .clickOKOnDialog()
                .checkIsToastWithMessageDisplayed(R.string.invalid_email_address)
                .clickMetadataEmail().inputText("aa@bb")
                .clickOKOnDialog()
                .checkIsTextDisplayed("aa@bb");
    }

    @Test
    public void emptyUsername_ShouldNotDisplayUsernameInForm() {

        //TestCase2
        new MainMenuPage(main)
                .startBlankForm("Test")
                .swipeOnText("")
                .clickSaveAndExit();
    }

    @Test
    public void setMetadataUsername_ShouldDisplayMetadataUsernameInForm() {

        //TestCase3
        new MainMenuPage(main)
                .clickOnMenu()
                .clickGeneralSettings()
                .clickUserAndDeviceIdentity()
                .clickFormMetadata()
                .clickMetadataUsername()
                .inputText("AAA")
                .clickOKOnDialog()
                .pressBack(new UserAndDeviceIdentitySettingsPage(main))
                .pressBack(new GeneralSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .startBlankForm("Test")
                .swipeOnText("AAA")
                .clickSaveAndExit();
    }

    @Test
    public void setAggregateUsername_ShouldDisplayAggregateUsernameInForm() {
        //TestCase4
        new MainMenuPage(main)
                .clickOnMenu()
                .clickGeneralSettings()
                .clickUserAndDeviceIdentity()
                .clickFormMetadata()
                .clickMetadataUsername()
                .inputText("")
                .clickOKOnDialog()
                .pressBack(new UserAndDeviceIdentitySettingsPage(main))
                .pressBack(new GeneralSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .clickOnMenu()
                .clickGeneralSettings()
                .openServerSettings()
                .clickOnServerType()
                .clickOnString(R.string.server_platform_odk_aggregate)
                .clickAggregateUsername()
                .inputText("BBB")
                .clickOKOnDialog()
                .pressBack(new GeneralSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .startBlankForm("Test")
                .swipeOnText("BBB")
                .clickSaveAndExit();
    }

    @Test
    public void setBothUsernames_ShouldDisplayMetadataUsernameInForm() {
        //TestCase5
        new MainMenuPage(main)
                .clickOnMenu()
                .clickGeneralSettings()
                .clickUserAndDeviceIdentity()
                .clickFormMetadata()
                .clickMetadataUsername()
                .inputText("CCC")
                .clickOKOnDialog()
                .pressBack(new UserAndDeviceIdentitySettingsPage(main))
                .pressBack(new GeneralSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .clickOnMenu()
                .clickGeneralSettings()
                .openServerSettings()
                .clickOnServerType()
                .clickOnString(R.string.server_platform_odk_aggregate)
                .clickAggregateUsername()
                .inputText("DDD")
                .clickOKOnDialog()
                .pressBack(new GeneralSettingsPage(main))
                .pressBack(new MainMenuPage(main))
                .startBlankForm("Test")
                .swipeOnText("CCC")
                .clickSaveAndExit();
    }
}