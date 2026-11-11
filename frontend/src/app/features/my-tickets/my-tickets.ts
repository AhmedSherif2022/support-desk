import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { DsPriorityBadge, DsSlaState, DsStatusPill } from '../../shared/ui';
import { TICKET_STATUS, TicketService } from '../../core/theme/auth/ticket.service';
import { Ticket } from '../customer-search/customer.models';

type StatusFilter = 'all' | 'open' | 'done';
const PAGE_SIZE = 8;

@Component({
  selector: 'app-my-tickets',
  imports: [DatePipe, RouterLink, DsPriorityBadge, DsStatusPill],
  templateUrl: './my-tickets.html',
})
export class MyTickets {
  protected tickets = inject(TicketService);
  private router = inject(Router);

  /** Optional ?incident=INC-301 from the Home banner (withComponentInputBinding). */
  incident = input<string>();

  status = signal<StatusFilter>('all');
  query = signal('');
  page = signal(1);

  statusFilters: { id: StatusFilter; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'open', label: 'Open' },
    { id: 'done', label: 'Resolved & closed' },
  ];

  filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.tickets.mine().filter(t => {
      const done = this.tickets.isDone(t);
      if (this.status() === 'open' && done) return false;
      if (this.status() === 'done' && !done) return false;
      if (this.incident() && t.incidentId !== this.incident()) return false;
      if (!q) return true;
      const name = this.tickets.customerOf(t)?.name.toLowerCase() ?? '';
      return (
        t.id.includes(q.replace('#', '')) ||
        t.cif.includes(q) ||
        (t.adoItem ?? '').toLowerCase().includes(q) ||
        name.includes(q)
      );
    });
  });

  pages = computed(() => Math.max(1, Math.ceil(this.filtered().length / PAGE_SIZE)));
  rows = computed(() => this.filtered().slice((this.page() - 1) * PAGE_SIZE, this.page() * PAGE_SIZE));

  statusInfo = TICKET_STATUS;

  constructor() {
    // A new filter or search starts again from page 1.
    effect(() => {
      this.status();
      this.query();
      this.incident();
      this.page.set(1);
    });
  }

  clearIncident() {
    this.router.navigate(['/tickets']);
  }

  customer(t: Ticket) {
    return this.tickets.customerOf(t);
  }

  action(t: Ticket): string {
    if (t.status === 'new') return 'Start';
    if (t.status === 'fixed') return 'Confirm fix';
    if (t.status === 'with-sre' && this.tickets.sla(t).state === 'breached') return 'Ask SRE for update';
    return 'Open';
  }

  slaClass(state: DsSlaState) {
    return {
      ok: 'bg-success-tint text-success',
      'at-risk': 'bg-warning-tint text-warning',
      breached: 'bg-danger-tint text-danger',
      paused: 'bg-page text-ink-3',
    }[state];
  }
}
