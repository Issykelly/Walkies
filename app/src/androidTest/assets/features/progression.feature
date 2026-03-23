Feature: Coins, XP and Levels
  As a user
  I want to earn coins and XP for completing walks
  So that I can level up and buy items

  Background:
    Given I am on the Tamagotchi screen

  Scenario: Earning coins and XP from walks
    When I complete a mystery walk
    Then my XP should increase
    And my coins should increase
    And I might level up if I have enough XP

  Scenario: Spending coins on items
    When I open the hat menu
    And I buy a new hat
    Then my coins should decrease
    And the dog should be wearing the hat
