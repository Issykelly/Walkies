Feature: Washing the Tamagotchi

  Scenario: Opening the wash menu
    Given I am on the Tamagotchi screen
    When I tap the bath button
    Then I should see the wash minigame

  Scenario: Washing the dog
    Given I am on the Tamagotchi screen
    When I tap the bath button
    Then I should see the wash minigame
    When I wash the dog
    Then the dog is clean
