import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DsSidebar, DsSidebarItem } from './sidebar';

@Component({
  imports: [DsSidebar],
  template: `
    <ds-sidebar
      title="Digital Support"
      subtitle="Mobile & web banking"
      sectionLabel="Customer service"
      [items]="items"
      [activeId]="active()"
      [user]="{ name: 'Mona Hassan', role: 'Customer Service Agent', initials: 'MH' }"
      (itemSelect)="selected = $event.id"
    />
  `,
})
class Host {
  active = signal('home');
  selected = '';
  items: DsSidebarItem[] = [
    { id: 'home', label: 'Home', icon: 'pi pi-home' },
    { id: 'tickets', label: 'My tickets', icon: 'pi pi-ticket', badge: { count: 8, tone: 'open' } },
    { id: 'notif', label: 'Notifications', icon: 'pi pi-bell', badge: { count: 120, tone: 'action' } },
  ];
}

describe('DsSidebar', () => {
  async function setup() {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('renders items, marks the active one and exposes accessible names', async () => {
    const { el } = await setup();
    const links = el.querySelectorAll<HTMLButtonElement>('.link');
    expect(links.length).toBe(3);
    expect(links[0].getAttribute('aria-current')).toBe('page');
    expect(links[1].getAttribute('aria-current')).toBeNull();
    expect(links[1].getAttribute('aria-label')).toBe('My tickets, 8');
    expect(el.querySelector('.badge.action')?.textContent?.trim()).toBe('99+');
    expect(el.querySelector('.user-name')?.textContent).toContain('Mona Hassan');
  });

  it('emits the selected item', async () => {
    const { fixture, el } = await setup();
    el.querySelectorAll<HTMLButtonElement>('.link')[1].click();
    expect(fixture.componentInstance.selected).toBe('tickets');
  });

  it('collapses to icons only', async () => {
    const { fixture, el } = await setup();
    el.querySelector<HTMLButtonElement>('.rail-toggle')!.click();
    await fixture.whenStable();
    expect(el.querySelector('ds-sidebar')?.classList.contains('compact')).toBe(true);
    expect(el.querySelector('.label')).toBeNull();
    expect(el.querySelectorAll('.badge.corner').length).toBe(2);
    expect(el.querySelector('.link')?.getAttribute('title')).toBe('Home');
  });
});
