import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DsAppBar } from './app-bar';

@Component({
  imports: [DsAppBar],
  template: `
    <ds-app-bar
      title="Digital Support"
      subtitle="Mobile & web banking"
      initials="MH"
      [notificationCount]="count()"
      [showMenu]="menu()"
      (notificationsClick)="events.push('bell')"
      (avatarClick)="events.push('avatar')"
      (menuClick)="events.push('menu')"
    />
  `,
})
class Host {
  count = signal(3);
  menu = signal(false);
  events: string[] = [];
}

describe('DsAppBar', () => {
  async function setup() {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('shows the brand, the bell count and the avatar with accessible names', async () => {
    const { el } = await setup();
    expect(el.querySelector('.title')?.textContent).toContain('Digital Support');
    expect(el.querySelector('.sub')?.textContent).toContain('Mobile & web banking');
    expect(el.querySelector('.bell')?.getAttribute('aria-label')).toBe('Notifications, 3');
    expect(el.querySelector('.badge')?.textContent?.trim()).toBe('3');
    expect(el.querySelector('.avatar')?.getAttribute('aria-label')).toBe('Account, MH');
    expect(el.querySelector('[aria-label="Open menu"]')).toBeNull();
  });

  it('hides the badge at 0, caps it at 99+ and shows the menu on demand', async () => {
    const { fixture, el } = await setup();
    fixture.componentInstance.count.set(0);
    fixture.componentInstance.menu.set(true);
    await fixture.whenStable();
    expect(el.querySelector('.badge')).toBeNull();
    expect(el.querySelector('.bell')?.getAttribute('aria-label')).toBe('Notifications');
    expect(el.querySelector('[aria-label="Open menu"]')).not.toBeNull();
    fixture.componentInstance.count.set(120);
    await fixture.whenStable();
    expect(el.querySelector('.badge')?.textContent?.trim()).toBe('99+');
  });

  it('emits actions', async () => {
    const { fixture, el } = await setup();
    el.querySelector<HTMLButtonElement>('.bell')!.click();
    el.querySelector<HTMLButtonElement>('.avatar')!.click();
    expect(fixture.componentInstance.events).toEqual(['bell', 'avatar']);
  });
});
