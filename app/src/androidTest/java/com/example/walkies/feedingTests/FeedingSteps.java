package com.example.walkies.feedingTests;

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

import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.GeneralSwipeAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Swipe;
import androidx.test.espresso.matcher.ViewMatchers;

public class FeedingSteps extends GreenCoffeeSteps {
    @Given("^I am on the Tomagatchi screen$")
    public void iAmOnTheTomagatchiScreen() {
        onView(ViewMatchers.withId(R.id.main)).check(matches(isDisplayed()));
    }

    @When("^I tap the feed button$")
    public void iTapTheFeedButton() {
        onView(withId(R.id.feed)).perform(click());
    }

    @Then("^I should see the food menu$")
    public void iShouldSeeTheFoodMenu() {
        onView(withId(R.id.foodMenu)).check(matches(isDisplayed()));
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
        onView(withId(R.id.mainMenu)).check(matches(isDisplayed()));
    }
}
