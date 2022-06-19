<img src="doc/img/eu_regional_development_fund_horizontal.jpg" width="350" height="200" alt="European Union European Regional Development Fund"/>

# GOVSSO Performance Tests

Load tests for GOVSSO service (both Ory Hydra and GOVSSO-Session).

## Prerequisites

* Java 17 JDK
* Checkout [GOVSSO-Session](https://github.com/e-gov/GOVSSO-Session) and follow instructions
  in [README.md](https://github.com/e-gov/GOVSSO-Session/blob/master/README.md) to bring up Docker Compose containers
  with required services.
* If reading this in IntelliJ IDEA,
  enable [Mermaid.js support in Markdown files](https://www.jetbrains.com/go/guide/tips/mermaid-js-support-in-markdown/)
  .

## GOVSSO service high level architecture

```mermaid
flowchart LR
    USER((User))
    BROWSER[Browser]        
    TARA[TARA]   
    USER---BROWSER
    BROWSER-->CLIENT_APP    
    
    subgraph Institution        
        CLIENT_APP[Client Application]
    end
    
    BROWSER--->GOVSSO
    BROWSER--->OIDC
    
    subgraph GOVSSO service
    GOVSSO[GOVSSO SESSION]
    OIDC[GOVSSO OIDC]
    GOVSSO--->OIDC
    end
    
    BROWSER--->TARA
    GOVSSO-->TARA
    CLIENT_APP<-->GOVSSO
```

## Authentication flow

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application
    participant GOVSSO SESSION
    participant GOVSSO OIDC
    participant TARA
   
    User Agent->>Client Application:1. Log in: client/oauth2/authorization    
    activate User Agent
    activate Client Application
    Client Application-->>Client Application: No session. Start Authorize Code Flow.        
    Client Application-->>User Agent:  Redirect: govsso-oidc/oauth2/auth   
    deactivate Client Application
    
    User Agent->>GOVSSO OIDC: 2. Get: govsso-oidc/oauth2/auth 
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/login/init?login_challenge
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION: 3. Get: govsso/login/init?login_challenge   
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Login Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC    
    GOVSSO SESSION-->>GOVSSO SESSION: Check if GOVSSO session exists?
    GOVSSO SESSION-->>User Agent: Redirect: tara/oidc/authorize
    deactivate GOVSSO SESSION
    
    User Agent->>TARA: 4. Get: tara/oidc/authorize
    activate TARA
    TARA-->>TARA: Authentication in TARA.
    TARA-->>User Agent: Redirect: govsso/login/taracallback
    deactivate TARA
    
    User Agent->>GOVSSO SESSION: 5. Get: govsso/login/taracallback
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Login Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION->>TARA: TARA identity token request.
    activate TARA
    TARA-->>GOVSSO SESSION: 
    deactivate TARA
    GOVSSO SESSION->>GOVSSO OIDC: Accept login.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>User Agent: Redirect: govsso-oidc/oauth2/auth?login_verifier
    deactivate GOVSSO SESSION
    
    User Agent->>GOVSSO OIDC: 6. Get: govsso-oidc/oauth2/auth?login_verifier
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/consent/init
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION: 7. Get: govsso/consent/init
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Consent Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION->>GOVSSO OIDC: Accept consent.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>User Agent: Redirect: govsso-oidc/oauth2/auth?consent_verifier
    deactivate GOVSSO SESSION
    
    User Agent->>GOVSSO OIDC: 8. Get: govsso-oidc/oauth2/auth?consent_verifier
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: client/oauth/code
    deactivate GOVSSO OIDC
    
    User Agent->>Client Application: 9. Get: client/oauth/code
    activate Client Application
    Client Application->>GOVSSO OIDC: GOVSSO identity token request.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>Client Application: 
    deactivate GOVSSO OIDC
    Client Application-->>Client Application: Create session.
    Client Application-->>User Agent: Logged in. Redirect: client/dashboard
    deactivate Client Application
    
    User Agent->>Client Application: 10. Get: client/dashboard
    activate Client Application
    Client Application-->>User Agent: 
    deactivate Client Application
    
    deactivate User Agent
```

## Continue authentication flow

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application
    participant GOVSSO SESSION
    participant GOVSSO OIDC
    
    User Agent->>Client Application:1. Log in: client/oauth2/authorization    
    activate User Agent
    activate Client Application
    Client Application->>GOVSSO OIDC: No session. Start Authorize Code Flow
    activate GOVSSO OIDC
    GOVSSO OIDC-->>Client Application: 
    deactivate GOVSSO OIDC    
    Client Application-->>User Agent:  Redirect: govsso-oidc/oauth2/auth   
    deactivate Client Application
    
    User Agent->>GOVSSO OIDC: 2. Get: govsso-oidc/oauth2/auth 
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/login/init?login_challenge
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION: 3. Get: govsso/login/init?login_challenge
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Login Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC    
    GOVSSO SESSION-->>GOVSSO SESSION: Check if GOVSSO session exists?
    GOVSSO SESSION->>GOVSSO OIDC: Session found. Get Active Consents.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC 
    GOVSSO SESSION-->>GOVSSO SESSION: Check if valid consent exists?
    GOVSSO SESSION-->>User Agent: Consent exists. Return continue/reauthenticate view.
    deactivate GOVSSO SESSION   
    
    User Agent->>GOVSSO SESSION: 4. Post: govsso/login/continuesession
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Login Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION->>GOVSSO OIDC: Get active consents.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>GOVSSO SESSION: Check if valid consent exists?
    GOVSSO SESSION->>GOVSSO OIDC: Accept Login.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC    
    GOVSSO SESSION-->>User Agent: Redirect: govsso-oidc/oauth2/auth?login_verifier 
    deactivate GOVSSO SESSION
    
    User Agent->>GOVSSO OIDC: 5. Get: govsso-oidc/oauth2/auth?login_verifier
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/consent/init
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION: 6. Get: govsso/consent/init
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Consent Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION->>GOVSSO OIDC: Accept consent.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>User Agent: Redirect: govsso-oidc/oauth2/auth?consent_verifier
    deactivate GOVSSO SESSION
    
    User Agent->>GOVSSO OIDC: 7. Get: govsso-oidc/oauth2/auth?consent_verifier
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: client/oauth/code
    deactivate GOVSSO OIDC
    
    User Agent->>Client Application: 8. Get: client/oauth/code
    activate Client Application
    Client Application->>GOVSSO OIDC: GOVSSO identity token request.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>Client Application: 
    deactivate GOVSSO OIDC
    Client Application-->>Client Application: Create session.
    Client Application-->>User Agent: Logged in. Redirect: client/dashboard
    deactivate Client Application
    
    User Agent->>Client Application: 9. Get: client/dashboard
    activate Client Application
    Client Application-->>User Agent: 
    deactivate Client Application
    
    deactivate User Agent
```

## Refresh session flow

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application
    participant GOVSSO SESSION
    participant GOVSSO OIDC    
    
    User Agent->>Client Application:1. Start silent refresh: client/oauth2/authorization?prompt=none
    activate User Agent
    activate Client Application
    Client Application->>GOVSSO OIDC: Session found. Start Authorize Code Flow with id token hint.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>Client Application: 
    deactivate GOVSSO OIDC    
    Client Application-->>User Agent: Redirect: govsso-oidc/oauth2/auth?prompt=none&id_token_hint
    deactivate Client Application
    
    User Agent->>GOVSSO OIDC: 2. Get: govsso-oidc/oauth2/auth?prompt=none&id_token_hint
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/login/init?login_challenge
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION: 3. Get: govsso/login/init?login_challenge
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Login Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC    
    GOVSSO SESSION-->>GOVSSO SESSION: Check if Authentication Code Flow started with prompt=none?
    GOVSSO SESSION-->>GOVSSO SESSION: Check if valid GOVSSO id token?
    GOVSSO SESSION->>GOVSSO OIDC: Get Active Consents.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC 
    GOVSSO SESSION-->>GOVSSO SESSION: Check if valid TARA id token?
    GOVSSO SESSION->>GOVSSO OIDC: Accept login.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>User Agent: Redirect: govsso-oidc/oauth2/auth?login_verifier&prompt=none&id_token_hint
    deactivate GOVSSO SESSION   
    
    User Agent->>GOVSSO OIDC: 4. Get: govsso-oidc/oauth2/auth?login_verifier
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/consent/init
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION: 5. Get: govsso/consent/init
    activate GOVSSO SESSION
    activate GOVSSO OIDC
    GOVSSO SESSION->>GOVSSO OIDC: Get Consent Request Info.
    GOVSSO OIDC-->>GOVSSO SESSION: 
    GOVSSO SESSION->>GOVSSO OIDC: Accept consent.
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>User Agent: Redirect: govsso-oidc/oauth2/auth?consent_verifier
    deactivate GOVSSO SESSION
    
    User Agent->>GOVSSO OIDC: 6. Get: govsso-oidc/oauth2/auth?consent_verifier
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: client/oauth/code
    deactivate GOVSSO OIDC
    
    User Agent->>Client Application: 7. Get: client/oauth/code
    activate Client Application
    Client Application->>GOVSSO OIDC: GOVSSO identity token request.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>Client Application: 
    deactivate GOVSSO OIDC
    Client Application-->>Client Application: Extend session.
    Client Application-->>User Agent: Logged in. Redirect: client/dashboard
    deactivate Client Application
    
    User Agent->>Client Application: 8. Get: client/dashboard
    activate Client Application
    Client Application-->>User Agent: 
    deactivate Client Application
    
    deactivate User Agent
```

## Logout flow

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application
    participant GOVSSO SESSION
    participant GOVSSO OIDC    
   
    User Agent->>Client Application:1. Logout: client/oauth/logout        
    activate User Agent
    activate Client Application
    Client Application-->>Client Application: End session
    Client Application-->>User Agent: Redirect: govsso-oidc/oauth2/sessions/logout?id_token_hint
    deactivate Client Application
   
    User Agent->>GOVSSO OIDC:2. Get: govsso-oidc/oauth2/sessions/logout?id_token_hint        
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/logout/init?logout_challenge
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION:3. Get: govsso/logout/init?logout_challenge
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Logout Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    GOVSSO SESSION->>GOVSSO OIDC: Get Active Consents.
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC    
    GOVSSO SESSION-->>GOVSSO SESSION: Check if session related consents exist?
    GOVSSO SESSION->>GOVSSO OIDC: No session related consents. Accept logout.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC    
    GOVSSO SESSION-->>User Agent: Redirect: govsso-oidc/oauth2/sessions/logout?logout_verifier
    deactivate GOVSSO SESSION
    
    User Agent->>GOVSSO OIDC:4. Get: govsso-oidc/oauth2/sessions/logout?logout_verifier        
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: client/redirecturl
    deactivate GOVSSO OIDC
        
    User Agent->>Client Application:5. Get: client/redirecturl        
    activate Client Application
    Client Application-->>User Agent: 
    deactivate Client Application
    
    deactivate User Agent
```

## Logout with continue or end all sessions flow

```mermaid
sequenceDiagram
    actor User Agent
    participant Client A Application
    participant GOVSSO SESSION
    participant GOVSSO OIDC    
    participant Client B Application
   
    User Agent->>Client A Application:1. Logout: client/oauth/logout        
    activate User Agent
    activate Client A Application
    Client A Application-->>Client A Application: End session
    Client A Application-->>User Agent: Redirect: govsso-oidc/oauth2/sessions/logout?id_token_hint
    deactivate Client A Application
   
    User Agent->>GOVSSO OIDC:2. Get: govsso-oidc/oauth2/sessions/logout?id_token_hint        
    activate GOVSSO OIDC
    GOVSSO OIDC-->>User Agent: Redirect: govsso/logout/init?logout_challenge
    deactivate GOVSSO OIDC
    
    User Agent->>GOVSSO SESSION:3. Get: govsso/logout/init?logout_challenge
    activate GOVSSO SESSION
    GOVSSO SESSION->>GOVSSO OIDC: Get Logout Request Info.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    GOVSSO SESSION->>GOVSSO OIDC: Get Active Consents.
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>GOVSSO SESSION: Check if session related consents exist?
    GOVSSO SESSION->>GOVSSO OIDC: Session related consents found. Delete consent for logout requested client.
    activate GOVSSO OIDC
    GOVSSO OIDC-->>GOVSSO SESSION: 
    deactivate GOVSSO OIDC
    GOVSSO SESSION-->>User Agent: Return logout view
    deactivate GOVSSO SESSION
    
    alt continue sessions
        User Agent->>GOVSSO SESSION:4. Post: govsso/logout/continuesession
        activate GOVSSO SESSION
        Note right of GOVSSO SESSION: No additional consents are deleted
        GOVSSO SESSION->>GOVSSO OIDC: Get Logout Request Info.
        activate GOVSSO OIDC
        GOVSSO OIDC-->>GOVSSO SESSION: 
        deactivate GOVSSO OIDC
        GOVSSO SESSION-->>User Agent: Redirect: client/redirecturl    
        deactivate GOVSSO SESSION
    else end all sessions
        User Agent->>GOVSSO SESSION:4. Post: govsso/logout/endsession
        activate GOVSSO SESSION
        GOVSSO SESSION->>GOVSSO OIDC: Get Logout Request Info.
        activate GOVSSO OIDC
        GOVSSO OIDC-->>GOVSSO SESSION: 
        deactivate GOVSSO OIDC
        GOVSSO SESSION->>GOVSSO OIDC: Accept logout.
        activate GOVSSO OIDC        
        GOVSSO OIDC--)Client B Application: Back Channel logout.
        activate Client B Application
        Client B Application-->>Client B Application: End Session
        deactivate Client B Application
        GOVSSO OIDC-->>GOVSSO SESSION: 
        deactivate GOVSSO OIDC
        GOVSSO SESSION-->>User Agent: Redirect: client/redirecturl    
        deactivate GOVSSO SESSION
    end

    User Agent->>Client A Application:5. Get: client/redirecturl    
    activate Client A Application
    Client A Application-->>User Agent: 
    deactivate Client A Application
    
    deactivate User Agent
```

## Common simulation parameters

| Parameter | Mandatory | Default value | Description, example |
| :---------|:----------|:--------------|:---------------------|
| `gatling.simulationClass` | Yes | | Simulation to execute. Example `ee.ria.govsso.perftest.MultiClientSimulation` |
| `injectorProfile` | No | `RAMP_USERS` | Injector profile to execute. Any of `RAMP_USERS, STRESS_RAMP_USERS, STRESS_PEAK_USERS` |
| `clientA` | No | `https://clienta.localhost:11443` | Client A URL. |
| `clientB` | No | `https://clientb.localhost:12443` | Client B URL. |
| `maxSessionTime` | No| `43200` | Maximum session time in seconds that is allowed by GOVSSO-Session service. |
| `sessionRefreshInterval` | No | `780` | Session refresh interval in seconds. |
| `sessionRefreshWithPause` | No | `false` | Simulate session refresh flow with actual pauses between intervals. Example: If `sessionRefreshInterval=15`, `maxSessionTime=12` and `sessionRefreshWithPause=false`, then session refresh flow is performed `N=43200/780=55` times, without pauses in between. |

## Simulation scenarios

### ee.ria.govsso.perftest.SingleClientAuthOnlySimulation

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application A
    participant GOVSSO
       
    User Agent->>Client Application A: Login        
    activate User Agent
    Client Application A->>GOVSSO: 
    Note over Client Application A,GOVSSO: Authentication flow
    GOVSSO-->>Client Application A: 
    Client Application A-->>User Agent: 
 
    deactivate User Agent
```

### ee.ria.govsso.perftest.SingleClientAuthAndRefreshSimulation

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application A
    participant GOVSSO
       
    User Agent->>Client Application A: Login        
    activate User Agent
    Client Application A->>GOVSSO: 
    Note over Client Application A,GOVSSO: Authentication flow
    GOVSSO-->>Client Application A: 
    Client Application A-->>User Agent: 
 
    User Agent->>Client Application A: Refresh        
    activate User Agent
    Client Application A->>GOVSSO: 
    Note over Client Application A,GOVSSO: Refresh session flow
    GOVSSO-->>Client Application A: 
    Client Application A-->>User Agent: 
    
    deactivate User Agent
```

### ee.ria.govsso.perftest.SingleClientSimulation

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application A    
    participant GOVSSO
       
    User Agent->>Client Application A: Login        
    activate User Agent
    Client Application A->>GOVSSO: 
    Note over Client Application A,GOVSSO: Authentication flow
    GOVSSO-->>Client Application A: 
    Client Application A-->>User Agent: 
        
    loop Repeat N times, with/without pause
        User Agent-)Client Application A: Refresh
        Client Application A-)GOVSSO: 
        Note over Client Application A,GOVSSO: Refresh session flow
        GOVSSO--)Client Application A: 
        Client Application A--)User Agent: 
    end
    
    User Agent->>Client Application A: Logout        
    Client Application A->>GOVSSO: 
    Note over Client Application A,GOVSSO: Logout flow
    GOVSSO-->>Client Application A: 
    Client Application A-->>User Agent: 
      
    deactivate User Agent
