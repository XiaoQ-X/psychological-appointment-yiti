# Student UI design QA

## Scope

- Login: `design/student-final-v1/01-login.png`
- Appointment home: `design/student-final-v1/02-appointment-home.png`
- Counselor detail: `design/student-final-v1/03-counselor-detail.png`

Implementation is in the original native mini-program routes. Previous source is
preserved outside the build at `design/source-archive/student-ui-before-final-v1`.

## Completed checks

- Preserved the existing student authentication, counselor, slot and appointment
  APIs and the existing booking navigation contract.
- Replaced the home banner with a native three-slide carousel using optimized
  raster assets; source-resolution images are archived under the design folder.
- Added real loading, empty-slot, selected-date and selected-slot states.
- Verified a real student login against the local backend. The account can read
  three counselors and one appointment. The next seven days currently contain no
  available slots, so the real preview should show the empty availability state.
- Static JavaScript, JSON, WXML binding and whitespace checks are listed in the
  final verification output for this change.

## Visual comparison status

Completed in WeChat DevTools Stable `2.01.2510290` with base library `3.15.2`.
The automated preview viewport reports 430 x 834 CSS pixels. Captures and comparison
boards are stored under
`design/screenshots/student-final-implementation-20260712`.

The final run covered the login page, real student login, fully loaded appointment
home and counselor detail. It reported no mini-program exceptions or severe logs.
The login used the mini-program's own login request; the API-session fallback in
the QA harness was not used.

## Findings fixed during visual QA

- The native WeChat `button` margins reduced the login button to 184 px despite a
  percentage width declaration. Explicit width and margin overrides now produce a
  394 px button matching the form container.
- A past confirmed appointment was incorrectly presented as the next appointment.
  The home page now filters by active status and a non-expired end time, then sorts
  future appointments by start time.
- The counselor profile band was missing the reference design's quiet line motif.
  It now uses the existing optimized raster line asset.

## Data-dependent differences

- The local counselor records do not have avatar URLs, so the verified preview uses
  the designed initial-letter fallback instead of the reference portraits.
- The next seven days currently have no available slots. Home and detail therefore
  show real empty availability states; selected-date and selected-slot visuals can
  only be screenshot-tested after a future slot is generated.

## Verification

- Native mini-program JavaScript, JSON, event-binding and whitespace checks pass.
- Backend Maven suite: 27 tests, 0 failures, 0 errors, 0 skipped.
- Final automation report: 0 exceptions, 0 severe logs, login fallback not used.

Final result: accepted for the current real-data states. Keep the archived source
until the remaining student pages are implemented and the complete student flow is
accepted.
