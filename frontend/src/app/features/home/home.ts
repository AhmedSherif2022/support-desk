import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DsPriorityBadge, DsSlaState } from '../../shared/ui';
import { AuthService } from '../../core/theme/auth/auth.service';
import { CustomerService } from '../../core/theme/auth/customer.service';
import { TICKET_STATUS, TicketService } from '../../core/theme/auth/ticket.service';
import { Ticket } from '../customer-search/customer.models';
import { SERVICE_HEALTH } from '../customer-search/customers.mock';

type QueueFilter = 'all' | 'risk' | 'confirm' | 'auto';

@Component({
  selector: 'app-home',
  imports: [RouterLink, DsPriorityBadge],
  templateUrl: './home.html',
})
export class Home {
  private auth = inject(AuthService);
  private customers = inject(CustomerService);
  protected tickets = inject(TicketService);

  firstName = computed(() => this.auth.user()?.name.split(' ')[0] ?? '');
  greeting = computed(() => {
    const h = new Date(this.tickets.now()).getHours();
    return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
  });

  // ---------- known issue banner ----------
  incident = computed(() => this.customers.activeIncidents()[0]);
  linkedCount = computed(() => {
    const inc = this.incident();
    return inc ? this.tickets.mine().filter(t => t.incidentId === inc.id && !this.tickets.isDone(t)).length : 0;
  });
  showScript = signal(false);

  // ---------- queue ----------
  /** Open work, ordered by SLA risk, then by age. */
  queue = computed(() =>
    this.tickets
      .mine()
      .filter(t => !this.tickets.isDone(t))
      .sort(
        (a, b) =>
          this.tickets.riskRank(a) - this.tickets.riskRank(b) || a.createdAt.getTime() - b.createdAt.getTime(),
      ),
  );

  filter = signal<QueueFilter>('all');

  filters = computed(() => {
    const q = this.queue();
    return [
      { id: 'all' as const, label: 'All', count: q.length },
      { id: 'risk' as const, label: 'At risk', count: q.filter(t => this.atRisk(t)).length },
      { id: 'confirm' as const, label: 'Confirm fix', count: q.filter(t => t.status === 'fixed').length },
      { id: 'auto' as const, label: 'Auto-created', count: q.filter(t => t.origin === 'monitor').length },
    ];
  });

  visible = computed(() => {
    const q = this.queue();
    switch (this.filter()) {
      case 'risk': return q.filter(t => this.atRisk(t));
      case 'confirm': return q.filter(t => t.status === 'fixed');
      case 'auto': return q.filter(t => t.origin === 'monitor');
      default: return q;
    }
  });

  shown = computed(() => this.visible().slice(0, 8));

  // ---------- side ----------
  health = SERVICE_HEALTH;

  attention = computed(() => {
    const items: { text: string; tone: 'bad' | 'info'; link: string }[] = [];
    for (const t of this.queue()) {
      if (this.tickets.sla(t).state === 'breached') items.push({ text: `SLA breached on #${t.id}`, tone: 'bad', link: `/tickets/${t.id}` });
    }
    for (const t of this.queue()) {
      if (t.status === 'fixed') items.push({ text: `SRE fixed #${t.id} — confirm with the customer`, tone: 'info', link: `/tickets/${t.id}` });
      else if (t.adoItem && !t.incidentId) items.push({ text: `Ticket #${t.id} is now ${t.adoItem} in Azure DevOps`, tone: 'info', link: `/tickets/${t.id}` });
    }
    const inc = this.incident();
    if (inc) items.push({ text: `Known issue: ${inc.summary}. Tickets are grouped under ${inc.id}.`, tone: 'bad', link: '/tickets' });
    return items;
  });

  status = TICKET_STATUS;

  atRisk(t: Ticket) {
    const s = this.tickets.sla(t).state;
    return s === 'breached' || s === 'at-risk';
  }

  customerLine(t: Ticket) {
    const c = this.tickets.customerOf(t);
    return [c?.name, c?.segment, t.platform === 'mobile' ? 'Mobile' : 'Web', t.transactionRef].filter(Boolean).join(' · ');
  }

  /** What the agent should do next on this ticket. */
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

  /** Icon tile colour: red for breached, purple while SRE owns it, blue otherwise. */
  tileClass(t: Ticket) {
    if (this.tickets.sla(t).state === 'breached') return 'bg-danger-tint text-danger';
    if (t.status === 'with-sre') return 'bg-sre-tint text-sre';
    if (t.status === 'fixed') return 'bg-warning-tint text-warning';
    return 'bg-blue-50 text-brand';
  }
}
