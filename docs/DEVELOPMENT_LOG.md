# Yatayat Development Log

## Phase 2B — Endpoint Security, Permission Matrix, and Ownership Enforcement

**Branch:** `phase-02b-endpoint-security`  
**Baseline:** `66d3f98` — Phase 2A merged into `main`

### Scope completed

- Replaced the permissive backend fallback with an explicit public endpoint allowlist and authenticated default.
- Applied role rules for passenger, driver, operator, and administrator endpoint families.
- Enforced authenticated ownership for wallet, booking, password-change, driver application/profile/dashboard, and operator-application operations without renaming APIs.
- Preserved and verified existing operator bus and driver-invitation ownership behavior.
- Ensured normal registration and Google login create role-bearing server sessions for protected follow-on flows.
- Added credentials to existing protected frontend requests that previously omitted the session cookie.
- Removed the temporary `/api/test` endpoint and the database-information `/api/auth/db-check` endpoint.
- Removed logging of OTP values.
- Added null-safe ownership error responses.
- Added a complete API permission matrix and expanded security setup documentation.

### Automated verification

- `mvnw.cmd clean test`: **passed**; 19 tests passed with zero failures, errors, or skips.
- `npm run build`: **passed**; the existing large-bundle warning remains.
- `npm run lint`: **failed** with the pre-existing baseline of 28 errors and 12 warnings. Findings are primarily existing hook-purity/dependency rules and unused imports; Phase 2B did not broaden scope to unrelated frontend cleanup.

### Security tests added

- Anonymous denial for passenger, driver, operator, and administrator endpoints.
- Cross-role denial for passenger, driver, and operator access to privileged endpoint families.
- Administrator approval access.
- Cross-passenger wallet, ticket, and cancellation denial.
- Cross-driver profile and invitation denial.
- Cross-operator bus denial and authenticated operator identity for invitations/applications.

### Known limitations retained

- Driver QR operations are role-protected, but trip-level ownership cannot be enforced until a real trip-assignment domain exists.
- CSRF hardening is not included in Phase 2B.
- OTP and password-reset redesign is deferred to its dedicated security phase.
- Wallet top-up still lacks external payment-provider verification.
- Frontend lint debt remains and must be handled as a separate, focused quality phase.

### Final verification review

- Compared frontend lint against baseline commit `66d3f98` using an archived baseline source tree and the same installed ESLint configuration.
- Baseline result: 28 errors and 12 warnings.
- Phase 2B result: 28 errors and 12 warnings.
- The findings are identical by affected file and rule; Phase 2B introduced no lint regression.
- CSRF remains disabled because the React client has no token bootstrap/header flow. A focused pre-deployment implementation is documented in `SECURITY_SETUP.md`.
- An executable role-by-role manual plan is recorded in `PHASE_2B_SMOKE_TEST.md`; it has not been claimed as executed.

### Commit status

No commit was created automatically. The proposed commit message after review is:

```text
feat(security): enforce endpoint roles and resource ownership
```
