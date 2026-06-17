# Legalidad Page Overrides

> **PROJECT:** Pokemon TCG Deck Builder
> **Generated:** 2026-06-17 15:32:58
> **Page Type:** Search Results

> ⚠️ **IMPORTANT:** Rules in this file **override** the Master file (`design-system/MASTER.md`).
> Only deviations from the Master are documented here. For all other rules, refer to the Master.

---

## Page-Specific Rules

### Layout Overrides

- **Max Width:** 1200px (standard)
- **Layout:** Full-width sections, centered content
- **Sections:** 1. Intro (Vertical), 2. The Journey (Horizontal Track), 3. Detail Reveal, 4. Vertical Footer

### Spacing Overrides

- No overrides — use Master spacing

### Typography Overrides

- No overrides — use Master typography

### Color Overrides

- **Strategy:** Continuous palette transition. Chapter colors. Progress bar #000000.

### Component Overrides

- Avoid: Validate only on submit
- Avoid: No feedback after submit
- Avoid: No feedback during loading

---

## Page-Specific Components

- No unique components for this page

---

## Recommendations

- Effects: Neon glow (text-shadow), glitch animations (skew/offset), scanlines (::before overlay), terminal fonts
- Forms: Validate on blur for most fields
- Forms: Show loading then success/error state
- Feedback: Show spinner/skeleton for operations > 300ms
- CTA Placement: Floating Sticky CTA or End of Horizontal Track
