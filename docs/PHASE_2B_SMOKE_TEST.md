# Phase 2B Manual Smoke-Test Plan

This checklist is executable against the current `phase-02b-endpoint-security` branch. Record the date, tester, environment, account IDs, response status, and result for every item. Do not use production credentials.

## Preparation

- [ ] Start MySQL and the backend with all required environment variables.
- [ ] Start the React frontend at the configured CORS origin.
- [ ] Open browser developer tools and preserve the Network log.
- [ ] Prepare Passenger A and Passenger B, Driver A and Driver B, Operator A and Operator B, and one administrator account.
- [ ] Record each test user's database ID and create owned/foreign booking, bus, and invitation IDs where required.
- [ ] Use an incognito window or clear site data between roles so sessions are not reused.

## Public flows

| Account / role | Page or endpoint | Action | Expected result |
| --- | --- | --- | --- |
| Anonymous | `/register`, `POST /api/auth/send-otp` | Enter a new email and request an OTP. | HTTP 200; visible OTP-sent confirmation; email arrives. |
| Anonymous | `POST /api/auth/verify-otp` | Submit the received OTP. | HTTP 200; visible OTP-verified result. |
| Anonymous | `/register`, `POST /api/auth/register` | Complete registration for a passenger test account. | HTTP 200; “Successfully registered”; session cookie is created. |
| Anonymous | `/login`, `POST /api/auth/login` | Log in with the new account. | HTTP 200; role-appropriate navigation; authenticated requests send the session cookie. |
| Anonymous | `/forgot-password`, `POST /api/auth/send-forgot-password-otp` | Request a password-reset OTP. | HTTP 200; reset OTP confirmation and email. |
| Anonymous | `/forgot-password`, `POST /api/auth/reset-password` | Submit the OTP and a new password, then log in. | HTTP 200 reset result; login with the new password succeeds. |
| Anonymous, Google test account | Google button, `/api/auth/google-login` | Complete Google login if localhost redirect URIs and credentials are configured. | Google callback succeeds; `/google-success` opens; passenger session can access its protected endpoint. If OAuth is unavailable locally, record “not executable” with reason. |
| Anonymous | `/routes`, `/`, `/help` | Open public frontend pages without signing in. | Pages remain visible; no protected API data is disclosed. |

## Passenger flows

| Account / role | Page or endpoint | Action | Expected result |
| --- | --- | --- | --- |
| Passenger A | `/wallet`, `GET /api/wallet/balance/{A}` | Open own wallet. | HTTP 200; own balance and transaction history appear. |
| Passenger A | `/wallet`, wallet PIN endpoints | Create or verify Passenger A's PIN. | HTTP 200; visible success result. |
| Passenger A | `/my-bookings`, `GET /api/bookings/user/{A}` | Open own bookings. | HTTP 200; only Passenger A bookings appear. |
| Passenger A | `/book-ticket` → payment, `POST /api/bookings/create` | Select a bus/seat and complete wallet payment. | HTTP 200 with `success: true`; ticket page opens and balance decreases. |
| Passenger A | `GET /api/bookings/{A-booking}/ticket-pdf` | Download own ticket. | HTTP 200; PDF downloads and opens. |
| Passenger A | `PUT /api/bookings/{A-booking}/cancel` | Cancel own confirmed booking. | HTTP 200; booking becomes cancelled and wallet refund appears. |
| Passenger A | `GET /api/wallet/balance/{B}` | Replace the URL ID with Passenger B's ID. | HTTP 404; no balance disclosed. |
| Passenger A | `GET /api/bookings/{B-booking}/ticket-pdf` | Request Passenger B's ticket ID. | HTTP 404; no PDF or booking details disclosed. |
| Passenger A | `PUT /api/bookings/{B-booking}/cancel` | Attempt to cancel Passenger B's booking. | HTTP 404; Passenger B booking remains unchanged. |
| Passenger A | `/api/operator/buses`, `/api/admin/drivers/pending` | Call operator and admin endpoints using Passenger A's session. | HTTP 403 for both. |

