package com.example.walkies.hatTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.content.Context;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.walkies.R;
import com.example.walkies.tamagotchi.Tamagotchi;
import com.example.walkies.tamagotchi.TamagotchiRepository;
import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import java.util.Collection;
import java.util.HashSet;

public class HatSteps extends GreenCoffeeSteps {
    @Given("^I have enough coins to buy a hat$")
    public void iHaveEnoughCoinsToBuyAHat() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);
        repository.saveCoins(500, 500);
        refreshStats();
    }

    @Given("^I have no coins for a hat$")
    public void iHaveNoCoinsForAHat() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);
        repository.saveCoins(0, 0);
        refreshStats();
    }

    @Given("^I am on the Tamagotchi screen$")
    public void iAmOnTheTamagotchiScreen() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);
        repository.saveUsername("testuser");
        repository.saveCity("London");
        repository.saveGoal("alone");
        repository.IsFirstLaunch();

        refreshStats();
        handleOnboarding();
        onView(withId(R.id.main)).check(matches(isDisplayed()));
    }

    private void refreshStats() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumedActivities =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            for (Activity activity : resumedActivities) {
                if (activity instanceof Tamagotchi) {
                    ((Tamagotchi) activity).getPresenter().loadStats();
                }
            }
        });
    }

    private void handleOnboarding() {
        try {
            onView(withId(R.id.btnWelcomeContinue)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWelcomeContinue)).perform(click());

            onView(withId(R.id.etUsername)).perform(typeText("testuser"));
            onView(withId(R.id.btnProfileContinue)).perform(click());

            onView(withId(R.id.rbLeaveHouse)).perform(click());
            onView(withId(R.id.btnGoalFinish)).perform(click());
        } catch (NoMatchingViewException e) {
        }
    }

    @Given("^I do not own any hats$")
    public void iDoNotOwnAnyHats() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);
        repository.saveOwnedHats(new HashSet<>());
        refreshStats();
    }

    @When("^I tap the hat button$")
    public void iTapTheHatButton() {
        onView(withId(R.id.dress)).perform(click());
    }

    @When("^I tap the dress button$")
    public void iTapTheDressButton() {
        onView(withId(R.id.dress)).perform(click());
    }

    @Then("^I should see the hat menu$")
    public void iShouldSeetheHatMenu() {
        onView(withId(R.id.hatMenu)).check(matches(isDisplayed()));
    }

    @Given("^I am on the hat menu$")
    public void iAmOnTheHatMenu() {
        iAmOnTheTamagotchiScreen();
        iTapTheHatButton();
        iShouldSeetheHatMenu();
    }

    @When("^I tap on a hat$")
    public void iTapOnAHat() {
        onView(withId(R.id.brownCowboy)).perform(click());
    }

    @When("^I choose a hat$")
    public void iChooseAHat() {
        onView(withId(R.id.brownCowboy)).perform(click());
    }

    @Then("^I should see the hat on$")
    public void iShouldSeeTheHatOn() {
        onView(withId(R.id.accessories)).check(matches(isDisplayed()));
    }

    @When("^I click back$")
    public void iClickBack() {
        onView(withId(R.id.backButton)).perform(click());
    }

    @Then("^The hat shouldn't be equipted$")
    public void theHatShouldNotBeEquipted() {
        onView(withId(R.id.accessories)).check(matches(not(isDisplayed())));
    }

    @Given("^No hat is equipted$")
    public void noHatIsEquipted() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);
        repository.saveSelectedHat(0);
        refreshStats();
    }

    @When("^I click the tick$")
    public void iClickTheTick() {
        onView(withId(R.id.confirmButton)).perform(click());
    }

    @When("^I confirm the purchase$")
    public void iConfirmThePurchase() {
        onView(withId(R.id.confirmButton)).perform(click());
    }

    @Then("^The hat should be equipted$")
    public void theHatShouldBeEquipted() {
        onView(withId(R.id.accessories)).check(matches(isDisplayed()));
    }

    @Then("^the dog is wearing the hat$")
    public void theDogIsWearingTheHat() {
        onView(withId(R.id.accessories)).check(matches(isDisplayed()));
    }

    @Then("^I should see the cant afford dialog for hat$")
    public void iShouldSeeTheCantAffordDialogForHat() {
        onView(withText(R.string.cant_afford))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }
}
