package com.example.walkies.progressionTests;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.walkies.tamagotchi.Tamagotchi;
import com.mauriciotogneri.greencoffee.GreenCoffeeConfig;
import com.mauriciotogneri.greencoffee.GreenCoffeeTest;
import com.mauriciotogneri.greencoffee.ScenarioConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;

@RunWith(Parameterized.class)
@LargeTest
public class ProgressionTest extends GreenCoffeeTest {

    public ProgressionTest(ScenarioConfig scenarioConfig) {
        super(scenarioConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<ScenarioConfig> scenarios() throws IOException {
        return new GreenCoffeeConfig()
                .withFeatureFromInputStream(
                        InstrumentationRegistry.getInstrumentation()
                                .getContext()
                                .getAssets()
                                .open("features/progression.feature")
                )
                .scenarios();
    }

    @Test
    public void test() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("WalkiesPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("username", "testuser")
                .putString("city", "London")
                .putBoolean("first_launch", false)
                .putInt("coins", 500)
                .putInt("xp", 0)
                .putInt("level", 1)
                .apply();

        try (ActivityScenario<Tamagotchi> scenario = ActivityScenario.launch(Tamagotchi.class)) {
            start(new ProgressionSteps());
        }
    }
}
