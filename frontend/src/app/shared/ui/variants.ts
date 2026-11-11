/** Shared variant types for the design-system components. */
export type DsTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'sre';
export type DsPriority = 'critical' | 'high' | 'medium' | 'low';
export type DsSlaState = 'ok' | 'at-risk' | 'breached' | 'paused';

/** Priority → number of lit signal bars (of 4) and SLA hours (from the PDF). */
export const PRIORITY_META: Record<DsPriority, { label: string; bars: number; slaHours: number; tone: DsTone }> = {
  critical: { label: 'Critical', bars: 4, slaHours: 2, tone: 'danger' },
  high: { label: 'High', bars: 3, slaHours: 8, tone: 'danger' },
  medium: { label: 'Medium', bars: 2, slaHours: 24, tone: 'warning' },
  low: { label: 'Low', bars: 1, slaHours: 72, tone: 'neutral' },
};

/** Tone → CSS custom property pair, consumed by every component. */
export const TONE_VARS: Record<DsTone, { fg: string; bg: string }> = {
  neutral: { fg: 'var(--ds-ink-2)', bg: 'var(--ds-line)' },
  info: { fg: 'var(--ds-info-fg)', bg: 'var(--ds-info-bg)' },
  success: { fg: 'var(--ds-success-fg)', bg: 'var(--ds-success-bg)' },
  warning: { fg: 'var(--ds-warning-fg)', bg: 'var(--ds-warning-bg)' },
  danger: { fg: 'var(--ds-danger-fg)', bg: 'var(--ds-danger-bg)' },
  sre: { fg: 'var(--ds-sre-fg)', bg: 'var(--ds-sre-bg)' },
};

