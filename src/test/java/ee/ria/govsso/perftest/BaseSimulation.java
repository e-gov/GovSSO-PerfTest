package ee.ria.govsso.perftest;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.css;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.stressPeakUsers;
import static io.gatling.javaapi.http.HttpDsl.currentLocationRegex;
import static io.gatling.javaapi.http.HttpDsl.header;
import static io.gatling.javaapi.http.HttpDsl.headerRegex;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static java.time.Duration.ofSeconds;
import static java.util.stream.IntStream.concat;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.generate;

abstract class BaseSimulation extends Simulation {

    AtomicInteger id = new AtomicInteger(0);
    Iterator<Map<String, Object>> userIdFeeder = generate((Supplier<Map<String, Object>>) () -> Map.of("user_id", id.getAndIncrement())).iterator();
    String clientA = System.getProperty("clientA", "https://clienta.localhost:11443");
    String clientB = System.getProperty("clientB", "https://clientb.localhost:12443");
    int sessionRefreshInterval = Integer.getInteger("sessionRefreshInterval", 780);
    int maxSessionTime = Integer.getInteger("maxSessionTime", 43200);
    int sessionRefreshRepeatCount = maxSessionTime / sessionRefreshInterval;
    boolean sessionRefreshWithPause = Boolean.getBoolean("sessionRefreshWithPause");
    int startRampUsers = Integer.getInteger("startRampUsers", 0);
    int rampUsers = Integer.getInteger("rampUsers", 5);
    int maxRampUsers = Integer.getInteger("maxRampUsers", 30);
    int peakUsers = Integer.getInteger("peakUsers", 1000);
    int duration = Integer.getInteger("duration", 3600);

    enum InjectorProfile {RAMP_USERS, STRESS_RAMP_USERS, STRESS_PEAK_USERS}

    InjectorProfile injectorProfile = InjectorProfile.valueOf(System.getProperty("injectorProfile", "RAMP_USERS"));
    HttpProtocolBuilder httpProtocol = http
            .disableUrlEncoding()
            .disableFollowRedirect()
            .silentResources()
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .acceptEncodingHeader("gzip, deflate")
            .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Safari/537.36");

    public BaseSimulation() {
        ScenarioBuilder scenario = getScenario();
        runSimulation(scenario);
    }

    abstract ScenarioBuilder getScenario();

    private OpenInjectionStep[] getInjectorProfile() {
        IntStream rampNrOfUsersStream = concat(
                rangeClosed(startRampUsers, maxRampUsers).filter(n -> n % rampUsers == 0),
                rangeClosed(rampUsers, maxRampUsers - startRampUsers).filter(n -> n % rampUsers == 0)
                        .map(r -> maxRampUsers - r));

        return switch (injectorProfile) {
            case RAMP_USERS -> new OpenInjectionStep[]{rampUsers(rampUsers).during(ofSeconds(duration))};
            case STRESS_RAMP_USERS -> rampNrOfUsersStream
                    .mapToObj(r -> constantUsersPerSec(r).during(duration).randomized())
                    .toList().toArray(new OpenInjectionStep[0]);
            case STRESS_PEAK_USERS -> new OpenInjectionStep[]{stressPeakUsers(peakUsers).during(ofSeconds(duration))};
        };
    }

    private void runSimulation(ScenarioBuilder scenario) {
        setUp(scenario.injectOpen(getInjectorProfile())).protocols(httpProtocol); // TODO: https://gatling.io/docs/gatling/reference/current/core/assertions/
    }

