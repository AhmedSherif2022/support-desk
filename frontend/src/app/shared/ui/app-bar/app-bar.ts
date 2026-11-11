import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Phone top bar: a floating blue bar with the brand, a notifications bell and the user avatar.
 * Presentational only: the parent owns the count, the user and what each action does.
 *
 * <ds-app-bar title="Digital Support" subtitle="Mobile & web banking" initials="MH"
 *             [notificationCount]="3" (notificationsClick)="openAlerts()" (avatarClick)="openProfile()" />
 *
 * The menu button is optional (`showMenu`); the phone layout normally relies on `ds-bottom-nav` instead.
 */
@Component({
  selector: 'ds-app-bar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './app-bar.css',
  template: `
    <header class="bar">
      @if (showMenu()) {
        <button type="button" class="tile" aria-label="Open menu" (click)="menuClick.emit()">
          <i class="pi pi-bars" aria-hidden="true"></i>
        </button>
      }
      <span class="logo" aria-hidden="true"><i [class]="brandIcon()"></i></span>
      <span class="text">
        <span class="title">{{ title() }}</span>
        @if (subtitle()) {
          <span class="sub">{{ subtitle() }}</span>
        }
      </span>
      <button type="button" class="tile bell" [attr.aria-label]="bellLabel()" (click)="notificationsClick.emit()">
        <i class="pi pi-bell" aria-hidden="true"></i>
        @if (notificationCount() > 0) {
          <span class="badge" aria-hidden="true">{{ count() }}</span>
        }
      </button>
      <button type="button" class="avatar" [attr.aria-label]="'Account, ' + initials()" (click)="avatarClick.emit()">
        {{ initials() }}
      </button>
    </header>
  `,
})
export class DsAppBar {
  readonly title = input('Digital Support');
  readonly subtitle = input('');
  /** primeicons class for the logo tile. */
  readonly brandIcon = input('pi pi-headphones');
  /** Unread notifications; the badge is hidden at 0. */
  readonly notificationCount = input(0);
  /** Shown in the avatar, e.g. "MH". */
  readonly initials = input.required<string>();
  readonly showMenu = input(false);
  readonly menuClick = output<void>();
  readonly notificationsClick = output<void>();
  readonly avatarClick = output<void>();

  protected count(): string {
    const n = this.notificationCount();
    return n > 99 ? '99+' : String(n);
  }

  protected bellLabel(): string {
    const n = this.notificationCount();
    return n > 0 ? `Notifications, ${n}` : 'Notifications';
  }
}
