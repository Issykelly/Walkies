Feature: Dressing the Tamagotchi

  Scenario: Opening the hat menu
    Given I am on the Tamagotchi screen
    When I tap the hat button
    Then I should see the hat menu

  Scenario: a hat is testing but not brought
    Given I am on the hat menu
    When I tap on a hat
    Then I should see the hat on
    When I click back
    Then The hat shouldn't be equipted

  Scenario: a hat is tested and then brought
    Given I am on the hat menu
    When I tap on a hat
    Then I should see the hat on
    When I click the tick
    Then The hat should be equipted