import { Component, computed, inject, input, signal } from '@angular/core';
import { DatePipe, TitleCasePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DsPriorityBadge, DsSlaMeter, DsStatusPill } from '../../shared/ui';
import { CustomerService } from '../../core/theme/auth/customer.service';
import { TICKET_STATUS, TicketService } from '../../core/theme/auth/ticket.service';

type Tab = 'work' | 'timeline' | 'contact';

@Component({
  selector: 'app-ticket-workspace',
  imports: [DatePipe, TitleCasePipe, RouterLink, ReactiveFormsModule, DsPriorityBadge, DsSlaMeter, DsStatusPill],
  templateUrl: './ticket-workspace.html',
})
export class TicketWorkspace {
  protected tickets = inject(TicketService);
  private customers = inject(CustomerService);

  /** From the URL /tickets/:id. */
  id = input.required<string>();

  ticket = computed(() => this.tickets.byId(this.id()));
  customer = computed(() => {
    const t = this.ticket();
    return t ? this.customers.getByCif(t.cif) : undefined;
  });
  verified = computed(() => {
    const t = this.ticket();
    return !!t && this.customers.isVerified(t.cif);
  });

  sla = computed(() => {
    const t = this.ticket();
    return t ? this.tickets.sla(t) : undefined;
  });

  /** The customer's latest failed events, shown next to the ticket. */
  failedActivity = computed(() => {
    const t = this.ticket();
    return t ? this.customers.activityFor(t.cif).filter(e => e.failed).slice(0, 3) : [];
  });

  /** Timeline, newest first. */
  timeline = computed(() => [...(this.ticket()?.timeline ?? [])].reverse());

  /** SRE progress steps: done / current / todo. */
  steps = computed(() => {
    const t = this.ticket();
    const s = t?.status;
    const done = s === 'resolved' || s === 'closed';
    return [
      { label: 'Escalated', state: t?.adoItem ? 'done' : 'todo' },
      { label: 'With SRE', state: s === 'with-sre' ? 'current' : t?.adoItem && s !== 'open' ? 'done' : 'todo' },
      { label: 'Fixed by SRE · confirm', state: s === 'fixed' ? 'current' : done && t?.adoItem ? 'done' : 'todo' },
      { label: 'Resolved', state: done ? 'done' : 'todo' },
    ] as { label: string; state: 'done' | 'current' | 'todo' }[];
  });

  notePlaceholder = computed(
    () => `e.g. Called ${this.customer()?.name.split(' ')[0] ?? 'the customer'}, asked them to retry while on the call.`,
  );

  tab = signal<Tab>('work');
  statusInfo = TICKET_STATUS;

  note = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(5)] });
  noteSaved = signal(false);

  addNote() {
    if (this.note.invalid) {
      this.note.markAsTouched();
      return;
    }
    this.tickets.addNote(this.id(), this.note.value.trim());
    this.note.reset();
    this.noteSaved.set(true);
  }

  noteInvalid() {
    return this.note.invalid && this.note.touched;
  }

  // ---------- actions ----------
  start() { this.tickets.start(this.id()); }
  escalate() { this.tickets.escalate(this.id()); }
  askSre() { this.tickets.askSre(this.id()); }
  resolve() { this.tickets.resolve(this.id()); }
  stillNotWorking() { this.tickets.stillNotWorking(this.id()); }
  reopen() { this.tickets.reopen(this.id()); }
}
