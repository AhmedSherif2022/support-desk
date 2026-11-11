import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DsPriority, PRIORITY_META, TONE_VARS } from '../variants';

/** Signal bars + word: <ds-priority level="critical" /> or with SLA hours: [showSla]="true". */
@Component({
  selector: 'ds-priority',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'inline-flex items-center gap-1.5 text-xs font-bold whitespace-nowrap',
    '[style.color]': 'color()',
  },
  template: `
    <span class="inline-flex items-end gap-0.5" aria-hidden="true">
      @for (n of [1, 2, 3, 4]; track n) {
        <span
          class="w-[3px] rounded-[1px] bg-current"
          [style.height.px]="4 + n * 2.5"
          [style.opacity]="n <= meta().bars ? 1 : 0.22"
        ></span>
      }
    </span>
    <span>{{ meta().label }}@if (showSla()) { · {{ meta().slaHours }}h }</span>
  `,
})
export class DsPriorityBadge {
  readonly level = input.required<DsPriority>();
  readonly showSla = input(false);
  protected readonly meta = computed(() => PRIORITY_META[this.level()]);
  protected readonly color = computed(() =>
    this.level() === 'medium' ? 'var(--ds-warning-fg)' : TONE_VARS[this.meta().tone].fg,
  );
}
