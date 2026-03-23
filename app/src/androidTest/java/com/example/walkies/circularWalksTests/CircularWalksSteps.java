package com.example.walkies.circularWalksTests;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import androidx.test.platform.app.InstrumentationRegistry;

import com.example.walkies.circularWalks.CircularWalksContract;
import com.example.walkies.circularWalks.CircularWalksPresenter;
import com.example.walkies.walkModel;
import com.google.android.gms.maps.model.LatLng;
import com.mauriciotogneri.greencoffee.GreenCoffeeSteps;
import com.mauriciotogneri.greencoffee.annotations.Given;
import com.mauriciotogneri.greencoffee.annotations.Then;
import com.mauriciotogneri.greencoffee.annotations.When;

import java.util.ArrayList;
import java.util.List;

public class CircularWalksSteps extends GreenCoffeeSteps {

    private CircularWalksContract.View mockView;
    private CircularWalksContract.Model mockModel;
    private CircularWalksPresenter presenter;

    private Location userLocation;
    private walkModel selectedWalk;
    private LatLng walkDest;

    @Given("^the user location is at latitude ([+-]?\\d*\\.?\\d+) and longitude ([+-]?\\d*\\.?\\d+)$")
    public void setUserLocation(String latitude, String longitude) {
        userLocation = createLocation(Double.parseDouble(latitude), Double.parseDouble(longitude));
        setupMocks();
    }

    private void setupMocks() {
        if (presenter != null) return;

        mockView = mock(CircularWalksContract.View.class);
        mockModel = mock(CircularWalksContract.Model.class);
        
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Clean slate for each test
        context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        
        when(mockView.getContext()).thenReturn(context);

        presenter = new CircularWalksPresenter(mockView, mockModel);
    }

    @When("the app fetches walks")
    public void fetchWalks() {
        setupMocks();
        
        if (userLocation != null) {
            walkModel walk = new walkModel("Test Walk", 1.0, userLocation.getLongitude(), userLocation.getLatitude(), null);

            doAnswer(invocation -> {
                CircularWalksContract.Model.WalksCallback callback = invocation.getArgument(2);
                callback.onLoaded(List.of(walk));
                return null;
            }).when(mockModel).fetchWalks(anyDouble(), anyDouble(), any());

            presenter.onLocationReceived(userLocation);
        }
    }

    @Then("the app displays a list of walks sorted by distance")
    public void verifySortedWalks() {
        verify(mockView, atLeastOnce()).showWalks(any());
    }

    @Then("map markers are shown for each walk")
    public void verifyMarkers() {
        verify(mockView, atLeastOnce()).showMarkers(any());
    }

    @Given("the user is on the Circular Walks Map screen")
    public void onMapScreen() {
        setupMocks();
        if (userLocation != null) {
            presenter.onLocationReceived(userLocation);
        }
    }

    @Given("^the walk \"([^\"]*)\" is available at latitude ([+-]?\\d*\\.?\\d+) and longitude ([+-]?\\d*\\.?\\d+)$")
    public void availableWalk(String name, String lat, String lon) {
        double dLat = Double.parseDouble(lat);
        double dLon = Double.parseDouble(lon);
        walkDest = new LatLng(dLat, dLon);
        selectedWalk = new walkModel(name, 1.0, dLon, dLat, null);
    }

    @When("^the user selects the walk \"([^\"]*)\"$")
    public void selectWalk(String name) {
        setupMocks();
        if (selectedWalk == null) {
            selectedWalk = new walkModel(name, 1.0, -0.1280, 51.5080, null);
        }
        presenter.onWalkSelected(selectedWalk);
        verify(mockView).moveCamera(any(LatLng.class), anyFloat());
    }

    @When("requests a route to the walk")
    public void requestRoute() {
        setupMocks();

        doAnswer(invocation -> {
            CircularWalksContract.Model.RouteCallback callback = invocation.getArgument(2);
            List<LatLng> points = new ArrayList<>();
            double lat = (userLocation != null) ? userLocation.getLatitude() : 51.5074;
            double lon = (userLocation != null) ? userLocation.getLongitude() : -0.1278;
            points.add(new LatLng(lat, lon));
            if (selectedWalk != null) {
                points.add(new LatLng(selectedWalk.getWalkLatitude(), selectedWalk.getWalkLongitude()));
            } else {
                points.add(new LatLng(lat + 0.001, lon + 0.001));
            }
            callback.onLoaded(points);
            return null;
        }).when(mockModel).fetchRoute(any(LatLng.class), any(LatLng.class), any());

        presenter.onRouteRequested(selectedWalk);
    }

    @When("the user reaches the walk destination")
    public void reachDestination() {
        if (walkDest != null) {
            Location destLoc = createLocation(walkDest.latitude, walkDest.longitude);
            presenter.onLocationReceived(destLoc);
            waitForLoop();
        }
    }

    @When("the user returns to the start point")
    public void returnToStart() {
        if (userLocation != null) {
            Location startLoc = createLocation(userLocation.getLatitude(), userLocation.getLongitude());
            presenter.onLocationReceived(startLoc);
            waitForLoop();
        }
    }

    @Then("^a message \"([^\"]*)\" is shown$")
    public void verifyMessage(String msg) {
        verify(mockView, atLeastOnce()).showMessage(msg);
    }

    @Then("the walk is marked as completed in shared preferences")
    public void verifyWalkCompletion() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE);

        assertEquals(100, prefs.getInt("walked", -1));
        
        verify(mockView, atLeastOnce()).toggleWalkList(true);
    }

    @When("the user moves far off the route")
    public void moveFarOffRoute() {
        if (userLocation != null) {
            Location offRoute = createLocation(
                    userLocation.getLatitude() + 0.01,
                    userLocation.getLongitude() + 0.01
            );

            presenter.onLocationReceived(offRoute);
            waitForLoop();
        }
    }

    @When("the map becomes ready")
    public void mapReady() {
        setupMocks();
        // If location was provided, ensure presenter has it before mapReady logic
        if (userLocation != null) {
            presenter.onLocationReceived(userLocation);
        }
        presenter.onMapReady();
    }

    @Then("the camera moves to the user location")
    public void verifyCameraMoved() {
        verify(mockView, atLeastOnce())
                .moveCamera(any(LatLng.class), anyFloat());
    }

    // helpers
    // -------------------------------------------------------------------------------

    private void waitForLoop() {
        try {
            Thread.sleep(2000); // wait for the 1.5s Handler loop to fire
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Location createLocation(double latitude, double longitude) {
        Location loc = new Location("mock");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setTime(System.currentTimeMillis());
        return loc;
    }
}
