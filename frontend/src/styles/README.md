# Design system (DSP v4)

Source of truth: `DSP_v4_Redesign_Concept_2.pdf`. Values tagged `[INF]` in `tokens.css` were inferred from the PDF visuals, not printed in it.

| Layer | File | Use |
|---|---|---|
| Raw tokens | `src/styles/tokens.css` | CSS variables `--ds-*`. Edit values here only. |
| Tailwind bridge | `src/styles/theme.css` | Utilities: `bg-brand`, `text-ink-2`, `bg-danger-tint`, `rounded-card`, `shadow-card`, `font-mono` |
| Type scale | `src/styles/typography.css` | `.ds-display .ds-h1 .ds-question .ds-h2 .ds-body .ds-caption .ds-eyebrow .ds-mono .ds-code` |
| Fonts (offline) | `src/styles/fonts.css` | `@fontsource` imports: Plus Jakarta Sans, JetBrains Mono |
| Global base | `src/styles/base.css` | Page background, focus ring, reduced motion |
| PrimeNG theme | `src/app/core/theme/dsp-preset.ts` | Primary colour, radius, surfaces; wired in `app.config.ts` |
| Shared UI | `src/app/shared/ui/` | `DsStatusPill`, `DsPriorityBadge`, `DsSlaMeter`, `DsCallout`, `DsSidebar`, `DsBottomNav`, `DsAppBar`, `DsAppShell` |

## Rules for new screens
- Buttons, inputs, tabs, chips, toast, dialog: use PrimeNG (`p-button`, `pInputText`, `p-tabs`, `p-tag`, `p-toast`, `p-dialog`). Do not restyle them per page.
- Colours come from tokens/Tailwind utilities; never hard-code hex values.
- Minimum tap target 44px (`--ds-target-min`). State is never colour alone: pair it with a dot, icon or word.
- Project scope: light mode and English (LTR) only. No dark mode, no RTL/Arabic.

## Shared components
```html
<ds-status-pill tone="sre">With SRE</ds-status-pill>        <!-- neutral|info|success|warning|danger|sre -->
<ds-priority level="critical" [showSla]="true" />            <!-- critical|high|medium|low -->
<ds-sla-meter state="at-risk" label="1h 40m left" [progress]="75" />  <!-- ok|at-risk|breached|paused -->
<ds-callout tone="danger" icon="pi pi-times-circle" heading="Can't publish">step s5 can't be reached</ds-callout>
```

## Sidebar
```html
<ds-sidebar title="Digital Support" subtitle="Mobile & web banking" sectionLabel="Customer service"
            [items]="items" [activeId]="activeId()" [user]="user" (itemSelect)="go($event)" />
```
- `items: DsSidebarItem[]`: `{ id, label, icon: 'pi pi-home', badge?: { count, tone: 'open' | 'action' } }`. The parent supplies items per role; the component knows nothing about roles, routes or auth.
- Presentational: it emits `itemSelect`; the parent navigates and sets `activeId`.
- Rail is 264px; icons-only (84px) when `collapsed` is true (two-way: `[(collapsed)]`) and forced below 1180px.
- Layout contract: put it on the page background (`--ds-page`) with `--ds-rail-edge-gap` (16px) free on its right; the active tab extends into that gap and the content surface must be `--ds-rail-tab-bg`.

## Bottom nav
```html
<ds-bottom-nav [items]="items" [activeId]="activeId()" (itemSelect)="go($event)" />
```
- Phone-only navigation. Same item model as `ds-sidebar` (`DsSidebarItem`); use 3 to 5 items.
- Fixed to the bottom, floating with `--ds-bottom-nav-gap` (sides) and `--ds-bar-edge-y` (top), and clears the home indicator via `safe-area-inset-bottom`.
- The layout decides which one to show: bottom nav below `--ds-bottom-nav-breakpoint` (768px), sidebar from 768px (icons only until 1180px). Leave `--ds-bottom-nav-height` plus `--ds-bar-edge-y` twice as bottom padding in the scrolling content.

## App bar
```html
<ds-app-bar title="Digital Support" subtitle="Mobile & web banking" initials="MH"
            [notificationCount]="3" (notificationsClick)="openAlerts()" (avatarClick)="openProfile()" />
```
- Phone top bar: brand, notifications bell (badge hidden at 0, capped at 99+) and avatar. Sticky, floating with `--ds-bottom-nav-gap` (sides) and `--ds-bar-edge-y` (top).
- `showMenu` adds the optional menu button (`menuClick`); off by default because the bottom nav covers navigation.
- Pair it with `ds-bottom-nav` below 768px; both float, so leave top and bottom padding in the scrolling content.

## App shell
```html
<ds-app-shell [items]="items" [activeId]="activeId()" [user]="user" (itemSelect)="go($event)">
  <router-outlet />
</ds-app-shell>
```
- Use it **once**, in a layout route; pages render only their own content and never include a sidebar or bottom nav.
- From 768px it shows `ds-sidebar`; below that `ds-app-bar` plus `ds-bottom-nav` (items: `phoneItems`, or the first five `items`). The 768px breakpoint is a constant in `app-shell.ts`, kept in sync with `--ds-bottom-nav-breakpoint`.
- It sets the page surface (`--ds-rail-tab-bg`), the sticky sidebar and the content padding (room for the floating bars on phones).
- Presentational: the layout route supplies `items`, `user`, `activeId` (from the URL) and `notificationCount`, and handles `itemSelect`.
