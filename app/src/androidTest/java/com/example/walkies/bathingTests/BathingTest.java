package com.example.walkies.bathingTests;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

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
public class BathingTest extends GreenCoffeeTest {
    @Rule
    public ActivityScenarioRule<Tomagatchi> activityRule = new ActivityScenarioRule<>(Tomagatchi.class);

    public BathingTest(ScenarioConfig scenarioConfig) {
        super(scenarioConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<ScenarioConfig> scenarios() throws IOException {

        return new GreenCoffeeConfig()
                .withFeatureFromInputStream(
                        InstrumentationRegistry.getInstrumentation()
                                .getContext()
                                .getAssets()
                                .open("features/bathing.feature")
                )
                .scenarios();

    }


    @Test
    public void test() {
        start(new BathingSteps());
    }
}

