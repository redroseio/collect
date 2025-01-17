package com.redrosecps.collect.android.formentry;

import android.Manifest;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import com.redrosecps.collect.android.R;
import com.redrosecps.collect.android.activities.FormEntryActivity;
import com.redrosecps.collect.android.espressoutils.pages.FormEntryPage;
import com.redrosecps.collect.android.support.CopyFormRule;
import com.redrosecps.collect.android.support.ResetStateRule;
import com.redrosecps.collect.android.support.matchers.RecyclerViewMatcher;
import com.redrosecps.collect.android.test.FormLoadingUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class DeletingRepeatGroupsTest {
    private static final String TEST_FORM = "repeat_groups.xml";

    @Rule
    public IntentsTestRule<FormEntryActivity> activityTestRule = FormLoadingUtils.getFormActivityTestRuleFor(TEST_FORM);

    @Rule
    public RuleChain copyFormChain = RuleChain
            .outerRule(GrantPermissionRule.grant(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
            )
            .around(new ResetStateRule())
            .around(new CopyFormRule(TEST_FORM, null));

    @Test
    public void requestingDeletionOfFirstRepeat_deletesFirstRepeat() {
        new FormEntryPage("repeatGroups", activityTestRule)
                .deleteGroup("text1")
                .checkIsTextDisplayed("2");
    }

    @Test
    public void requestingDeletionOfMiddleRepeat_deletesMiddleRepeat() {
        new FormEntryPage("repeatGroups", activityTestRule)
                .swipeToNextQuestion()
                .deleteGroup("text1")
                .checkIsTextDisplayed("3");
    }

    @Test
    public void requestingDeletionOfLastRepeat_deletesLastRepeat() {
        new FormEntryPage("repeatGroups", activityTestRule)
                .swipeToNextQuestion()
                .swipeToNextQuestion()
                .swipeToNextQuestion()
                .deleteGroup("text1")
                .checkIsTextDisplayed("number1");
    }

    @Test
    public void requestingDeletionOfFirstRepeatInHierarchy_deletesFirstRepeat() {
        FormEntryPage page = new FormEntryPage("repeatGroups", activityTestRule)
                .clickOnGoToIconInForm()
                .clickGoUpIcon();

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(4)));

        page.clickOnText("repeatGroup > 1")
                .checkIsTextDisplayed("1")
                .deleteGroup();

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(3)));

        page.clickOnText("repeatGroup > 1")
                .checkIsTextDisplayed("2");
    }

    @Test
    public void requestingDeletionOfMiddleRepeatInHierarchy_deletesMiddleRepeat() {
        FormEntryPage page = new FormEntryPage("repeatGroups", activityTestRule)
                .clickOnGoToIconInForm()
                .clickGoUpIcon();

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(4)));

        page.clickOnText("repeatGroup > 2")
                .checkIsTextDisplayed("2")
                .deleteGroup();

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(3)));

        page.clickOnText("repeatGroup > 2")
                .checkIsTextDisplayed("3");
    }

    @Test
    public void requestingDeletionOfLastRepeatInHierarchy_deletesLastRepeat() {
        FormEntryPage page = new FormEntryPage("repeatGroups", activityTestRule)
                .clickOnGoToIconInForm()
                .clickGoUpIcon();

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(4)));

        page.clickOnText("repeatGroup > 4")
                .checkIsTextDisplayed("4")
                .deleteGroup();

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(3)));

        page.clickOnText("repeatGroup > 3")
                .checkIsTextDisplayed("3");
    }

    @Test
    public void requestingDeletionOfFirstRepeatWithFieldList_deletesFirstRepeat() {
        new FormEntryPage("repeatGroups", activityTestRule)
                .clickGoToIconInForm()
                .clickGoUpIcon()
                .clickGoUpIcon()
                .clickOnText("repeatGroupFieldList")
                .clickOnText("repeatGroupFieldList > 1")
                .clickOnText("number1")
                .deleteGroup("number1")
                .checkIsTextDisplayed("2");
    }

    @Test
    public void requestingDeletionOfMiddleRepeatWithFieldList_deletesMiddleRepeat() {
        new FormEntryPage("repeatGroups", activityTestRule)
                .clickGoToIconInForm()
                .clickGoUpIcon()
                .clickGoUpIcon()
                .clickOnText("repeatGroupFieldList")
                .clickOnText("repeatGroupFieldList > 2")
                .clickOnText("number1")
                .deleteGroup("number1")
                .checkIsTextDisplayed("3");
    }

    @Test
    public void requestingDeletionOfLastRepeatWithFieldList_deletesLastRepeat() {
        new FormEntryPage("repeatGroups", activityTestRule)
                .clickGoToIconInForm()
                .clickGoUpIcon()
                .clickGoUpIcon()
                .clickOnText("repeatGroupFieldList")
                .clickOnText("repeatGroupFieldList > 4")
                .clickOnText("number1")
                .deleteGroup("number1")
                .checkIsStringDisplayed(R.string.quit_entry);
    }

    @Test
    public void requestingDeletionOfFirstRepeatWithFieldListInHierarchy_deletesFirstRepeat() {
        FormEntryPage page = new FormEntryPage("repeatGroups", activityTestRule)
                .clickGoToIconInForm()
                .clickGoUpIcon()
                .clickGoUpIcon()
                .clickOnText("repeatGroupFieldList");

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(4)));

        page.clickOnText("repeatGroupFieldList > 1")
                .deleteGroup();

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(3)));

        page.clickOnText("repeatGroupFieldList > 1")
                .checkIsTextDisplayed("2");
    }

    @Test
    public void requestingDeletionOfMiddleRepeatWithFieldListInHierarchy_deletesMiddleRepeat() {
        FormEntryPage page = new FormEntryPage("repeatGroups", activityTestRule)
                .clickGoToIconInForm()
                .clickGoUpIcon()
                .clickGoUpIcon()
                .clickOnText("repeatGroupFieldList");

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(4)));

        page.clickOnText("repeatGroupFieldList > 2")
                .deleteGroup();
        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(3)));

        page.clickOnText("repeatGroupFieldList > 2")
                .checkIsTextDisplayed("3");
    }

    @Test
    public void requestingDeletionOfLastRepeatWithFieldListInHierarchy_deletesLastRepeat() {
        FormEntryPage page = new FormEntryPage("repeatGroups", activityTestRule)
                .clickGoToIconInForm()
                .clickGoUpIcon()
                .clickGoUpIcon()
                .clickOnText("repeatGroupFieldList");

        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(4)));

        page.clickOnText("repeatGroupFieldList > 4")
                .deleteGroup();
        onView(withId(R.id.list)).check(matches(RecyclerViewMatcher.withListSize(3)));

        page.clickOnText("repeatGroupFieldList > 3")
                .checkIsTextDisplayed("3");
    }
}
