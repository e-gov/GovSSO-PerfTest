package ee.ria.govsso.perftest;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.doIfOrElse;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static java.time.Duration.ofSeconds;

public class MultiClientSimulation extends BaseSimulation {

    @Override
    ScenarioBuilder getScenario() {
        return scenario("Multi client authentication scenario")
                .feed(userIdFeeder)
                .exec(authenticateFlow("A", clientA),
                        continueSessionFlow("B", clientB))
                .repeat(sessionRefreshRepeatCount).on(
                        doIfOrElse(s -> sessionRefreshWithPause)
                                .then(pause(ofSeconds(sessionRefreshInterval))
                                        .exec(silentRefreshFlow("A", clientA),
                                                silentRefreshFlow("B", clientB)))
                                .orElse(exec(silentRefreshFlow("A", clientA),
                                        silentRefreshFlow("B", clientB))))
                .exec(logoutWithContinueSessionFlow("A", clientA),
                        logoutFlow("B", clientB));
    }
}