# Personal Assistant — Android agent shell

A dark, mobile-first agent dashboard with text chat, voice input/output, local skills, safe file placeholders, and a provider-neutral backend boundary.

## Architecture and safety

- The APK contains **no AI-provider key** and never calls DeepSeek (or another provider) directly.
- `AgentBackendClient` calls only a configured, strict HTTPS app-owned endpoint and accepts an optional Google ID token supplied by the host integration.
- If `agent.backend.url`/`AGENT_BACKEND_URL` is absent, invalid, or unreachable, the app keeps working through its local/offline rule engine.
- Local `ApprovalPolicy` stops requests involving send, post, delete, purchase, permission, access, account, or security until explicit human approval. Dashboard quick actions are explanatory only (Skills opens the existing skills view).
- No Google auth SDK, Firebase SDK, fake login, or external Android project configuration is included. Owner Firebase/Google setup is required before identity-backed backend use.

The sample Functions endpoint is in [`backend/`](backend/README.md). It verifies a Google identity token, allow-lists owner emails, uses a server-only DeepSeek secret, makes flash/pro routing explicit, and returns only `{reply}`.

## Android setup

1. Create `local.properties` with `sdk.dir=/path/to/Android/Sdk`.
2. Optionally add `agent.backend.url=https://your-owned-endpoint.example/agent`, or export `AGENT_BACKEND_URL` while building. Do not add provider keys.
3. Install NDK `27.3.13750724` and CMake `3.31.5`, then run:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug --no-daemon
   ```

The endpoint field in Settings is intentionally URL-only and rejects non-HTTPS URLs, credentials, query parameters, and fragments. See `backend/README.md` for deployment, identity, secret, and mandatory approval requirements.

## Main dependencies

AndroidX, Material Components, OkHttp, Kotlin coroutines, lifecycle runtime, and JUnit/MockWebServer. Cloud/provider credentials are backend-only.
