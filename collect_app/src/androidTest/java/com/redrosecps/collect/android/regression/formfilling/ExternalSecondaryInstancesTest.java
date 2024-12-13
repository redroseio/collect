package com.redrosecps.collect.android.regression.formfilling;

import android.Manifest;

import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import com.redrosecps.collect.android.espressoutils.pages.MainMenuPage;
import com.redrosecps.collect.android.regression.BaseRegressionTest;
import com.redrosecps.collect.android.support.CopyFormRule;
import com.redrosecps.collect.android.support.ResetStateRule;

import java.util.Collections;

// Issue number NODK-377
@RunWith(AndroidJUnit4.class)
public class ExternalSecondaryInstancesTest extends BaseRegressionTest {

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(GrantPermissionRule.grant(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
            .around(new ResetStateRule())
            .around(new CopyFormRule("internal_select_10.xml"))
            .around(new CopyFormRule("external_select_10.xml", Collections.singletonList("external_data_10.xml")));

    @Test
    public void external_ShouldFillTheForm() {

        //TestCase1
        new MainMenuPage(main)
                .startBlankForm("external select 10")
                .clickOnText("b")
                .swipeToNextQuestion()
                .clickOnText("ba")
                .swipeToNextQuestion()
                .clickSaveAndExit();
    }

    @Test
    public void internal_ShouldFillTheForm() {

        //TestCase2
        new MainMenuPage(main)
                .startBlankForm("internal select 10")
                .clickOnText("c")
                .swipeToNextQuestion()
                .clickOnText("ca")
                .swipeToNextQuestion()
                .clickSaveAndExit();
    }
}