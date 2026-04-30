package com.example.walkies.bathingTests;

import android.Manifest;
import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.example.walkies.tamagotchi.Tamagotchi;
import com.example.walkies.tamagotchi.TamagotchiRepository;
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
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS);

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
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TamagotchiRepository repository = new TamagotchiRepository(context);

        repository.saveUsername("testuser");
        repository.saveCity("London");
        repository.saveGoal("alone");
        repository.saveStats(100, 50, 100);
        repository.IsFirstLaunch();

        try (ActivityScenario<Tamagotchi> ignored = ActivityScenario.launch(Tamagotchi.class)) {
            start(new BathingSteps());
        }
    }
}
