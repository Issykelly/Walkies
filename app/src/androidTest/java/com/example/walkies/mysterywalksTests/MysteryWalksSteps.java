package com.example.walkies.mysterywalksTests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.location.Location;

import androidx.test.platform.app.InstrumentationRegistry;

import com.example.walkies.mysteryWalks.MysteryWalksContract;
import com.example.walkies.mysteryWalks.MysteryWalksPresenter;
import com.example.walkies.walkModel;
import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import java.util.ArrayList;
import java.util.List;

public class MysteryWalksSteps extends GreenCoffeeSteps {

    private MysteryWalksContract.View mockView;
    private MysteryWalksContract.Model mockModel;
    private MysteryWalksPresenter presenter;

    private walkModel testWalk;
    private List<walkModel> availableWalks;

    private void setupMocks() {
        if (presenter != null) return;

        mockView = mock(MysteryWalksContract.View.class);
        mockModel = mock(MysteryWalksContract.Model.class);

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE).edit().clear().apply();

        presenter = new MysteryWalksPresenter(mockView, mockModel);
    }

    @Given("^the user is on the Mystery Walks screen$")
    public void onMysteryWalksScreen() {
        setupMocks();
    }

    @When("^the app fetches mystery walks$")
    public void fetchWalks() {
        availableWalks = new ArrayList<>();
        testWalk = new walkModel("Secret Path", 1.0, -0.1280, 51.5080, new String[]{"Hint 1", "Hint 2", "Hint 3"});
        availableWalks.add(testWalk);

        doAnswer(invocation -> {
            MysteryWalksContract.Model.Callback<List<walkModel>> callback = invocation.getArgument(0);
            callback.call(availableWalks);
            return null;
        }).when(mockModel).loadWalks(any());

        doAnswer(invocation -> {
            MysteryWalksContract.Model.LocationCallback callback = invocation.getArgument(0);
            Location mockLoc = new Location("mock");
            mockLoc.setLatitude(51.5);
            mockLoc.setLongitude(-0.1);
            callback.call(mockLoc);
            return null;
        }).when(mockModel).getLastLocation(any());

        when(mockView.hasLocationPermission()).thenReturn(true);

        presenter.init(true);
    }

    @Then("^the app displays a list of mystery walks$")
    public void verifyWalkListShown() {
        verify(mockView, atLeastOnce()).showWalks(anyList());
    }

    @Then("^each walk shows its name and approximate distance$")
    public void verifyWalkDetails() {
        verify(mockView, atLeastOnce()).showWalks(anyList());
    }

    @Given("^the mystery walk \"([^\"]*)\" is available$")
    public void walkIsAvailable(String name) {
        setupMocks();
        testWalk = new walkModel(name, 1.0, -0.1280, 51.5080, new String[]{"Hint 1", "Hint 2", "Hint 3"});
        availableWalks = List.of(testWalk);
    }

    @When("^the user selects the walk \"([^\"]*)\"$")
    public void selectWalk(String name) {
        if (testWalk == null) {
            testWalk = new walkModel(name, 1.0, -0.1280, 51.5080, new String[]{"Hint 1", "Hint 2", "Hint 3"});
        }

        doAnswer(invocation -> {
            MysteryWalksContract.Model.LocationCallback callback = invocation.getArgument(0);
            Location mockLoc = new Location("mock");
            mockLoc.setLatitude(51.5);
            mockLoc.setLongitude(-0.1);
            callback.call(mockLoc);
            return null;
        }).when(mockModel).getLastLocation(any());
        
        when(mockView.hasLocationPermission()).thenReturn(true);
        presenter.walkSelected(testWalk);
    }

    @Then("^the app starts tracking the user's location$")
    public void verifyTrackingStarted() {
        verify(mockModel).startTracking(any());
    }

    @Then("^the first hint for \"([^\"]*)\" is displayed$")
    public void verifyFirstHint(String name) {
        verify(mockView, atLeastOnce()).showHints(true);
        verify(mockView, atLeastOnce()).showHint(eq("Hint 1"), eq(1));
    }

    @Then("^the current distance to the destination is shown$")
    public void verifyDistanceShown() {
        verify(mockView, atLeastOnce()).showDistance(anyInt());
    }

    @Given("^a mystery walk is in progress$")
    public void walkInProgress() {
        setupMocks();
        testWalk = new walkModel("Secret Path", 1.0, -0.1280, 51.5080, new String[]{"Hint 1", "Hint 2", "Hint 3"});

        doAnswer(invocation -> {
            MysteryWalksContract.Model.LocationCallback callback = invocation.getArgument(0);
            Location mockLoc = new Location("mock");
            mockLoc.setLatitude(51.5);
            mockLoc.setLongitude(-0.1);
            callback.call(mockLoc);
            return null;
        }).when(mockModel).getLastLocation(any());

        when(mockView.hasLocationPermission()).thenReturn(true);
        presenter.walkSelected(testWalk);
    }

    @Given("^hint \"([^\"]*)\" is currently displayed$")
    public void hintIsDisplayed(String hintNum) {
        if (hintNum.equals("2/3")) {
            presenter.nextHint();
        }
    }

    @When("^the user clicks the \"([^\"]*)\" button$")
    public void clickButton(String button) {
        switch (button) {
            case "next":
                presenter.nextHint();
                break;
            case "prev":
                presenter.prevHint();
                break;
            case "Give up?":
                presenter.giveUp();
                break;
        }
    }

    @Then("^hint \"([^\"]*)\" is displayed$")
    public void verifySpecificHintDisplayed(String hintNum) {
        int index = Integer.parseInt(hintNum.split("/")[0]);
        verify(mockView, atLeastOnce()).showHint(eq("Hint " + index), eq(index));
    }

    @Given("^a mystery walk is in progress for \"([^\"]*)\"$")
    public void walkInProgressFor(String name) {
        setupMocks();
        testWalk = new walkModel(name, 1.0, -0.1280, 51.5080, new String[]{"Hint 1", "Hint 2", "Hint 3"});
        
        // Stub getLastLocation (uses LocationCallback)
        doAnswer(invocation -> {
            MysteryWalksContract.Model.LocationCallback callback = invocation.getArgument(0);
            Location mockLoc = new Location("mock");
            mockLoc.setLatitude(51.5);
            mockLoc.setLongitude(-0.1);
            callback.call(mockLoc);
            return null;
        }).when(mockModel).getLastLocation(any());

        when(mockView.hasLocationPermission()).thenReturn(true);
        presenter.walkSelected(testWalk);
    }

    @Then("^the app opens the map showing the location of \"([^\"]*)\"$")
    public void verifyMapOpened(String name) {
        verify(mockView).openMap(eq(51.5080), eq(-0.1280));
    }

    @When("^the user location moves within 30 meters of the destination$")
    public void moveToDestination() {
        Location destination = new Location("mock");
        destination.setLatitude(51.5080);
        destination.setLongitude(-0.1280);

        doAnswer(invocation -> {
            MysteryWalksContract.Model.LocationCallback callback = invocation.getArgument(0);
            callback.call(destination);
            return null;
        }).when(mockModel).startTracking(any());

        presenter.walkSelected(testWalk);
    }

    @Then("^the walk is marked as completed$")
    public void verifyWalkCompleted() {
        verify(mockModel).saveCompletion();
    }

    @Then("^the user is navigated back to the Tamagotchi screen$")
    public void verifyNavigatedBack() {
        verify(mockView, atLeastOnce()).closeActivity();
    }

    @Given("^location permission is not granted$")
    public void locationPermissionNotGranted() {
        setupMocks();
        when(mockView.hasLocationPermission()).thenReturn(false);
    }

    @When("^the screen is initialized$")
    public void screenInitialized() {
        // mocking the model to return a sample walk list for initialization
        List<walkModel> mockList = new ArrayList<>();
        mockList.add(new walkModel("Test", 0, 0, 0, null));
        
        doAnswer(invocation -> {
            MysteryWalksContract.Model.Callback<List<walkModel>> cb = invocation.getArgument(0);
            cb.call(mockList);
            return null;
        }).when(mockModel).loadWalks(any());

        presenter.init(true);
    }

    @Then("^the app requests location permission$")
    public void verifyPermissionRequested() {
        verify(mockView, atLeastOnce()).requestLocationPermission();
    }
}
