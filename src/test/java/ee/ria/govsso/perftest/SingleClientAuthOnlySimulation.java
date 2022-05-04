package ee.ria.govsso.perftest;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.scenario;

public class SingleClientAuthOnlySimulation extends BaseSimulation {

    @Override
    ScenarioBuilder getScenario() {
        return scenario("Single client authentication only scenario")
                .feed(userIdFeeder)
                .exec(authenticateFlow("A", clientA));
    }
}
