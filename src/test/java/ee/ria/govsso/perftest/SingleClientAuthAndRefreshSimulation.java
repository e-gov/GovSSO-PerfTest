package ee.ria.govsso.perftest;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.doIfOrElse;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static java.time.Duration.ofSeconds;

public class SingleClientAuthAndRefreshSimulation extends BaseSimulation {

    @Override
    ScenarioBuilder getScenario() {
        return scenario("Single client authentication and refresh scenario")
                .feed(userIdFeeder)
                .exec(authenticateFlow("A", clientA))
                .repeat(sessionRefreshRepeatCount).on(
                        doIfOrElse(s -> sessionRefreshWithPause)
                                .then(pause(ofSeconds(sessionRefreshInterval))
                                        .exec(silentRefreshFlow("A", clientA)))
                                .orElse(exec(silentRefreshFlow("A", clientA))));
    }
}
