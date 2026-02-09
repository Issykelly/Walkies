Feature: Washing the Tomagatchi

  Scenario: Opening the wash menu
    Given I am on the Tomagatchi screen
    When I tap the bath button
    Then I should see the wash minigame

  Scenario: Washing the dog
    Given I am on the Tomagatchi screen
    When I tap the bath button
    Then I should see the wash minigame
    When I wash the dog
    Then the dog is clean
