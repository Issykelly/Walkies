package com.example.walkies.progressionTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.walkies.R;
import com.example.walkies.mysteryWalks.MysteryWalksModel;
import com.example.walkies.tamagotchi.Tamagotchi;
import com.example.walkies.tamagotchi.TamagotchiRepository;
import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import java.util.Collection;
import java.util.HashSet;

public class ProgressionSteps extends GreenCoffeeSteps {

    private int initialXP;
    private int initialCoins;
    private TamagotchiRepository repository;

    @Given("^I am on the Tamagotchi screen$")
    public void iAmOnTheTamagotchiScreen() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        repository = new TamagotchiRepository(context);

        repository.clear();
        repository.saveUsername("testuser");
        repository.saveCity("London");
        repository.saveGoal("alone");
        repository.saveCoins(500, 500);
        repository.saveStats(100, 100, 100);
        repository.saveXPandLevel(0, 1, 0, 0, "");
        repository.saveOwnedHats(new HashSet<>());
        repository.saveSelectedHat(0);
        repository.IsFirstLaunch();

        refreshStats();
        
        onView(withId(R.id.main)).check(matches(isDisplayed()));

        initialXP = repository.getXP();
        initialCoins = repository.getCoins();
    }

    private void refreshStats() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            for (Activity a : activities) {
                if (a instanceof Tamagotchi) {
                    ((Tamagotchi) a).getPresenter().loadStats();
                    break;
                }
            }
        });
    }

    @When("^I complete a mystery walk$")
    public void iCompleteAMysteryWalk() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MysteryWalksModel model = new MysteryWalksModel(context);
        model.setInitialDistance(1500);
        model.setMaxHint(1); 
        model.saveCompletion();
        
        waitForAsync();
        refreshStats();
    }

    @When("^I feed the dog$")
    public void iFeedTheDog() {
        onView(withId(R.id.feed)).perform(click());
        onView(withId(R.id.sock)).perform(click());
        waitForAsync();
        refreshStats();
    }

    @Then("^my XP should increase$")
    public void myXPShouldIncrease() {
        int currentXP = repository.getXP();
        assertTrue("XP should have increased after a walk. Initial: " + initialXP + ", Current: " + currentXP, 
                currentXP > initialXP);
    }

    @Then("^my XP should not increase$")
    public void myXPShouldNotIncrease() {
        int currentXP = repository.getXP();
        assertEquals("XP should not increase when feeding", initialXP, currentXP);
    }

    @Then("^my coins should increase$")
    public void myCoinsShouldIncrease() {
        int currentCoins = repository.getCoins();
        assertTrue("Coins should increase after a walk. Initial: " + initialCoins + ", Current: " + currentCoins, 
                currentCoins > initialCoins);
    }

    @Then("^I might level up if I have enough XP$")
    public void iMightLevelUp() {
        // Logic handled by repository and presenter
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
        refreshStats();
    }

    @Then("^my coins should decrease$")
    public void myCoinsShouldDecrease() {
        int currentCoins = repository.getCoins();
        assertTrue("Coins should decrease after buying a hat. Initial: " + initialCoins + ", Current: " + currentCoins, 
                currentCoins < initialCoins);
    }

    @Then("^the dog should be wearing the hat$")
    public void dogWearingHat() {
        onView(withId(R.id.accessories)).check(matches(isDisplayed()));
    }

    protected void waitForAsync() {
        try {
            Thread.sleep(1000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
