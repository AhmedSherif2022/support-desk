import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DsTone, TONE_VARS } from '../variants';

/** Dotted status pill: <ds-status-pill tone="sre">With SRE</ds-status-pill>. The dot + text keep colour from being the only signal. */
@Component({
  selector: 'ds-status-pill',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold whitespace-nowrap',
    '[style.color]': 'vars().fg',
    '[style.background]': 'vars().bg',
  },
  template: `
    @if (dot()) {
      <span class="size-1.5 rounded-full bg-current" aria-hidden="true"></span>
    }
    <ng-content />
  `,
})
export class DsStatusPill {
  readonly tone = input<DsTone>('neutral');
  readonly dot = input(true);
  protected readonly vars = computed(() => TONE_VARS[this.tone()]);
}
