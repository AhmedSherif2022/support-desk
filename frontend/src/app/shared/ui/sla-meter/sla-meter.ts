import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DsSlaState } from '../variants';

const SLA = {
  ok: { fg: 'var(--ds-success-fg)', bg: 'var(--ds-success-bg)', bar: 'var(--ds-success-solid)' },
  'at-risk': { fg: 'var(--ds-warning-fg)', bg: 'var(--ds-warning-bg)', bar: 'var(--ds-warning-solid)' },
  breached: { fg: 'var(--ds-danger-fg)', bg: 'var(--ds-danger-bg)', bar: 'var(--ds-danger-solid)' },
  paused: { fg: 'var(--ds-ink-2)', bg: 'var(--ds-line)', bar: '#a6afbf' },
} as const;

/** One number + meter: <ds-sla-meter state="breached" label="breached 1h 30m" [progress]="100" />. */
@Component({
  selector: 'ds-sla-meter',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex flex-col gap-1.5 min-w-28' },
  template: `
    <span
      class="inline-flex w-fit rounded-full px-2.5 py-1 text-xs font-bold whitespace-nowrap"
      [style.color]="c().fg"
      [style.background]="c().bg"
      >{{ label() }}</span
    >
    <span
      class="block h-1.5 overflow-hidden rounded-full bg-[var(--ds-line)]"
      role="progressbar"
      [attr.aria-valuenow]="pct()"
      aria-valuemin="0"
      aria-valuemax="100"
      [attr.aria-label]="label()"
    >
      <span class="block h-full rounded-full" [style.width.%]="pct()" [style.background]="c().bar"></span>
    </span>
  `,
})
export class DsSlaMeter {
  readonly state = input<DsSlaState>('ok');
  /** The one number shown, e.g. "6h 12m left". */
  readonly label = input.required<string>();
  /** Share of the SLA window used, 0–100. */
  readonly progress = input(0);
  protected readonly c = computed(() => SLA[this.state()]);
  protected readonly pct = computed(() => Math.min(100, Math.max(0, this.progress())));
}
