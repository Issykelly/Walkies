package com.example.walkies.hatTests;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.walkies.tamagotchi.Tamagotchi;
import com.mauriciotogneri.greencoffee.GreenCoffeeConfig;
import com.mauriciotogneri.greencoffee.GreenCoffeeTest;
import com.mauriciotogneri.greencoffee.ScenarioConfig;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;

@RunWith(Parameterized.class)
@LargeTest
public class HatTest extends GreenCoffeeTest {
    @Rule
    public ActivityScenarioRule<Tamagotchi> activityRule = new ActivityScenarioRule<>(Tamagotchi.class);

    public HatTest(ScenarioConfig scenarioConfig) {
        super(scenarioConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<ScenarioConfig> scenarios() throws IOException {

        return new GreenCoffeeConfig()
                .withFeatureFromInputStream(
                        InstrumentationRegistry.getInstrumentation()
                                .getContext()
                                .getAssets()
                                .open("features/hat.feature")
                )
                .scenarios();

    }


    @Test
    public void test() {
        start(new HatSteps());
    }
}