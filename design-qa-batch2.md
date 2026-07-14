# Student UI design QA - batch 2

## Scope

- `04-booking-form.png` -> `pages/student/booking-form/index`
- `05-consent.png` -> `pages/student/consent/index`
- `06-booking-result.png` -> `pages/student/booking-result/index`
- `07-my-appointments.png` -> `pages/student/appointments/index`
- `08-appointment-detail.png` -> `pages/student/appointment-detail/index`
- `09-cancel-dialog.png` -> appointment detail cancellation sheet
- `10-canceled-detail.png` -> canceled appointment state
- `11-emergency.png` -> `pages/student/emergency/index`

Previous source is preserved under
`design/source-archive/student-ui-before-final-v1`. Final captures and
side-by-side boards are under
`design/screenshots/student-final-implementation-20260712-batch2`.

## Real integration coverage

- Student session created through the real local login API.
- Current consent version loaded through `/api/public/consent/current`.
- Booking result, appointment list and appointment detail loaded from the real
  student appointment APIs.
- The real historical test appointment is still stored as `CONFIRMED` after its
  end time. Student pages now present this as `待更新` and disable cancellation,
  while preserving the backend status for administrative correction.

## Non-mutating display states

- Booking form fields use local display data because no future slot currently
  exists. The submit action was not invoked, so no slot was locked and no new
  appointment was created.
- Cancelable detail, selected cancellation reason and canceled detail were rendered
  with page-local display data after the real detail loaded.
- No cancellation request was sent. The known test appointment was not modified.
- Emergency phone controls were visually verified without initiating a phone call.

## Findings fixed during visual QA

- Native button minimum width made the refresh control oval; its dimensions and
  margins are now explicitly constrained.
- Global bottom-action padding overrode the booking and consent safe-area spacing;
  page-specific selectors now preserve visible consent/version rows.
- Cancellation sheet padding caused horizontal overflow; border-box sizing now
  keeps date and time inside the summary.
- Optional cancellation notes appeared for every reason and pushed the warning
  below the action bar. Notes now appear only when `其他原因` is selected.
- Backend issue/contact codes leaked into student-facing text; known values are now
  mapped to Chinese labels.

## Verification

- Final WeChat DevTools run: 12 screenshots, 0 exceptions, 0 severe logs.
- `realCancellationPerformed`: `false`.
- `phoneCallPerformed`: `false`.
- JavaScript syntax, page JSON, WXML event bindings and diff whitespace checks pass.
- Backend Maven suite: 27 tests, 0 failures, 0 errors, 0 skipped.

Final result: accepted for the available real-data and non-mutating display states.
Real slot locking and appointment submission still require a newly generated future
slot before release-candidate acceptance.
