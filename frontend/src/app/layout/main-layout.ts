import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { AuthService } from '../core/theme/auth/auth.service';
import { DsAppShell, DsSidebarItem, DsSidebarUser } from '../shared/ui';
import { NAV_BY_ROLE } from './nav-items';

/**
 * The frame of every signed-in page: one `ds-app-shell` with the page projected inside it.
 * It is the only place that knows the user and the navigation; pages render their own content only.
 * Every route except `/signin` is a child of this layout (see app.routes.ts).
 */
@Component({
  selector: 'app-main-layout',
  imports: [DsAppShell, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ds-app-shell
      [subtitle]="nav()?.subtitle ?? ''"
      [sectionLabel]="nav()?.sectionLabel ?? ''"
      [items]="nav()?.items ?? []"
      [phoneItems]="nav()?.phoneItems ?? null"
      [activeId]="activeId()"
      [user]="user()"
      [notificationCount]="3"
      (itemSelect)="go($event)"
      (notificationsClick)="go({ id: 'alerts' })"
    >
      <router-outlet />
    </ds-app-shell>
  `,
})
export class MainLayout {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly nav = computed(() => {
    const role = this.auth.user()?.role;
    return role ? NAV_BY_ROLE[role] : null;
  });

  protected readonly user = computed<DsSidebarUser | null>(() => {
    const u = this.auth.user();
    if (!u) return null;
    const initials = u.name
      .split(' ')
      .map(part => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
    return { name: u.name, role: NAV_BY_ROLE[u.role].userRole, initials };
  });

  /** First URL segment, e.g. `/tickets/42` gives `tickets`. */
  protected readonly activeId = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      startWith(null),
      map(() => this.router.url.split(/[/?#]/)[1] ?? null),
    ),
    { initialValue: null },
  );

  protected go(item: Pick<DsSidebarItem, 'id'>) {
    this.router.navigate(['/', item.id]);
  }
}
