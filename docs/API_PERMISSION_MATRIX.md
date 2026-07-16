# Yatayat API Permission Matrix

This matrix reflects the backend after Phase 2B. Access is deny-by-default: only the explicit public allowlist is anonymous, role families require the named role, and ownership-sensitive operations resolve the authenticated session principal.

## Access classifications

- **PUBLIC**: anonymous access is intentional.
- **AUTHENTICATED**: any signed-in application role.
- **PASSENGER / DRIVER / OPERATOR / ADMIN**: requires that exact role.
- **OWNER_ONLY**: requires the role and a resource belonging to the authenticated account.

## Controller endpoints

| Method | Endpoint | Controller | Purpose | Before Phase 2B | Access | Role | Ownership rule | Notes / risks |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| POST | `/api/admin/auth/login` | AdminAuthController | Administrator login | Public | PUBLIC | — | — | Creates the existing server session. |
| POST | `/api/admin/auth/logout` | AdminAuthController | Administrator logout | Public through fallback | ADMIN | ADMIN | Current session | Invalidates the current session. |
| GET | `/api/admin/buses` | AdminBusController | List buses for review | Admin protected | ADMIN | ADMIN | — | Optional status filter. |
| GET | `/api/admin/buses/{busId}` | AdminBusController | View a bus review record | Admin protected | ADMIN | ADMIN | — | Administrative oversight. |
| PUT | `/api/admin/buses/{busId}/approve` | AdminBusController | Approve a bus | Admin protected | ADMIN | ADMIN | — | Operator cannot access. |
| PUT | `/api/admin/buses/{busId}/reject` | AdminBusController | Reject a bus | Admin protected | ADMIN | ADMIN | — | Requires rejection reason in existing service. |
| GET | `/api/admin/drivers/pending` | AdminDriverController | List pending driver applications | Public through fallback | ADMIN | ADMIN | — | Now administrator-only. |
| GET | `/api/admin/drivers/{profileId}` | AdminDriverController | View driver application | Public through fallback | ADMIN | ADMIN | — | Contains private application data. |
| PUT | `/api/admin/drivers/{profileId}/approve` | AdminDriverController | Approve driver | Public through fallback | ADMIN | ADMIN | — | Now administrator-only. |
| PUT | `/api/admin/drivers/{profileId}/reject` | AdminDriverController | Reject driver | Public through fallback | ADMIN | ADMIN | — | Now administrator-only. |
| GET | `/api/admin/operators` | AdminOperatorController | List operator applications | Public through fallback | ADMIN | ADMIN | — | Contains private operator data. |
| PUT | `/api/admin/operators/{operatorId}/approve` | AdminOperatorController | Approve operator | Public through fallback | ADMIN | ADMIN | — | Now administrator-only. |
| PUT | `/api/admin/operators/{operatorId}/reject` | AdminOperatorController | Reject operator | Public through fallback | ADMIN | ADMIN | — | Now administrator-only. |
| GET | `/api/auth/google-login` | AuthController | Start Google login | Public | PUBLIC | — | — | OAuth framework completes the callback. |
| GET | `/api/auth/google-register` | AuthController | Start Google registration | Public | PUBLIC | — | — | OAuth framework completes the callback. |
| POST | `/api/auth/send-otp` | AuthController | Request registration OTP | Public | PUBLIC | — | — | OTP hardening is deferred; value logging removed. |
| POST | `/api/auth/verify-otp` | AuthController | Verify registration OTP | Public | PUBLIC | — | — | OTP redesign is outside Phase 2B. |
| POST | `/api/auth/register` | AuthController | Register passenger/driver/operator | Public | PUBLIC | — | Newly created account | Establishes a role-bearing session so application flows remain functional. |
| POST | `/api/auth/login` | AuthController | User login | Public | PUBLIC | — | — | Creates the existing server session. |
| POST | `/api/auth/logout` | AuthController | User logout | Public through `/api/auth/**` | AUTHENTICATED | Any | Current session | Invalidates the current session. |
| POST | `/api/auth/change-password` | AuthController | Change current password | Public through `/api/auth/**` | OWNER_ONLY | Any | Account is derived from authenticated email | Request email no longer selects the account. |
| POST | `/api/auth/send-forgot-password-otp` | AuthController | Request reset OTP | Public | PUBLIC | — | — | Public by workflow necessity. |
| POST | `/api/auth/reset-password` | AuthController | Complete password reset | Public | PUBLIC | — | — | Reset redesign is outside Phase 2B. |
| POST | `/api/bookings/create` | BookingController | Create and pay for booking | Public through fallback | OWNER_ONLY | PASSENGER | Request user ID must equal session user | Wallet PIN remains required. |
| PUT | `/api/bookings/{bookingId}/cancel` | BookingController | Cancel and refund booking | Public through fallback | OWNER_ONLY | PASSENGER | Booking must belong to session passenger | Foreign ID returns 404. |
| GET | `/api/bookings/{bookingId}/ticket-pdf` | BookingController | Download ticket PDF | Public through fallback | OWNER_ONLY | PASSENGER | Booking must belong to session passenger | Foreign ID returns 404. |
| GET | `/api/bookings/user/{userId}` | BookingController | List passenger bookings | Public through fallback | OWNER_ONLY | PASSENGER | User ID must equal session user | Existing path retained. |
| POST | `/api/bookings/validate-qr` | BookingController | Validate ticket QR | Public through fallback | DRIVER | DRIVER | No trip scope exists yet | Assignment-level validation remains a risk. |
| POST | `/api/bookings/mark-used` | BookingController | Mark ticket used | Public through fallback | DRIVER | DRIVER | No trip scope exists yet | Assignment-level validation remains a risk. |
| POST | `/api/drivers/application` | DriverApplicationController | Submit driver application | Public through fallback | OWNER_ONLY | DRIVER | Email is derived from session user | Existing email form field is ignored for identity. |
| GET | `/api/drivers/status/{userId}` | DriverApplicationController | View application status | Public through fallback | OWNER_ONLY | DRIVER | User ID must equal session user | Foreign ID returns 404. |
| GET | `/api/drivers/profile/{userId}` | DriverApplicationController | View driver profile | Public through fallback | OWNER_ONLY | DRIVER | User ID must equal session user | Foreign ID returns 404. |
| GET | `/api/drivers/dashboard/{userId}` | DriverDashboardController | View driver dashboard | Public through fallback | OWNER_ONLY | DRIVER | User ID must equal session user | Foreign ID returns 404. |
| GET | `/api/driver/operator-invitations` | DriverOperatorInvitationController | List driver invitations | Driver protected | OWNER_ONLY | DRIVER | Driver derived from session email | Existing ownership service retained. |
| POST | `/api/driver/operator-invitations/{associationId}/accept` | DriverOperatorInvitationController | Accept operator invitation | Driver protected | OWNER_ONLY | DRIVER | Invitation must belong to session driver | Foreign ID returns 404. |
| POST | `/api/driver/operator-invitations/{associationId}/reject` | DriverOperatorInvitationController | Reject operator invitation | Driver protected | OWNER_ONLY | DRIVER | Invitation must belong to session driver | Foreign ID returns 404. |
| GET | `/api/driver/operator-association` | DriverOperatorInvitationController | View active operator association | Driver protected | OWNER_ONLY | DRIVER | Driver derived from session email | — |
| POST | `/api/operators/application` | OperatorApplicationController | Submit operator application | Public through fallback | OWNER_ONLY | OPERATOR | Request user ID must equal session user | Existing path and payload retained. |
| PUT | `/api/operators/application/resubmit` | OperatorApplicationController | Resubmit rejected application | Public through fallback | OWNER_ONLY | OPERATOR | Request user ID must equal session user | — |
| GET | `/api/operators/status/{userId}` | OperatorApplicationController | View operator application status | Public through fallback | OWNER_ONLY | OPERATOR | User ID must equal session user | Foreign ID returns 404. |
| POST | `/api/operator/buses` | OperatorBusController | Register operator bus | Operator protected | OWNER_ONLY | OPERATOR | Operator derived from session email | Bus starts in approval workflow. |
| GET | `/api/operator/buses` | OperatorBusController | List operator buses | Operator protected | OWNER_ONLY | OPERATOR | Query is scoped to session operator | — |
| GET | `/api/operator/buses/{busId}` | OperatorBusController | View operator bus | Operator protected | OWNER_ONLY | OPERATOR | Repository requires bus and session operator | Foreign ID returns 404. |
| GET | `/api/operator/dashboard` | OperatorDashboardController | View operator dashboard | Operator protected | OWNER_ONLY | OPERATOR | Operator derived from session email | Approved operator required by service. |
| GET | `/api/operator/drivers` | OperatorDriverController | List associated drivers | Operator protected | OWNER_ONLY | OPERATOR | Operator derived from session email | — |
| GET | `/api/operator/drivers/eligible` | OperatorDriverController | Search eligible drivers | Operator protected | OWNER_ONLY | OPERATOR | Search initiated by session operator | Returns approved available drivers. |
| POST | `/api/operator/driver-invitations` | OperatorDriverController | Invite driver | Operator protected | OWNER_ONLY | OPERATOR | Inviting operator derived from session email | Request identifies only the target driver. |
| GET | `/api/wallet/balance/{userId}` | WalletController | Read wallet balance | Public through fallback | OWNER_ONLY | PASSENGER | User ID must equal session user | Foreign ID returns 404. |
| POST | `/api/wallet/topup` | WalletController | Top up wallet | Public through fallback | OWNER_ONLY | PASSENGER | Request user ID must equal session user | Payment-provider verification remains outside this phase. |
| GET | `/api/wallet/history/{userId}` | WalletController | Read wallet transactions | Public through fallback | OWNER_ONLY | PASSENGER | User ID must equal session user | Foreign ID returns 404. |
| POST | `/api/wallet/create-pin` | WalletController | Create wallet PIN | Public through fallback | OWNER_ONLY | PASSENGER | Request user ID must equal session user | PIN remains BCrypt encoded. |
| POST | `/api/wallet/verify-pin` | WalletController | Verify wallet PIN | Public through fallback | OWNER_ONLY | PASSENGER | Request user ID must equal session user | — |
| GET | `/api/wallet/pin-status/{userId}` | WalletController | Read PIN setup status | Public through fallback | OWNER_ONLY | PASSENGER | User ID must equal session user | — |
| POST | `/api/wallet/pay` | WalletController | Debit wallet | Public through fallback | OWNER_ONLY | PASSENGER | Request user ID must equal session user | Endpoint retained; booking has its own payment transaction. |

## Framework and non-controller paths

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| OPTIONS | `/api/**` | PUBLIC | CORS preflight. |
| GET | `/oauth2/**` | PUBLIC | Spring Security OAuth authorization flow. |
| GET | `/login/**` | PUBLIC | Spring Security OAuth callback/login support. |
| Any | `/error` | PUBLIC | Framework error dispatch. |
| GET | `/` | PUBLIC | Application root if served by the backend. |

All other requests require authentication. Unknown requests are not made anonymously accessible by a fallback rule.

## Removed development endpoints

| Former endpoint | Action | Reason |
| --- | --- | --- |
| `GET /api/test` | Removed | Temporary backend test response exposed no production function. |
| `GET /api/auth/db-check` | Removed | Exposed database URL and user count. |

## Known scope limitations

- QR validation and ticket use are restricted to drivers, but the current data model has no trip assignment that can prove the scanning driver owns the relevant trip.
- There is no operator bus-update endpoint, so cross-operator bus modification is not exposed.
- Operator invitation endpoints do not accept an operator or association owner ID; the operator identity always comes from the authenticated session.
- CSRF and OTP redesign are separate security work and are not represented as completed here.