```

### ee.ria.govsso.perftest.MultiClientSimulation

```mermaid
sequenceDiagram
    actor User Agent
    participant Client Application A
    participant Client Application B
    participant GOVSSO
       
    User Agent->>Client Application A: Login        
    activate User Agent
    Client Application A->>GOVSSO: 
    Note over Client Application A,GOVSSO: Authentication flow
    GOVSSO-->>Client Application A: 
    Client Application A-->>User Agent: 
    
    User Agent->>Client Application B: Login        
    
    Client Application B->>GOVSSO: 
    Note over Client Application B,GOVSSO: Continue authentication flow
    GOVSSO-->>Client Application B: 
    Client Application B-->>User Agent: 
    
    loop Repeat N times, with/without pause
        User Agent-)Client Application A: Refresh
        Client Application A-)GOVSSO: 
        Note over Client Application A,GOVSSO: Refresh session flow
        GOVSSO--)Client Application A: 
        Client Application A--)User Agent: 
        
        User Agent-)Client Application B: Refresh
        Client Application B-)GOVSSO: 
        Note over Client Application B,GOVSSO: Refresh session flow
        GOVSSO--)Client Application B: 
        Client Application B--)User Agent: 
    end
    
    User Agent->>Client Application A: Logout        
    Client Application A->>GOVSSO: 
    Note over Client Application A,GOVSSO: Logout with continue sessions flow
    GOVSSO-->>Client Application A: 
    Client Application A-->>User Agent: 
    
    User Agent->>Client Application B: Logout        
    Client Application B->>GOVSSO: 
    Note over Client Application B,GOVSSO: Logout flow
    GOVSSO-->>Client Application B: 
    Client Application B-->>User Agent: 
    
    deactivate User Agent
