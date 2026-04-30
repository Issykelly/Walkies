package com.example.walkies.feedingTests;

import android.app.Activity;
import android.content.Context;
import android.view.View;

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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;

import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.matcher.ViewMatchers;

import java.util.Collection;

public class FeedingSteps extends GreenCoffeeSteps {
    @Given("^I have enough coins to buy food$")
    public void iHaveEnoughCoinsToBuyFood() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);
        repository.saveCoins(1000, 1000);
        refreshStats();
    }

    @Given("^I have no coins$")
    public void iHaveNoCoins() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);
        repository.saveCoins(0, 0);
        refreshStats();
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
        onView(ViewMatchers.withId(R.id.main)).check(matches(isDisplayed()));
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

    @When("^I tap the feed button$")
    public void iTapTheFeedButton() {
        onView(withId(R.id.feed)).perform(click());
    }

    @Then("^I should see the food menu$")
    public void iShouldSeeTheFoodMenu() {
        onView(withId(R.id.foodMenu)).check(matches(isDisplayed()));
    }

    @When("^I tap the back button$")
    public void iTapTheBackButton() {
        onView(withId(R.id.backButton)).perform(click());
    }

    @Then("^I should see the main menu$")
    public void iShouldSeeTheMainMenu() {
        waitFor(500);
        onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
    }
    
    @When("^I choose an item of food$")
    public void iChooseAnItemOfFood() {
        onView(withId(R.id.broccoli)).perform(click());
    }
    
    @Then("^I feed the dog$")
    public void iFeedTheDog() {
        onView(withId(R.id.draggingFood)).perform(new GeneralSwipeAction(
                Swipe.SLOW,
                GeneralLocation.CENTER,
                view -> {
                    View dog = view.getRootView().findViewById(R.id.Dog);
                    int[] location = new int[2];
                    dog.getLocationOnScreen(location);
                    return new float[]{location[0] + (dog.getWidth() / 2f), location[1] + (dog.getHeight() / 2f)};
                },
                Press.FINGER
        ));
    }

    @Then("^the dogs hunger fills$")
    public void theDogsHungerFills() {
        waitFor(500);
        onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
    }

    @Then("^I should see the cant afford dialog$")
    public void iShouldSeeTheCantAffordDialog() {
        onView(withText(R.string.cant_afford))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    protected void waitFor(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
