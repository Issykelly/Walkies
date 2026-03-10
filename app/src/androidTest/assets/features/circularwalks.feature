Feature: Circular Walks

  Scenario: User fetches and completes a walk
    Given the user location is at latitude 51.5074 and longitude -0.1278
    And the walk "Park Loop" is available at latitude 51.5080 and longitude -0.1280
    When the app fetches walks
    And the user selects the walk "Park Loop"
    And requests a route to the walk
    When the user reaches the walk destination
    And the user returns to the start point
    Then a message "Walk completed!" is shown
    And the walk is marked as completed in shared preferences

  Scenario: User sees walks fetched from the database
    Given the user location is at latitude 51.5074 and longitude -0.1278
    When the app fetches walks
    Then the app displays a list of walks sorted by distance
    And map markers are shown for each walk

  Scenario: User reaches destination but has not yet returned
    Given the user location is at latitude 51.5074 and longitude -0.1278
    And the walk "Park Loop" is available at latitude 51.5080 and longitude -0.1280
    When the app fetches walks
    And the user selects the walk "Park Loop"
    And requests a route to the walk
    When the user reaches the walk destination
    Then a message "Reached pin — returning." is shown

  Scenario: User goes off route and route is recalculated
    Given the user location is at latitude 51.5074 and longitude -0.1278
    And the walk "Park Loop" is available at latitude 51.5080 and longitude -0.1280
    When the app fetches walks
    And the user selects the walk "Park Loop"
    And requests a route to the walk
    When the user moves far off the route
    Then a message "Off-route — recalculating..." is shown

  Scenario: Route request before GPS is ready
    Given the user is on the Circular Walks Map screen
    And the walk "Park Loop" is available at latitude 51.5080 and longitude -0.1280
    When the user selects the walk "Park Loop"
    And requests a route to the walk
    Then a message "Waiting for GPS..." is shown

  Scenario: Map camera zooms to user location on first update
    Given the user location is at latitude 51.5074 and longitude -0.1278
    When the map becomes ready
    Then the camera moves to the user location