```

## Injector profiles

Read more about [Injection profiles](https://gatling.io/docs/gatling/reference/current/core/injection/).

This performance test contains some predefined injector profiles to execute scenario.

> TODO: Explain reasoning and add illustrative gatling response time reports.

### RAMP_USERS

Injects users distributed evenly on given duration.

## Simulation parameters

| Parameter | Mandatory | Default value | Description, example |
| :---------|:----------|:--------------|:---------------------|
| `duration` | No | `3600` | Duration of each ramping stage. |
| `rampUsers` | No | `5` | Number of users distributed evenly on given duration. |

```
injectOpen(
    rampUsers(rampUsers).during(ofSeconds(duration)))
)
```

### STRESS_RAMP_USERS

Injects users at a constant rate in stages, defined in users per second, during a given duration. Users will be injected
at randomized intervals.

## Simulation parameters

| Parameter | Mandatory | Default value | Description, example |
| :---------|:----------|:--------------|:---------------------|
| `duration` | No | `3600` | Duration of each ramping stage. |
| `startRampUsers` | No | `0` | Users at start stage. |
| `rampUsers` | No | `5` | Number of users to ramp up at each stage. |
| `maxRampUsers` | No | `30` | Max users at peak stage. |

Example:

- `duration = 120`
- `startRampUsers = 30`
- `rampUsers = 10`
- `maxRampUsers = 60`

Will generate injector profile:

```
injectOpen(
    constantUsersPerSec(30).during(ofSeconds(120)).randomized()
    constantUsersPerSec(40).during(ofSeconds(120)).randomized()
    constantUsersPerSec(50).during(ofSeconds(120)).randomized()
    constantUsersPerSec(60).during(ofSeconds(120)).randomized()
    constantUsersPerSec(50).during(ofSeconds(120)).randomized()
    constantUsersPerSec(40).during(ofSeconds(120)).randomized()
    constantUsersPerSec(30).during(ofSeconds(120)).randomized()
)
```

### STRESS_PEAK_USERS

Injects a given number of users following a smooth approximation of the heaviside step function stretched to a given
duration.

| Parameter | Mandatory | Default value | Description, example |
| :---------|:----------|:--------------|:---------------------|
| `duration` | No | `3600` | Duration of each ramping stage. |
| `peakUsers` | No | `100000` | Number of users distributed evenly on given duration. |

```
injectOpen(
    stressPeakUsers(peakUsers).during(ofSeconds(duration)))
)
```

## Running

With default parameters:

```shell
./mvnw gatling:test -Dgatling.simulationClass=ee.ria.govsso.perftest.MultiClientSimulation
```

With custom parameters:

```shell
./mvnw gatling:test -Dgatling.simulationClass=ee.ria.govsso.perftest.MultiClientSimulation -DclientA=https://clienta.localhost:8443 -DclientB=https://clientb.localhost:9443
```
