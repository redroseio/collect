package com.redrosecps.collect.android.regression;

import android.Manifest;

import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.espressoutils.pages.MainMenuPage;
import com.redrosecps.collect.android.support.CopyFormRule;
import com.redrosecps.collect.android.support.ResetStateRule;
import com.redrosecps.collect.android.support.ScreenshotOnFailureTestRule;

// Issue number NODK-209
@RunWith(AndroidJUnit4.class)
public class DrawWidgetTest extends BaseRegressionTest {

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(GrantPermissionRule.grant(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE)
            )
            .around(new ResetStateRule())
            .around(new CopyFormRule("All_widgets.xml"));

    @Rule
    public TestRule screenshotFailRule = new ScreenshotOnFailureTestRule();

    @Test
    public void saveIgnoreDialog_ShouldUseBothOptions() {

        //TestCase1
        new MainMenuPage(main)
                .startBlankForm("All widgets")
                .clickGoToIconInForm()
                .clickOnText("Image widgets")
                .clickOnText("Draw widget")
                .clickOnId(R.id.simple_button)
                .waitForRotationToEnd()
                .simplePressBack()
                .checkIsStringDisplayed(R.string.keep_changes)
                .clickOnString(R.string.do_not_save)
                .waitForRotationToEnd()
                .clickOnId(R.id.simple_button)
                .waitForRotationToEnd()
                .simplePressBack()
                .clickOnString(R.string.keep_changes)
                .waitForRotationToEnd()
                .clickGoToIconInForm()
                .clickJumpEndButton()
                .clickSaveAndExit();
    }

    @Test
    public void setColor_ShouldSeeColorPicker() {

        //TestCase2
        new MainMenuPage(main)
                .startBlankForm("All widgets")
                .clickGoToIconInForm()
                .clickOnText("Image widgets")
                .clickOnText("Draw widget")
                .clickOnId(R.id.simple_button)
                .waitForRotationToEnd()
                .clickOnId(R.id.fab_actions)
                .clickOnId(R.id.fab_set_color)
                .clickOnString(R.string.ok)
                .simplePressBack()
                .clickOnString(R.string.keep_changes)
                .waitForRotationToEnd()
                .clickGoToIconInForm()
                .clickJumpEndButton()
                .clickSaveAndExit();
    }

    @Test
    public void multiClickOnPlus_ShouldDisplayIcons() {

        //TestCase3
        new MainMenuPage(main)
                .startBlankForm("All widgets")
                .clickGoToIconInForm()
                .clickOnText("Image widgets")
                .clickOnText("Draw widget")
                .clickOnId(R.id.simple_button)
                .waitForRotationToEnd()
                .clickOnId(R.id.fab_actions)
                .checkIsStringDisplayed(R.string.set_color)
                .checkIsIdDisplayed(R.id.fab_clear)
                .clickOnId(R.id.fab_actions)
                .checkIsStringDisplayed(R.string.set_color)
                .checkIsIdDisplayed(R.id.fab_save_and_close)
                .clickOnId(R.id.fab_actions)
                .checkIsStringDisplayed(R.string.set_color)
                .checkIsStringDisplayed(R.string.set_color)
                .simplePressBack()
                .clickOnString(R.string.keep_changes)
                .waitForRotationToEnd()
                .clickGoToIconInForm()
                .clickJumpEndButton()
                .clickSaveAndExit();
    }
}