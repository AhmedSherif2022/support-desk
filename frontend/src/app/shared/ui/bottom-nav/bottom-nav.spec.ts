import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import type { DsSidebarItem } from '../sidebar/sidebar';
import { DsBottomNav } from './bottom-nav';

@Component({
  imports: [DsBottomNav],
  template: `<ds-bottom-nav [items]="items" [activeId]="active()" (itemSelect)="selected = $event.id" />`,
})
class Host {
  active = signal('home');
  selected = '';
  items: DsSidebarItem[] = [
    { id: 'home', label: 'Home', icon: 'pi pi-home' },
    { id: 'search', label: 'Search', icon: 'pi pi-search' },
    { id: 'tickets', label: 'Tickets', icon: 'pi pi-ticket', badge: { count: 8, tone: 'open' } },
    { id: 'alerts', label: 'Alerts', icon: 'pi pi-bell', badge: { count: 120, tone: 'action' } },
  ];
}

describe('DsBottomNav', () => {
  async function setup() {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('renders items, marks the active one and exposes accessible names', async () => {
    const { el } = await setup();
    const tabs = el.querySelectorAll<HTMLButtonElement>('.tab');
    expect(tabs.length).toBe(4);
    expect(tabs[0].getAttribute('aria-current')).toBe('page');
    expect(tabs[0].classList.contains('active')).toBe(true);
    expect(tabs[1].getAttribute('aria-current')).toBeNull();
    expect(tabs[2].getAttribute('aria-label')).toBe('Tickets, 8');
    expect(el.querySelector('.badge.action')?.textContent?.trim()).toBe('99+');
  });

  it('emits the selected item and follows activeId', async () => {
    const { fixture, el } = await setup();
    el.querySelectorAll<HTMLButtonElement>('.tab')[1].click();
    expect(fixture.componentInstance.selected).toBe('search');
    fixture.componentInstance.active.set('alerts');
    await fixture.whenStable();
    expect(el.querySelectorAll('.tab')[3].getAttribute('aria-current')).toBe('page');
  });
});
