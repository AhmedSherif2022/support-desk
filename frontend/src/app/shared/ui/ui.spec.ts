import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DsCallout, DsPriorityBadge, DsSlaMeter, DsStatusPill } from './index';

@Component({
  imports: [DsStatusPill, DsPriorityBadge, DsSlaMeter, DsCallout],
  template: `
    <ds-status-pill tone="sre">With SRE</ds-status-pill>
    <ds-priority level="critical" [showSla]="true" />
    <ds-sla-meter state="breached" label="breached 1h 30m" [progress]="100" />
    <ds-callout tone="danger" heading="Can't publish">step s5 can't be reached</ds-callout>
  `,
})
class Host {}

describe('design-system UI', () => {
  it('renders all shared components', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('ds-status-pill')?.textContent).toContain('With SRE');
    expect(el.querySelector('ds-priority')?.textContent).toContain('Critical · 2h');
    expect(el.querySelectorAll('ds-priority span.w-\\[3px\\]').length).toBe(4);
    expect(el.querySelector('[role="progressbar"]')?.getAttribute('aria-valuenow')).toBe('100');
    expect(el.querySelector('ds-callout')?.getAttribute('role')).toBe('alert');
  });
});
