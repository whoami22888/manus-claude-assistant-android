# Agent backend (Firebase Functions concept)
The Android app posts `{message, idToken?}` to an owner-managed endpoint and receives only `{reply:string}`. It carries no provider key and falls back locally if no valid HTTPS URL is configured.

## Owner setup
```bash
firebase init functions
cd functions && npm install firebase-functions firebase-admin
firebase functions:secrets:set DEEPSEEK_API_KEY
firebase deploy --only functions:agent
```
Configure `OWNER_EMAILS` as a comma-separated Functions environment value (or replace `owners()` with secret-backed config). Set `agent.backend.url=https://<region>-<project>.cloudfunctions.net/agent` in Android `local.properties`, or export `AGENT_BACKEND_URL` while building. Never put provider keys, Google client secrets, or an owner allow-list in Android.

## Identity and approval
The endpoint verifies a Google ID token using Firebase Admin and allow-lists its verified email. Android intentionally has no Google/Firebase auth SDK; the owner must establish token acquisition and inject it into `AgentBackendClient` separately.

Every consequential action (send, post, delete, purchase, permission, access, account, security) must carry an `action` and `approved: true` only after human review. This sample only returns a reply; it executes no external actions. Preserve approval records and server-side enforcement before adding tools.

`flash` (default) uses `deepseek-chat`; explicit `pro` uses `deepseek-reasoner`. `DEEPSEEK_API_KEY` is resolved only inside Functions.
