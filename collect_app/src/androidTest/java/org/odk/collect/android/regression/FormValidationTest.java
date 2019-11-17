package com.redrosecps.collect.android.regression;

import android.Manifest;

import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import com.redrosecps.collect.android.espressoutils.pages.MainMenuPage;
import com.redrosecps.collect.android.support.CopyFormRule;
import com.redrosecps.collect.android.support.ResetStateRule;

// Issue number NODK-251
@RunWith(AndroidJUnit4.class)
public class FormValidationTest extends BaseRegressionTest {

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(GrantPermissionRule.grant(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE)
            )
            .around(new ResetStateRule())
            .around(new CopyFormRule("OnePageFormShort.xml"));

    @Test
    public void invalidAnswer_ShouldDisplayAllQuestionsOnOnePage() {

        new MainMenuPage(main)
                .startBlankForm("OnePageFormShort")
                .putTextOnIndex(0, "A")
                .clickGoToIconInForm()
                .clickJumpEndButton()
                .clickSaveAndExitWhenValidationErrorIsExpected()
                .checkIsToastWithMessageDisplayed("Response length must be between 5 and 15")
                .checkIsTextDisplayed("Integer")
                .putTextOnIndex(0, "Aaaaa")
                .clickOnGoToIconInForm()
                .clickJumpEndButton()
                .clickSaveAndExit();
    }

    @Test
    public void openHierarchyView_ShouldSeeShortForms() {

        //TestCase3
        new MainMenuPage(main)
                .startBlankForm("OnePageFormShort")
                .clickGoToIconInForm()
                .checkIsTextDisplayed("YY MM")
                .checkIsTextDisplayed("YY")
                .simplePressBack()
                .closeSoftKeyboard()
                .simplePressBack()
                .clickIgnoreChanges();
    }
}