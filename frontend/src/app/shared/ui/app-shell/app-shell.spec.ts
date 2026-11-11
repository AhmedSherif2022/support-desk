import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import type { DsSidebarItem } from '../sidebar/sidebar';
import { DsAppShell } from './app-shell';

@Component({
  imports: [DsAppShell],
  template: `
    <ds-app-shell [items]="items" [activeId]="active()" [user]="user" (itemSelect)="selected = $event.id">
      <p class="page">Page content</p>
    </ds-app-shell>
  `,
})
class Host {
  active = signal('home');
  selected = '';
  user = { name: 'Mona Hassan', role: 'Agent', initials: 'MH' };
  items: DsSidebarItem[] = [
    { id: 'home', label: 'Home', icon: 'pi pi-home' },
    { id: 'tickets', label: 'Tickets', icon: 'pi pi-ticket' },
    { id: 'tasks', label: 'Tasks', icon: 'pi pi-list-check' },
    { id: 'alerts', label: 'Alerts', icon: 'pi pi-bell' },
    { id: 'search', label: 'Search', icon: 'pi pi-search' },
    { id: 'more', label: 'More', icon: 'pi pi-ellipsis-h' },
  ];
}

function mockViewport(phone: boolean) {
  vi.stubGlobal('matchMedia', () => ({
    matches: phone,
    addEventListener: () => {},
    removeEventListener: () => {},
  }));
}

describe('DsAppShell', () => {
  afterEach(() => vi.unstubAllGlobals());

  async function setup(phone: boolean) {
    mockViewport(phone);
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('shows the sidebar on wide screens and projects the page', async () => {
    const { el } = await setup(false);
    expect(el.querySelector('ds-sidebar')).not.toBeNull();
    expect(el.querySelector('ds-app-bar')).toBeNull();
    expect(el.querySelector('ds-bottom-nav')).toBeNull();
    expect(el.querySelector('main .page')?.textContent).toContain('Page content');
  });

  it('shows the app bar and a bottom nav of at most five items on phones', async () => {
    const { fixture, el } = await setup(true);
    expect(el.querySelector('ds-sidebar')).toBeNull();
    expect(el.querySelector('ds-app-bar')).not.toBeNull();
    expect(el.querySelectorAll('ds-bottom-nav .tab').length).toBe(5);
    expect(el.querySelector('main .page')).not.toBeNull();
    el.querySelectorAll<HTMLButtonElement>('ds-bottom-nav .tab')[1].click();
    expect(fixture.componentInstance.selected).toBe('tickets');
  });
});
