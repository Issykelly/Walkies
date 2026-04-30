Feature: Feeding the Tamagotchi

  Scenario: Opening the food menu
    Given I have enough coins to buy food
    Given I am on the Tamagotchi screen
    When I tap the feed button
    Then I should see the food menu

  Scenario: Opening and closing the food menu
    Given I have enough coins to buy food
    Given I am on the Tamagotchi screen
    When I tap the feed button
    And I tap the back button
    Then I should see the main menu

  Scenario: Feeding the tamagotchi
    Given I have enough coins to buy food
    Given I am on the Tamagotchi screen
    When I tap the feed button
    Then I should see the food menu
    When I choose an item of food
    Then I feed the dog
    And the dogs hunger fills

  Scenario: Not enough coins to buy food
    Given I have no coins
    Given I am on the Tamagotchi screen
    When I tap the feed button
    And I choose an item of food
    Then I should see the cant afford dialog