## Driver flows

| Account / role | Page or endpoint | Action | Expected result |
| --- | --- | --- | --- |
| Driver A | `/driver/application`, `POST /api/drivers/application` | Submit own application after registration. | HTTP 200; application status becomes pending. |
| Driver A | `/driver/profile`, `GET /api/drivers/profile/{A}` | Open own profile/application data. | HTTP 200; Driver A data appears. |
| Driver A | `/driver/notifications`, `GET /api/driver/operator-invitations` | View invitations. | HTTP 200; only Driver A invitations appear. |
| Driver A | `POST /api/driver/operator-invitations/{A-invitation}/accept` | Accept one pending invitation. | HTTP 200; invitation becomes active and association is visible. |
| Driver A with a separate pending invitation | `POST /api/driver/operator-invitations/{A-invitation}/reject` | Reject the pending invitation. | HTTP 200; invitation becomes rejected. |
| Driver A | `GET /api/drivers/profile/{B}` | Request Driver B's user ID. | HTTP 404; Driver B data is not disclosed. |
| Driver A | Accept/reject endpoint with Driver B invitation ID | Attempt to respond to Driver B's invitation. | HTTP 404; Driver B invitation remains unchanged. |
| Driver A | `/api/admin/drivers/pending` | Call an admin endpoint. | HTTP 403. |

## Operator flows

| Account / role | Page or endpoint | Action | Expected result |
| --- | --- | --- | --- |
| Approved Operator A | `/operator/dashboard`, `GET /api/operator/dashboard` | Open dashboard. | HTTP 200; only Operator A summary is shown. |
| Approved Operator A | `/operator/buses`, `GET /api/operator/buses` | List buses. | HTTP 200; only Operator A buses appear. |
| Approved Operator A | `/operator/buses/register`, `POST /api/operator/buses` | Register a unique bus. | HTTP 201; bus appears with pending approval state. |
| Approved Operator A | `/operator/drivers`, `GET /api/operator/drivers/eligible` | Search eligible drivers. | HTTP 200; approved available drivers are returned. |
| Approved Operator A | `POST /api/operator/driver-invitations` | Invite Driver A. | HTTP 201; invitation appears for Operator A and Driver A. |
| Approved Operator A | `GET /api/operator/buses/{B-bus}` | Request Operator B's bus ID. | HTTP 404; no Operator B bus data is disclosed. |
| Operator A | Submit/resubmit request containing Operator B user ID | Attempt to submit for Operator B. | HTTP 404; Operator B record is unchanged. |
| Operator A | `/api/admin/buses` or an approval endpoint | Attempt admin bus access/approval. | HTTP 403; bus status remains unchanged. |

## Administrator flows

| Account / role | Page or endpoint | Action | Expected result |
| --- | --- | --- | --- |
| Administrator | `/admin/login`, `POST /api/admin/auth/login` | Log in with the configured administrator account. | HTTP 200; admin dashboard opens and session cookie is present. |
| Administrator | `/admin/driver-applications`, driver approve/reject endpoint | Review and decide a pending driver application. | HTTP 200; status and review details update. |
| Administrator | `/admin/operators`, operator approve/reject endpoint | Review and decide a pending operator application. | HTTP 200; operator status updates. |
| Administrator | `/admin/buses`, bus approve/reject endpoint | Review and decide a pending bus. | HTTP 200; bus status updates. |
| Passenger, Driver, Operator | Any `/api/admin/**` endpoint | Repeat one admin read and one approval call with each non-admin session. | HTTP 403; no administrative state changes. |
| Anonymous | Any `/api/admin/**` endpoint except login | Call without a session cookie. | HTTP 401. |

## Completion record

- Tester:
- Date/time:
- Backend commit/working tree:
- Browser:
- MySQL database used:
- Passed:
- Failed:
- Not executable:
- Evidence/notes:
