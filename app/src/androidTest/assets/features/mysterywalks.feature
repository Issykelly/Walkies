Feature: Mystery Walks

  Scenario: User sees available mystery walks
    Given the user is on the Mystery Walks screen
    When the app fetches mystery walks
    Then the app displays a list of mystery walks
    And each walk shows its name and approximate distance

  Scenario: User selects a mystery walk and starts tracking
    Given the user is on the Mystery Walks screen
    And the mystery walk "Secret Path" is available
    When the user selects the walk "Secret Path"
    Then the app starts tracking the user's location
    And the first hint for "Secret Path" is displayed
    And the current distance to the destination is shown

  Scenario: User navigates through hints
    Given a mystery walk is in progress
    And hint "1/3" is currently displayed
    When the user clicks the "next" button
    Then hint "2/3" is displayed
    When the user clicks the "prev" button
    Then hint "1/3" is displayed

  Scenario: User gives up and views the location on map
    Given a mystery walk is in progress for "Secret Path"
    When the user clicks the "Give up?" button
    Then the app opens the map showing the location of "Secret Path"

  Scenario: User completes the mystery walk by reaching the destination
    Given a mystery walk is in progress
    When the user location moves within 30 meters of the destination
    Then the walk is marked as completed
    And the user is navigated back to the Tamagotchi screen

  Scenario: Location permission is requested if not granted
    Given the user is on the Mystery Walks screen
    And location permission is not granted
    When the screen is initialized
    Then the app requests location permission
