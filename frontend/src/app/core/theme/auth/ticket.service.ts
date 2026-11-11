import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { DsSlaState, DsTone, PRIORITY_META } from '../../../shared/ui';
import { AuthService } from './auth.service';
import { CustomerService } from './customer.service';
import { NewTicketDraft, Ticket, TicketStatus } from '../../../features/customer-search/customer.models';
import { TICKETS } from '../../../features/customer-search/customers.mock';

/** Label + colour for every ticket status. */
export const TICKET_STATUS: Record<TicketStatus, { label: string; tone: DsTone }> = {
  new: { label: 'New', tone: 'neutral' },
  open: { label: 'Open', tone: 'info' },
  'with-sre': { label: 'With SRE', tone: 'sre' },
  fixed: { label: 'Fixed by SRE · confirm', tone: 'warning' },
  resolved: { label: 'Resolved', tone: 'success' },
  closed: { label: 'Closed', tone: 'neutral' },
};

export interface SlaInfo {
  state: DsSlaState;
  /** The one number shown, e.g. "6h 12m left". */
  label: string;
  /** Share of the SLA window used, 0–100. */
  progress: number;
  hours: number;
}

const HOUR = 60 * 60_000;
const ASK_SRE_EVERY = HOUR; // "Ask SRE for update" is limited to once an hour

/** "1h 30m" */
function duration(ms: number): string {
  const minutes = Math.max(0, Math.round(ms / 60_000));
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

/**
 * The agent's tickets and everything they can do on them.
 * TODO: replace the mock data with HttpClient calls when the tickets API is ready.
 */
@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly auth = inject(AuthService);
  private readonly customers = inject(CustomerService);
  private readonly tickets = signal<Ticket[]>(TICKETS);

  /** Ticks every minute so SLA countdowns stay fresh. */
  readonly now = signal(Date.now());

  /** Tickets owned by the signed-in agent, newest first. */
  readonly mine = computed(() => {
    const me = this.auth.user()?.username;
    return this.tickets()
      .filter(t => t.assignee === me)
      .sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
  });

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 60_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  byId(id: string): Ticket | undefined {
    return this.tickets().find(t => t.id === id);
  }

  forCustomer(cif: string): Ticket[] {
    return this.tickets()
      .filter(t => t.cif === cif)
      .sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
  }

  customerOf(t: Ticket) {
    return this.customers.getByCif(t.cif);
  }

  isDone(t: Ticket): boolean {
    return t.status === 'resolved' || t.status === 'closed';
  }

  /**
   * SLA = priority hours from the moment the ticket was created.
   * It stops when the agent resolves, and is paused while the ticket is part of an incident.
   */
  sla(t: Ticket): SlaInfo {
    const hours = PRIORITY_META[t.priority].slaHours;
    if (this.isDone(t)) return { state: 'ok', label: 'Stopped · resolved', progress: 100, hours };
    if (t.incidentId) return { state: 'paused', label: `Part of ${t.incidentId}`, progress: 0, hours };

    const total = hours * HOUR;
    const used = this.now() - t.createdAt.getTime();
    const left = total - used;
    const progress = Math.round((used / total) * 100);

    if (left <= 0) return { state: 'breached', label: `SLA breached ${duration(-left)}`, progress: 100, hours };
    if (left < total * 0.25) return { state: 'at-risk', label: `${duration(left)} left`, progress, hours };
    return { state: 'ok', label: `${duration(left)} left`, progress, hours };
  }

  /** Breached first, then at risk, then by time left; paused ones last. Older first on ties. */
  riskRank(t: Ticket): number {
    const order: Record<DsSlaState, number> = { breached: 0, 'at-risk': 1, ok: 2, paused: 3 };
    return order[this.sla(t).state];
  }

  canAskSre(t: Ticket): boolean {
    return t.status === 'with-sre' && (!t.lastSreAskAt || this.now() - t.lastSreAskAt.getTime() >= ASK_SRE_EVERY);
  }

  nextAskAt(t: Ticket): Date | undefined {
    return t.lastSreAskAt ? new Date(t.lastSreAskAt.getTime() + ASK_SRE_EVERY) : undefined;
  }

  // ---------- actions (each one also writes the timeline) ----------
  create(draft: NewTicketDraft): Ticket {
    const nextId = Math.max(...this.tickets().map(t => Number(t.id))) + 1;
    const ticket: Ticket = {
      id: String(nextId),
      cif: draft.cif,
      title: draft.issue,
      category: draft.category,
      status: 'open',
      priority: draft.priority,
      platform: draft.platform,
      createdAt: new Date(),
      assignee: this.auth.user()?.username ?? '',
      origin: 'agent',
      report: draft.report,
      incidentId: this.customers.incidentFor(draft.category, draft.platform)?.id,
      notes: [],
      timeline: [{ at: new Date(), actor: this.me(), text: 'Ticket created' }],
    };
    this.tickets.update(list => [ticket, ...list]);
    return ticket;
  }

  start(id: string) {
    this.change(id, { status: 'open' }, 'Started working on the ticket');
  }

  escalate(id: string) {
    const ado = `ADO-${48122 + this.tickets().filter(t => t.adoItem).length}`;
    this.change(id, { status: 'with-sre', adoItem: ado, sreOwner: 'Omar Nabil' }, `Escalated to SRE as ${ado}`);
  }

  askSre(id: string) {
    this.change(id, { lastSreAskAt: new Date() }, 'Asked SRE for an update · Team Leader notified');
  }

  stillNotWorking(id: string) {
    this.change(id, { status: 'with-sre' }, 'Customer says it still does not work · reopen comment posted');
  }

  resolve(id: string) {
    this.change(id, { status: 'resolved', resolvedAt: new Date() }, 'Resolved with the customer');
  }

  reopen(id: string) {
    this.change(id, { status: 'open', resolvedAt: undefined }, 'Reopened');
  }

  addNote(id: string, text: string) {
    const note = { at: new Date(), author: this.me(), text };
    this.update(id, t => ({
      ...t,
      notes: [note, ...t.notes],
      timeline: [...t.timeline, { at: note.at, actor: note.author, text: 'Logged a customer contact note' }],
    }));
  }

  private me(): string {
    return this.auth.user()?.name ?? 'Agent';
  }

  private change(id: string, patch: Partial<Ticket>, event: string) {
    this.update(id, t => ({ ...t, ...patch, timeline: [...t.timeline, { at: new Date(), actor: this.me(), text: event }] }));
  }

  /** Replace one ticket with a new object, so every computed() that reads it updates. */
  private update(id: string, fn: (t: Ticket) => Ticket) {
    this.tickets.update(list => list.map(t => (t.id === id ? fn(t) : t)));
  }
}
