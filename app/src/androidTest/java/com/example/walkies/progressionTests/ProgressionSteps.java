package com.example.walkies.progressionTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.walkies.R;
import com.example.walkies.mysteryWalks.MysteryWalksModel;
import com.example.walkies.tamagotchi.Tamagotchi;
import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import java.util.Collection;
import java.util.HashSet;

public class ProgressionSteps extends GreenCoffeeSteps {

    private int initialXP;
    private int initialCoins;

    @Given("^I am on the Tamagotchi screen$")
    public void iAmOnTheTamagotchiScreen() {
        onView(withId(R.id.main)).check(matches(isDisplayed()));

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("coins", 500)
                .putInt("xp", 0)
                .putInt("level", 1)
                .putStringSet("owned_hats", new HashSet<>())
                .putInt("selected_hat", 0)
                .apply();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            for (Activity a : activities) {
                if (a instanceof Tamagotchi) {
                    Tamagotchi t = (Tamagotchi) a;
                    t.getPresenter().loadStats();
                    t.updateUI();
                    break;
                }
            }
        });

        waitForAsync();

        initialXP = getXpFromPrefs();
        initialCoins = getCoinsFromPrefs();
    }

    private int getXpFromPrefs() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE).getInt("xp", 0);
    }

    private int getCoinsFromPrefs() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE).getInt("coins", 0);
    }

    @When("^I complete a mystery walk$")
    public void iCompleteAMysteryWalk() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MysteryWalksModel model = new MysteryWalksModel(context);
        model.setInitialDistance(1000); 
        model.setMaxHint(1); 
        model.saveCompletion();
        
        waitForAsync();
    }

    @When("^I feed the dog$")
    public void iFeedTheDog() {
        onView(withId(R.id.feed)).perform(click());
        onView(withId(R.id.sock)).perform(click());
        waitForAsync();
    }

    @Then("^my XP should increase$")
    public void myXPShouldIncrease() {
        int currentXP = getXpFromPrefs();
        assertTrue("XP should have increased after a walk. Initial: " + initialXP + ", Current: " + currentXP, 
                currentXP > initialXP);
    }

    @Then("^my XP should not increase$")
    public void myXPShouldNotIncrease() {
        int currentXP = getXpFromPrefs();
        assertEquals("XP should not increase when feeding", initialXP, currentXP);
    }

    @Then("^my coins should increase$")
    public void myCoinsShouldIncrease() {
        int currentCoins = getCoinsFromPrefs();
        assertTrue("Coins should increase after a walk. Initial: " + initialCoins + ", Current: " + currentCoins, 
                currentCoins > initialCoins);
    }

    @Then("^I might level up if I have enough XP$")
    public void iMightLevelUp() {
        // add later??
    }

    @When("^I open the hat menu$")
    public void iOpenTheHatMenu() {
        onView(withId(R.id.dress)).perform(click());
        onView(withId(R.id.hatMenu)).check(matches(isDisplayed()));
    }

    @When("^I buy a new hat$")
    public void iBuyANewHat() {
        onView(withId(R.id.brownCowboy)).perform(click());
        onView(withId(R.id.confirmButton)).perform(click());
        waitForAsync();
    }

    @Then("^my coins should decrease$")
    public void myCoinsShouldDecrease() {
        int currentCoins = getCoinsFromPrefs();
        assertTrue("Coins should decrease after buying a hat. Initial: " + initialCoins + ", Current: " + currentCoins, 
                currentCoins < initialCoins);
    }

    @Then("^the dog should be wearing the hat$")
    public void dogWearingHat() {
        onView(withId(R.id.accessories)).check(matches(isDisplayed()));
    }

    private void waitForAsync() {
        try {
            Thread.sleep(1000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
