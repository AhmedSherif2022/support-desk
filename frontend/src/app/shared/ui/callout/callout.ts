import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DsTone, TONE_VARS } from '../variants';

/**
 * Alert / banner / "Say to the customer" block.
 * <ds-callout tone="danger" icon="pi pi-times-circle" heading="Can't publish">…</ds-callout>
 * Project an action: <button dsCalloutActions>…</button>.
 */
@Component({
  selector: 'ds-callout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'flex items-center gap-3 rounded-inset border p-4',
    '[style.background]': 'vars().bg',
    '[style.border-color]': 'border()',
    '[attr.role]': "tone() === 'danger' ? 'alert' : 'status'",
  },
  template: `
    @if (icon()) {
      <span
        class="grid size-9 shrink-0 place-items-center rounded-control bg-white"
        [style.color]="vars().fg"
        aria-hidden="true"
      >
        <i [class]="icon()"></i>
      </span>
    }
    <div class="min-w-0 flex-1">
      @if (eyebrow()) {
        <p class="ds-eyebrow" [style.color]="vars().fg">{{ eyebrow() }}</p>
      }
      @if (heading()) {
        <p class="font-bold" [style.color]="'var(--ds-ink)'">{{ heading() }}</p>
      }
      <div class="text-xs" [style.color]="vars().fg"><ng-content /></div>
    </div>
    <ng-content select="[dsCalloutActions]" />
  `,
})
export class DsCallout {
  readonly tone = input<DsTone>('info');
  readonly icon = input<string>('');
  readonly heading = input<string>('');
  readonly eyebrow = input<string>('');
  protected readonly vars = computed(() => TONE_VARS[this.tone()]);
  protected readonly border = computed(() => `color-mix(in srgb, ${this.vars().fg} 18%, transparent)`);
}
