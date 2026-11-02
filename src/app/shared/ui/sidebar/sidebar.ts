import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  model,
  output,
  signal,
} from '@angular/core';

/** Count badge: `open` = translucent (open work), `action` = red with white ring (needs action). */
export type DsSidebarBadgeTone = 'open' | 'action';

export interface DsSidebarItem {
  /** Unique id; matched against `activeId`. */
  id: string;
  label: string;
  /** primeicons class, e.g. `pi pi-home`. */
  icon: string;
  badge?: { count: number; tone: DsSidebarBadgeTone };
}

export interface DsSidebarUser {
  name: string;
  role: string;
  /** Shown in the avatar, e.g. "MH". */
  initials: string;
}

const TABLET_QUERY = '(max-width: 1179px)';

/**
 * Navigation rail: solid blue, the active item is a cut-out tab that flows into the content.
 * Presentational only: the parent owns the items, the active id and navigation.
 *
 * <ds-sidebar title="Digital Support" subtitle="Mobile & web banking" sectionLabel="Customer service"
 *             [items]="items" [activeId]="'home'" [user]="user" (itemSelect)="go($event)" />
 *
 * Place it inside a container with the page background (--ds-page) and `--ds-rail-edge-gap` of room on its
 * right: the active tab extends into that gap. Below 1180px it collapses to icons automatically.
 */
@Component({
  selector: 'ds-sidebar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './sidebar.css',
  host: { '[class.compact]': 'compact()' },
  template: `
    <nav class="rail fixed" [attr.aria-label]="title()">
      <header class="brand">
        <span class="logo" aria-hidden="true"><i [class]="brandIcon()"></i></span>
        @if (!compact()) {
          <span class="brand-text">
            <span class="brand-title">{{ title() }}</span>
            @if (subtitle()) {
              <span class="brand-sub">{{ subtitle() }}</span>
            }
          </span>
        }
        @if (!tablet()) {
          <button
            type="button"
            class="rail-toggle"
            [attr.aria-label]="collapsed() ? 'Expand sidebar' : 'Collapse sidebar'"
            [attr.aria-expanded]="!collapsed()"
            (click)="collapsed.set(!collapsed())"
          >
            <i class="pi pi-sidebar" aria-hidden="true"></i>
          </button>
        }
      </header>

      @if (sectionLabel() && !compact()) {
        <p class="section">{{ sectionLabel() }}</p>
      }

      <ul class="items">
        @for (item of items(); track item.id) {
          <li class="item" [class.active]="item.id === activeId()">
            <button
              type="button"
              class="link"
              [attr.aria-current]="item.id === activeId() ? 'page' : null"
              [attr.aria-label]="ariaLabel(item)"
              [attr.title]="compact() ? item.label : null"
              (click)="itemSelect.emit(item)"
            >
              <span class="icon" aria-hidden="true">
                <i [class]="item.icon"></i>
                @if (item.badge && compact()) {
                  <span class="badge corner" [class.action]="item.badge.tone === 'action'">{{
                    count(item.badge.count)
                  }}</span>
                }
              </span>
              @if (!compact()) {
                <span class="label">{{ item.label }}</span>
                @if (item.badge) {
                  <span class="badge" [class.action]="item.badge.tone === 'action'" aria-hidden="true">{{
                    count(item.badge.count)
                  }}</span>
                }
              }
            </button>
          </li>
        }
      </ul>

      @if (user(); as u) {
        <footer class="user">
          <span class="avatar" aria-hidden="true">{{ u.initials }}</span>
          @if (!compact()) {
            <span class="user-text">
              <span class="user-name">{{ u.name }}</span>
              <span class="user-role">{{ u.role }}</span>
            </span>
          }
        </footer>
      }
    </nav>
  `,
})
export class DsSidebar {
  readonly title = input('Digital Support');
  readonly subtitle = input('');
  readonly sectionLabel = input('');
  /** primeicons class for the logo tile. */
  readonly brandIcon = input('pi pi-headphones');
  readonly items = input.required<DsSidebarItem[]>();
  readonly activeId = input<string | null>(null);
  readonly user = input<DsSidebarUser | null>(null);
  /** Compact (icons-only) rail; two-way bindable. Below 1180px it is forced on. */
  readonly collapsed = model(false);
  readonly itemSelect = output<DsSidebarItem>();

  protected readonly tablet = signal(false);
  protected readonly compact = computed(() => this.collapsed() || this.tablet());

  constructor() {
    if (typeof matchMedia === 'function') {
      const mql = matchMedia(TABLET_QUERY);
      const update = () => this.tablet.set(mql.matches);
      update();
      mql.addEventListener('change', update);
      inject(DestroyRef).onDestroy(() => mql.removeEventListener('change', update));
    }
  }

  protected count(n: number): string {
    return n > 99 ? '99+' : String(n);
  }

  protected ariaLabel(item: DsSidebarItem): string {
    return item.badge ? `${item.label}, ${item.badge.count}` : item.label;
  }
}
