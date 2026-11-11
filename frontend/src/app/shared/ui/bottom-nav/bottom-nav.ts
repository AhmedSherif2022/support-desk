import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import type { DsSidebarItem } from '../sidebar/sidebar';

/**
 * Phone navigation: a floating blue bar with the active item as a light pill.
 * Presentational only: the parent owns the items, the active id and navigation. Shares the item model with
 * `ds-sidebar`; the layout shows one or the other (bottom nav below `--ds-bottom-nav-breakpoint`).
 *
 * <ds-bottom-nav [items]="items" [activeId]="'home'" (itemSelect)="go($event)" />
 *
 * Use 3 to 5 items. Fixed to the bottom of the viewport and keeps clear of the home indicator.
 */
@Component({
  selector: 'ds-bottom-nav',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './bottom-nav.css',
  template: `
    <nav class="bar" [attr.aria-label]="label()">
      @for (item of items(); track item.id) {
        <button
          type="button"
          class="tab"
          [class.active]="item.id === activeId()"
          [attr.aria-current]="item.id === activeId() ? 'page' : null"
          [attr.aria-label]="ariaLabel(item)"
          (click)="itemSelect.emit(item)"
        >
          <span class="icon" aria-hidden="true">
            <i [class]="item.icon"></i>
            @if (item.badge) {
              <span class="badge" [class.action]="item.badge.tone === 'action'">{{ count(item.badge.count) }}</span>
            }
          </span>
          <span class="label">{{ item.label }}</span>
        </button>
      }
    </nav>
  `,
})
export class DsBottomNav {
  readonly items = input.required<DsSidebarItem[]>();
  readonly activeId = input<string | null>(null);
  /** Accessible name of the navigation landmark. */
  readonly label = input('Main navigation');
  readonly itemSelect = output<DsSidebarItem>();

  protected count(n: number): string {
    return n > 99 ? '99+' : String(n);
  }

  protected ariaLabel(item: DsSidebarItem): string {
    return item.badge ? `${item.label}, ${item.badge.count}` : item.label;
  }
}
