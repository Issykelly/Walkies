package com.example.walkies.bathingTests;

import android.view.View;
import com.example.walkies.R;
import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.matcher.ViewMatchers;

public class BathingSteps extends GreenCoffeeSteps {
    @Given("^I am on the Tamagotchi screen$")
    public void iAmOnTheTamagotchiScreen() {
        onView(ViewMatchers.withId(R.id.main)).check(matches(isDisplayed()));
    }

    @When("^I tap the bath button$")
    public void iTapTheBathButton() {
        onView(withId(R.id.bathe)).perform(click());
    }
    
    @Then("^I should see the wash minigame$")
    public void iShouldSeeTheWashMinigame() {
        onView(withId(R.id.mainMenu)).check(matches(not(isDisplayed())));
        onView(withId(R.id.draggingSponge)).check(matches(isDisplayed()));
    }

    @When("^I wash the dog$")
    public void iWashTheDog() {
        for (int i = 0; i < 10; i++) {
            try {
                onView(withId(R.id.draggingSponge)).check(matches(isDisplayed()));
                performWashSwipe(0.20f, 0.50f);
                performWashSwipe(0.60f, 0.55f);
                performWashSwipe(0.20f, 0.60f);
                performWashSwipe(0.60f, 0.65f);

            } catch (Exception | AssertionError e) {
                break;
            }
        }
    }

    private void performWashSwipe(float xPref, float yPref) {
        onView(withId(R.id.draggingSponge)).perform(new GeneralSwipeAction(
                Swipe.FAST,
                GeneralLocation.CENTER,
                view -> {
                    View dog = view.getRootView().findViewById(R.id.Dog);
                    int[] loc = new int[2];
                    dog.getLocationOnScreen(loc);
                    return new float[]{loc[0] + (dog.getWidth() * xPref), loc[1] + (dog.getHeight() * yPref)};
                },
                Press.FINGER
        ));
    }

    @Then("^the dog is clean$")
    public void theDogIsClean() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
    }
}
