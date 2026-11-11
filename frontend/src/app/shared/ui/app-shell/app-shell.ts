import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { DsAppBar } from '../app-bar/app-bar';
import { DsBottomNav } from '../bottom-nav/bottom-nav';
import { DsSidebar, DsSidebarItem, DsSidebarUser } from '../sidebar/sidebar';

/** Keep in sync with `--ds-bottom-nav-breakpoint` (a media query cannot read a custom property). */
const PHONE_QUERY = '(max-width: 767px)';
const MAX_PHONE_ITEMS = 5;

/**
 * App frame: picks the navigation for the screen size and projects the page content next to it.
 * Wide screens get `ds-sidebar` (icons only below 1180px); phones get `ds-app-bar` and `ds-bottom-nav`.
 * Presentational only: the parent owns items, the user, the active id and what each action does.
 *
 * <ds-app-shell [items]="items" [activeId]="active()" [user]="user" (itemSelect)="go($event)">
 *   <router-outlet />
 * </ds-app-shell>
 *
 * Use it once, in a layout route, so pages only render their own content.
 */
@Component({
  selector: 'ds-app-shell',
  imports: [DsSidebar, DsAppBar, DsBottomNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './app-shell.css',
  host: { '[class.phone]': 'phone()' },
  template: `
    @if (phone()) {
      <ds-app-bar
        [title]="title()"
        [subtitle]="subtitle()"
        [brandIcon]="brandIcon()"
        [notificationCount]="notificationCount()"
        [initials]="user()?.initials ?? ''"
        (notificationsClick)="notificationsClick.emit()"
        (avatarClick)="avatarClick.emit()"
      />
    } @else {
      <ds-sidebar
        [title]="title()"
        [subtitle]="subtitle()"
        [sectionLabel]="sectionLabel()"
        [brandIcon]="brandIcon()"
        [items]="items()"
        [activeId]="activeId()"
        [user]="user()"
        (itemSelect)="itemSelect.emit($event)"
      />
    }

    <main class="content ">
      <ng-content />
    </main>

    @if (phone()) {
      <ds-bottom-nav [items]="navItems()" [activeId]="activeId()" (itemSelect)="itemSelect.emit($event)" />
    }
  `,
})
export class DsAppShell {
  readonly title = input('Digital Support');
  readonly subtitle = input('');
  readonly sectionLabel = input('');
  readonly brandIcon = input('pi pi-headphones');
  /** Sidebar items; also the bottom nav items unless `phoneItems` is given. */
  readonly items = input.required<DsSidebarItem[]>();
  /** Bottom nav items (3 to 5); defaults to the first five `items`. */
  readonly phoneItems = input<DsSidebarItem[] | null>(null);
  readonly activeId = input<string | null>(null);
  readonly user = input<DsSidebarUser | null>(null);
  /** Unread notifications shown on the phone app bar bell. */
  readonly notificationCount = input(0);
  readonly itemSelect = output<DsSidebarItem>();
  readonly notificationsClick = output<void>();
  readonly avatarClick = output<void>();

  protected readonly phone = signal(false);
  protected readonly navItems = computed(() => this.phoneItems() ?? this.items().slice(0, MAX_PHONE_ITEMS));

  constructor() {
    if (typeof matchMedia === 'function') {
      const mql = matchMedia(PHONE_QUERY);
      const update = () => this.phone.set(mql.matches);
      update();
      mql.addEventListener('change', update);
      inject(DestroyRef).onDestroy(() => mql.removeEventListener('change', update));
    }
  }
}