    ChainBuilder authenticateFlow(String clientId, String clientUrl) {
        return group("Authenticate %s".formatted(clientId))
                .on(exec(http("client/oauth2/authorization")
                        .get("%s/oauth2/authorization/govsso".formatted(clientUrl))
                        .silent()
                        .check(status().is(302))
                        .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/login/init?login_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(headerRegex("location", "(.*)/oidc/authorize").saveAs("tara_url"))
                                .check(headerRegex("location", "client_id=(.*)$").saveAs("client_id"))
                                .check(headerRegex("location", "govsso_login_challenge=(.*)$").saveAs("govsso_login_challenge"))
                                .check(headerRegex("location", "redirect_uri=(.*)&").saveAs("redirect_uri"))
                                .check(headerRegex("location", "state=(.*)&").saveAs("state"))
                                .check(headerRegex("location", "nonce=(.*)&").saveAs("nonce"))).exitHereIfFailed()
                        .exec(http("tara/oidc/authorize")
                                .post("#{tara_url}/back")
                                .silent()
                                .queryParam("idcode", "id-#{user_id}")
                                .queryParam("firstname", "fn-#{user_id}")
                                .queryParam("lastname", "ln-#{user_id}")
                                .queryParam("client_id", "#{client_id}")
                                .queryParam("govsso_login_challenge", "#{govsso_login_challenge}")
                                .queryParam("redirect_uri", "#{redirect_uri}")
                                .queryParam("state", "#{state}")
                                .queryParam("nonce", "#{nonce}")
                                .check(status().is(301))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/login/taracallback")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?login_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/consent/init")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?consent_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(303))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("client/oauth/code")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(headerRegex("location", ".*/dashboard"))).exitHereIfFailed()
                        .exec(http("client/dashboard")
                                .get("%s/dashboard".formatted(clientUrl))
                                .silent()
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value")
                                        .saveAs("client_%s_csrf_token".formatted(clientId)))).exitHereIfFailed());
    }

    ChainBuilder reauthenticateFlow(String clientId, String clientUrl) {
        return group("Reauthenticate %s".formatted(clientId))
                .on(exec(http("client/oauth2/authorization")
                        .get("%s/oauth2/authorization/govsso".formatted(clientUrl))
                        .silent()
                        .check(status().is(302))
                        .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/login/init?login_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value").exists().saveAs("csrf_token"))
                                .check(css("input[name='loginChallenge']", "value").exists().saveAs("login_challenge"))
                                .check(currentLocationRegex(("(.*)/login")).saveAs("govsso_url"))).exitHereIfFailed()
                        .exec(http("govsso/login/reauthenticate")
                                .post("#{govsso_url}/login/reauthenticate")
                                .formParam("_csrf", "#{csrf_token}")
                                .formParam("loginChallenge", "#{login_challenge}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/login/init?login_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("tara_redirect_uri"))
                                .check(headerRegex("location", "(.*)/oidc/authorize").saveAs("tara_url"))
                                .check(headerRegex("location", "client_id=(.*)$").saveAs("client_id"))
                                .check(headerRegex("location", "redirect_uri=(.*)&").saveAs("redirect_uri"))
                                .check(headerRegex("location", "state=(.*)&").saveAs("state"))
                                .check(headerRegex("location", "nonce=(.*)&").saveAs("nonce"))).exitHereIfFailed()
                        .exec(http("tara/back")
                                .post("#{tara_url}/back")
                                .silent()
                                .queryParam("isik", "2")
                                .queryParam("client_id", "#{client_id}")
                                .queryParam("redirect_uri", "#{redirect_uri}")
                                .queryParam("state", "#{state}")
                                .queryParam("nonce", "#{nonce}")
                                .check(status().is(301))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/login/taracallback")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?login_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/consent/init")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?consent_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(303))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("client/oauth/code")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(headerRegex("location", ".*/dashboard"))).exitHereIfFailed()
                        .exec(http("client/dashboard")
                                .get("%s/dashboard".formatted(clientUrl))
                                .silent()
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value")
                                        .saveAs("client_%s_csrf_token".formatted(clientId))))
                        .exitHereIfFailed());
    }

    ChainBuilder continueSessionFlow(String clientId, String clientUrl) {
        return group("Continue %s".formatted(clientId))
                .on(exec(http("client/oauth2/authorization")
                        .get("%s/oauth2/authorization/govsso".formatted(clientUrl))
                        .silent()
                        .check(status().is(302))
                        .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/login/init?login_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value").exists()
                                        .saveAs("csrf_token"))
                                .check(css("input[name='loginChallenge']", "value").exists()
                                        .saveAs("login_challenge"))
                                .check(currentLocationRegex(("(.*)/login")).saveAs("govsso_url"))).exitHereIfFailed()
                        .exec(http("  govsso/login/continuesession")
                                .post("#{govsso_url}/login/continuesession")
                                .formParam("_csrf", "#{csrf_token}")
                                .formParam("loginChallenge", "#{login_challenge}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("  govsso-oidc/oauth2/auth?login_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/consent/init?consent_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?consent_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(303))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("client/oauth/code")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(headerRegex("location", ".*/dashboard"))).exitHereIfFailed()
                        .exec(http("client/dashboard")
                                .get("%s/dashboard".formatted(clientUrl))
                                .silent()
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value")
                                        .saveAs("client_%s_csrf_token".formatted(clientId)))).exitHereIfFailed());
    }

    ChainBuilder silentRefreshFlow(String clientId, String clientUrl) {
        return group("Refresh %s".formatted(clientId))
                .on(exec(http("client/oauth2/authorization/govsso?prompt=none")
                        .get("%s/oauth2/authorization/govsso?prompt=none".formatted(clientUrl))
                        .silent()
                        .check(status().is(302))
                        .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?prompt=none&id_token_hint")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/login/init?login_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?login_verifier&prompt=none&id_token_hint")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/consent/init?consent_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/auth?consent_verifier&prompt=none&id_token_hint")
                                .get("#{redirect_uri}")
                                .check(status().is(303))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("client/oauth/code/govsso")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(headerRegex("location", ".*/dashboard"))).exitHereIfFailed()
                        .exec(http("client/dashboard")
                                .get("%s/dashboard".formatted(clientUrl))
                                .silent()
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value")
                                        .saveAs("client_%s_csrf_token".formatted(clientId)))).exitHereIfFailed());
    }

    ChainBuilder logoutFlow(String clientId, String clientUrl) {
        return group("Logout %s".formatted(clientId))
                .on(exec(http("client/oauth/logout")
                        .post("%s/oauth/logout".formatted(clientUrl))
                        .silent()
                        .formParam("_csrf", "#{client_%s_csrf_token}".formatted(clientId))
                        .check(status().is(302))
                        .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/sessions/logout?id_token_hint")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/logout/init?logout_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/sessions/logout?logout_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("client/redirect")
                                .get("#{redirect_uri}")
                                .check(status().is(200))).exitHereIfFailed());
    }

    ChainBuilder logoutWithContinueSessionFlow(String clientId, String clientUrl) {
        return group("Logout only %s".formatted(clientId))
                .on(exec(http("client/oauth/logout")
                        .post("%s/oauth/logout".formatted(clientUrl))
                        .silent()
                        .formParam("_csrf", "#{client_%s_csrf_token}".formatted(clientId))
                        .check(status().is(302))
                        .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/sessions/logout")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/logout/init?logout_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value").exists().saveAs("csrf_token"))
                                .check(css("input[name='logoutChallenge']", "value").exists().saveAs("logout_challenge"))
                                .check(currentLocationRegex(("(.*)/logout")).saveAs("govsso_url"))).exitHereIfFailed()
                        .exec(http("govsso/logout/continuesession")
                                .post("#{govsso_url}/logout/continuesession")
                                .formParam("_csrf", "#{csrf_token}")
                                .formParam("logoutChallenge", "#{logout_challenge}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("client/redirect")
                                .get("#{redirect_uri}")
                                .check(status().is(200))).exitHereIfFailed());
    }

    ChainBuilder logoutWithEndAllSessionsFlow(String clientId, String clientUrl) {
        return group("Reauthenticate %s".formatted(clientId))
                .on(exec(http("client/oauth/logout")
                        .post("%s/oauth/logout".formatted(clientUrl))
                        .silent()
                        .formParam("_csrf", "#{client_%s_csrf_token}".formatted(clientId))
                        .check(status().is(302))
                        .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/sessions/logout")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso/logout/init?logout_challenge")
                                .get("#{redirect_uri}")
                                .check(status().is(200))
                                .check(css("input[name='_csrf']", "value").exists().saveAs("csrf_token"))
                                .check(css("input[name='logoutChallenge']", "value").exists().saveAs("logout_challenge"))
                                .check(currentLocationRegex(("(.*)/logout")).saveAs("govsso_url"))).exitHereIfFailed()
                        .exec(http("govsso/logout/endsession")
                                .post("#{govsso_url}/logout/endsession")
                                .formParam("_csrf", "#{csrf_token}")
                                .formParam("logoutChallenge", "#{logout_challenge}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("govsso-oidc/oauth2/sessions/logout?logout_verifier")
                                .get("#{redirect_uri}")
                                .check(status().is(302))
                                .check(header("location").saveAs("redirect_uri"))).exitHereIfFailed()
                        .exec(http("client/redirect")
                                .get("#{redirect_uri}")
                                .check(status().is(200))).exitHereIfFailed());
    }
}
