# FantasyZone AI Assistant: Integration Guide

This folder contains **only new files**. Nothing in your existing project is replaced. You copy the files in, add one
block of lines to `application.properties`, set one environment variable, and restart the backend.

**What it adds:** `POST /api/assistant/ask`, a logged-in-users-only endpoint where someone can ask things like
"Who should I start?", "Compare these two players", or "Summarize my team's strengths and weaknesses". FantasyZone
gathers the relevant data, sends it with the question to an LLM, and returns the answer. **FantasyZone still runs the
league.** The assistant can only read and explain; it has no way to change rosters, scores, standings, or matchups.

---

## 1. Files to copy

Paths are relative to your project. If your Spring Boot project is the repo root (no `backend/` folder), drop the
`backend/` prefix. Every file belongs in the package shown, which already matches your existing packages.

| File in this `LLM` folder | Copy to (in your project) | Package declaration |
|---|---|---|
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/config/LlmConfig.java` | `backend/src/main/java/com/fantasyzone/config/` | `package com.fantasyzone.config;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/controller/AssistantController.java` | `.../com/fantasyzone/controller/` | `package com.fantasyzone.controller;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/dto/AssistantRequest.java` | `.../com/fantasyzone/dto/` | `package com.fantasyzone.dto;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/dto/AssistantResponse.java` | `.../com/fantasyzone/dto/` | `package com.fantasyzone.dto;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/exception/LlmException.java` | `.../com/fantasyzone/exception/` | `package com.fantasyzone.exception;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/exception/LlmExceptionHandler.java` | `.../com/fantasyzone/exception/` | `package com.fantasyzone.exception;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/service/LlmClient.java` | `.../com/fantasyzone/service/` | `package com.fantasyzone.service;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/service/AnthropicLlmClient.java` | `.../com/fantasyzone/service/` | `package com.fantasyzone.service;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/service/AssistantService.java` | `.../com/fantasyzone/service/` | `package com.fantasyzone.service;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/service/AssistantContextBuilder.java` | `.../com/fantasyzone/service/` | `package com.fantasyzone.service;` |
| `../../fantasyzone/backend/src/main/java/com/fantasyzone/service/AssistantRateLimiter.java` | `.../com/fantasyzone/service/` | `package com.fantasyzone.service;` |

Tests (optional but recommended, they do not call the real LLM):

| File in this `LLM` folder | Copy to |
|---|---|
| `../../fantasyzone/backend/src/test/java/com/fantasyzone/service/AnthropicLlmClientTest.java` | `backend/src/test/java/com/fantasyzone/service/` |
| `../../fantasyzone/backend/src/test/java/com/fantasyzone/service/AssistantServiceTest.java` | same folder |
| `../../fantasyzone/backend/src/test/java/com/fantasyzone/service/AssistantContextBuilderTest.java` | same folder |
| `../../fantasyzone/backend/src/test/java/com/fantasyzone/service/AssistantRateLimiterTest.java` | same folder |
| `../../fantasyzone/backend/src/test/java/com/fantasyzone/controller/AssistantApiIntegrationTest.java` | `backend/src/test/java/com/fantasyzone/controller/` |

Because the folder layout mirrors the project, you can also just copy the whole `LLM/backend/src` tree on top of your
`backend/src`. Nothing in it has the same name as an existing file.

---

## 2. Changes to existing files

### 2a. `src/main/resources/application.properties` (the only required edit)

**Add this block at the end of the file:**
```properties
# AI fantasy assistant (LLM). The key is read from the LLM_API_KEY environment variable and never stored in code.
llm.api-key=${LLM_API_KEY:}
llm.model=${LLM_MODEL:claude-haiku-4-5-20251001}
llm.base-url=https://api.anthropic.com
llm.max-tokens=700
llm.timeout-ms=60000
llm.rate-limit-per-hour=${LLM_RATE_LIMIT_PER_HOUR:20}
```
Do **not** type your real key after `llm.api-key=`. Leave it exactly as shown.

### 2b. Files you do NOT need to change (and why)
- **`pom.xml`: no new Maven dependency.** The code uses `RestClient` (already in `spring-boot-starter-web`), Jackson,
  and validation, all of which you already have.
- **`SecurityConfig.java`: no change.** Your rule `.anyRequest().authenticated()` already protects `/api/assistant/**`,
  so a valid login token is required. Admins are not required; any logged-in user can ask.
- **`GlobalExceptionHandler.java`: no change.** `LlmExceptionHandler` takes care of the new errors for the assistant
  endpoint only. It must be a separate class because your handler's catch-all `Exception` method would turn the new
  errors into HTTP 500s.
- **`src/test/resources/application.properties`: no change.** Every new `@Value` has a default, so your test
  properties keep working. (Optional: add `llm.api-key=` there if you like having it explicit.)
- **Frontend: no change.** The React app never sees the key. A small optional snippet is in section 8.

---

## 3. Set the API key securely

1. Create a key in the Anthropic Console (console.anthropic.com), under API Keys. API calls are billed to that account
   (I have not checked whether there are any free starting credits, so look at the Console's billing page). In the
   Console, **set a monthly spend limit** so a mistake cannot become a surprise bill.
2. Give the key to the backend as an **environment variable named `LLM_API_KEY`**. Pick one:

   **IntelliJ (easiest):** Run > Edit Configurations > your Spring Boot configuration > **Environment variables** >
   add `LLM_API_KEY=<your key>` > Apply. (Each run configuration has its own variables.)

   **PowerShell, current window only:**
   ```powershell
   $env:LLM_API_KEY = "paste-your-key-here"
   mvn spring-boot:run
   ```
   **PowerShell, saved for your Windows user** (restart IntelliJ and any terminals afterwards):
   ```powershell
   setx LLM_API_KEY "paste-your-key-here"
   ```
3. Rules that keep it safe:
   - Never put the key in `application.properties`, Java files, Postman collections you share, screenshots, or GitHub.
   - Never put it in the React app. Anything named `VITE_...` is bundled into the website and visible to everyone.
   - If a key is ever pasted somewhere public, delete it in the Console and create a new one.
   - The code never logs the key and never returns it. If the key is missing, the endpoint answers `503` and the rest of
     FantasyZone keeps working normally.

Optional environment variables: `LLM_MODEL` (default is a small, low-cost model; change it if you want higher quality or
if the provider retires that name) and `LLM_RATE_LIMIT_PER_HOUR` (questions per user per hour, default 20).

---

## 4. How a request flows

```
React app (user is logged in)
   |  POST /api/assistant/ask   Authorization: Bearer <JWT>   { question, teamId?, playerIds?, week? }
   v
Spring Security                  -> 401 if there is no valid login
AssistantController             -> @Valid checks (question 1-500 chars, up to 6 players, week 1-18)
   v
AssistantService
   1. AssistantRateLimiter      -> 429 if this user asked too many questions this hour
   2. AssistantContextBuilder   -> reads FantasyZone data through your EXISTING services:
                                     TeamService (team + roster; 403 if not in that league)
                                     StandingsService, MatchupService
                                     NflPlayerRepository + PlayerWeeklyStatsRepository
                                     FantasyScoringService (fantasy points are calculated by FantasyZone)
   3. builds the prompt         -> rules + <fantasyzone_data> ... </fantasyzone_data> + <question> ... </question>
   v
LlmClient -> AnthropicLlmClient -> HTTPS call to the LLM API (key added here, server side only)
   v
answer text -> AssistantResponse { answer, model, dataUsed } -> back to React
```

**What the LLM sees:** the league's scoring rules and, if requested, a team's name, manager display name, record,
roster with recent fantasy points (last 4 games, calculated by FantasyZone), the top of the standings, and the team's
matchup; or the recent stats of up to 6 chosen players. **It never sees** email addresses, passwords, tokens, join
codes, or other users' private data. Team access follows your existing rule: any league member can look at a team in
their league, nobody else can.

**What the LLM cannot do:** change anything. It has no tools and no database access, and the prompt tells it to treat
the question and data as untrusted text. The `dataUsed` field in each response lists exactly what was sent.

---

## 5. Test it

### 5a. Automated (no API key or internet needed)
```
mvn test -Dtest="Assistant*Test,AnthropicLlmClientTest"
```
These tests mock the LLM provider. They check the request sent to the provider, error handling, rate limiting, access
rules, the prompt contents, and the full endpoint with real login security.

### 5b. Manual, with a real key
1. Set `LLM_API_KEY` (section 3) and start the backend.
2. In PowerShell (replace the email, password, and team id with yours):
```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType 'application/json' -Body '{"email":"you@example.com","password":"YourPassword1!"}'
$h = @{ Authorization = "Bearer $($login.token)" }

# Summarize my team
$body = @{ question = "Summarize my team's strengths and weaknesses"; teamId = 1 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/assistant/ask -Headers $h -ContentType 'application/json' -Body $body

# Compare two players (use ids from GET /api/players)
$body = @{ question = "Compare these two players. Who is the better start?"; playerIds = @(1,2) } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/assistant/ask -Headers $h -ContentType 'application/json' -Body $body
```
**Postman:** `POST http://localhost:8080/api/assistant/ask`, Authorization tab > Bearer Token (paste the login token),
Body > raw > JSON.

**Example request and response**
```json
{ "question": "Who should I start at QB?", "teamId": 1, "playerIds": [1, 2], "week": 4 }
```
```json
{ "answer": "Start Josh Allen. He averaged 27.7 points over his last game ...",
  "model": "claude-haiku-4-5-20251001",
  "dataUsed": ["League scoring rules", "Team \"Alice FC\" with 5 roster players", "League standings", "Recent stats for Josh Allen"] }
```
Fields: `question` (required), `teamId` (optional, include a team's data), `playerIds` (optional, up to 6),
`week` (optional, 1 to 18, picks the matchup week).

### 5c. What the errors mean
| Status | Cause |
|---|---|
| 400 | Empty or too long question, more than 6 players, bad week (message lists the field) |
| 401 | Not logged in or expired token |
| 403 | You asked about a team in a league you are not in |
| 404 | Team or player id does not exist |
| 429 | You passed the per-hour question limit |
| 503 | `LLM_API_KEY` is not set, or the provider is busy (the message says which) |
| 502 | The provider rejected the key, timed out, or failed. Check the backend console log for the HTTP status |

---

## 6. Good to know
- **No new data:** the assistant only knows what FantasyZone has: scoring rules, rosters, standings, matchups, and the
  weekly stats you have synced. It has no injury reports, news, or opponent defense data, and the prompt tells it to say
  so instead of guessing. Sync more weeks (Admin page) to give it better recent-form data.
- **Cost control:** short answers (`llm.max-tokens`), a low-cost default model, and the per-user hourly limit. The limit is
  kept in memory, so it resets when the backend restarts.
- **Changing providers:** write another class that implements `LlmClient` (two methods) and remove or disable
  `AnthropicLlmClient`. Nothing else changes.
- **Only existing code touched by the new files:** `AssistantContextBuilder` calls `TeamService.get/roster`,
  `StandingsService.standings`, `MatchupService.list`, `FantasyScoringService.calculateFantasyPoints`,
  `NflPlayerRepository.findById`, and `PlayerWeeklyStatsRepository.findByNflPlayerId`, plus the DTO records
  `TeamResponse`, `RosterEntryResponse`, `StandingRow`, and `MatchupResponse`. I wrote against the version of
  FantasyZone I generated earlier. If you renamed or changed any of those, `AssistantContextBuilder` is the one file to
  adjust.

## 7. Troubleshooting
| Symptom | Fix |
|---|---|
| `503 ... not configured` | `LLM_API_KEY` was not visible to the backend. In IntelliJ use the run configuration's Environment variables; restart after `setx`. |
| `502 ... rejected this server's credentials` | Wrong or revoked key, or the Console account has no credit. |
| `502 ... could not answer` and the log shows HTTP 404 | `LLM_MODEL` is not a valid model name for your account. |
| Compile error "cannot find symbol" in `AssistantContextBuilder` | One of the existing methods listed above has a different name or signature in your copy. |
| Answers feel generic | Sync player stats first, and include `teamId` or `playerIds` in the request. |

## 8. Optional: calling it from React
The browser only ever calls **your** backend with the user's login token. Example for `src/services/api.ts`:
```ts
export const assistantApi = {
  ask: (body: { question: string; teamId?: number; playerIds?: number[]; week?: number }) =>
    request<{ answer: string; model: string; dataUsed: string[] }>('/api/assistant/ask', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
};
```
(`request` is your existing helper that adds the JWT header.) Show `dataUsed` to users and label answers as AI-generated.
