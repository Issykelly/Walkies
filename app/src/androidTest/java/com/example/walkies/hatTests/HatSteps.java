package com.example.walkies.hatTests;

import com.example.walkies.R;
import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import androidx.test.espresso.matcher.ViewMatchers;

public class HatSteps extends GreenCoffeeSteps {
    @Given("^I am on the Tomagatchi screen$")
    public void iAmOnTheTomagatchiScreen() {
        onView(ViewMatchers.withId(R.id.main)).check(matches(isDisplayed()));
    }

    @When("^I tap the hat button$")
    public void iTapTheHatButton() {
        onView(withId(R.id.dress)).perform(click());
    }

    @Then("^I should see the hat menu$")
    public void iShouldSeetheHatMenu() {
        onView(withId(R.id.hatMenu)).check(matches(isDisplayed()));
    }
    
    @Given("^I am on the hat menu$")
    public void iAmOnTheHatMenu() {
        onView(withId(R.id.dress)).perform(click());
        onView(withId(R.id.hatMenu)).check(matches(isDisplayed()));
    }
    
    @When("^I tap on a hat$")
    public void iTapOnAHat() {
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
    public void theHatShouldnTBeEquipted() {
        onView(withId(R.id.accessories)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    @When("^I click the tick$")
    public void iClickTheTick() {
        onView(withId(R.id.confirmButton)).perform(click());
    }

    @Then("^The hat should be equipted$")
    public void theHatShouldBeEquipted() {
        onView(withId(R.id.hatMenu)).check(matches(not(isDisplayed())));
        onView(withId(R.id.accessories)).check(matches(isDisplayed()));
    }
}
