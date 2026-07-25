# Frontend Accessibility Audit Plan

Last updated: 2026-07-25

## Scope

Practical WCAG-oriented checks for keyboard, focus, labels, and contrast.

## Current Implemented Baseline

- Visible focus styles on input/select/textarea.
- Reduced motion support via prefers-reduced-motion.
- Role-based unauthorized messaging screen for restricted routes.

## Audit Checklist

1. Keyboard-only navigation:
   - Verify all interactive elements reachable in logical order.
2. Focus visibility:
   - Confirm visible focus ring for buttons/links/inputs across pages.
3. Form labeling:
   - Add explicit labels/aria-label where placeholders are currently the only cue.
4. Color contrast:
   - Validate contrast for all status badges and CTA buttons.
5. Screen reader context:
   - Ensure form errors and success messages are announced.

## Next Actions

- Add aria-live region for toast and request-state alerts.
- Replace placeholder-only fields with label+helper text across all forms.
- Run manual pass on auth/jobs/companies/applications/notifications/monitoring pages.
