Feature: Feeding the Tomagatchi

  Scenario: Opening the food menu
    Given I am on the Tomagatchi screen
    When I tap the feed button
    Then I should see the food menu

  Scenario: Feeding the tomagatchi
    Given I am on the Tomagatchi screen
    When I tap the feed button
    Then I should see the food menu
    When I choose an item of food
    Then I feed the dog
    And the dogs hunger fills
