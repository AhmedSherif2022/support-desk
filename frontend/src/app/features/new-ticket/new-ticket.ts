import { Component, computed, inject, input } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { DsCallout, DsPriority, DsPriorityBadge, PRIORITY_META } from '../../shared/ui';
import { CustomerService } from '../../core/theme/auth/customer.service';
import { TicketService } from '../../core/theme/auth/ticket.service';
import { Platform } from '../customer-search/customer.models';

/** Category → its issues. TODO: load from the error-rules API. */
const CATEGORIES: Record<string, string[]> = {
  Authentication: ['Login Failed', 'Biometric login', 'Password reset'],
  OTP: ['OTP Not Received', 'OTP Expired'],
  'Mobile Application': ['App crash', 'App not opening'],
  'Web Banking': ['Page not loading', 'Session expired'],
  Transfers: ['Transfer Failed', 'Transfer Pending'],
  Payments: ['Bill payment failed', 'Card payment declined'],
};

@Component({
  selector: 'app-new-ticket',
  imports: [ReactiveFormsModule, RouterLink, DsCallout, DsPriorityBadge],
  templateUrl: './new-ticket.html',
})
export class NewTicket {
  private service = inject(CustomerService);
  private tickets = inject(TicketService);
  private router = inject(Router);

  /** From the URL /customer-search/:cif/new-ticket. */
  cif = input.required<string>();

  customer = computed(() => this.service.getByCif(this.cif()));
  verified = computed(() => this.service.isVerified(this.cif()));

  /** Latest failed event: its error code is attached to the ticket. */
  latestError = computed(() => this.service.activityFor(this.cif()).find(e => e.failed));

  /** An open ticket already exists → warn before creating a duplicate. */
  openTicket = computed(() =>
    this.tickets.forCustomer(this.cif()).find(t => !this.tickets.isDone(t)),
  );

  categoryNames = Object.keys(CATEGORIES);
  priorities: DsPriority[] = ['critical', 'high', 'medium', 'low'];
  platforms: { id: Platform; label: string; icon: string }[] = [
    { id: 'mobile', label: 'Mobile', icon: 'pi pi-mobile' },
    { id: 'web', label: 'Web', icon: 'pi pi-desktop' },
  ];

  form = inject(NonNullableFormBuilder).group({
    platform: ['mobile' as Platform],
    priority: ['high' as DsPriority],
    category: ['Authentication'],
    issue: ['Login Failed'],
    report: ['', [Validators.required, Validators.minLength(10)]],
  });

  /** The form value as a signal, so the preview and the callouts update while the agent types. */
  value = toSignal(this.form.valueChanges.pipe(map(() => this.form.getRawValue())), {
    initialValue: this.form.getRawValue(),
  });

  issues = computed(() => CATEGORIES[this.value().category] ?? []);
  sla = computed(() => PRIORITY_META[this.value().priority].slaHours);
  incident = computed(() => this.service.incidentFor(this.value().category, this.value().platform));

  constructor() {
    // A new category → pick its first issue.
    this.form.controls.category.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(category => this.form.controls.issue.setValue(CATEGORIES[category]?.[0] ?? ''));
  }

  reportInvalid() {
    const c = this.form.controls.report;
    return c.invalid && (c.dirty || c.touched);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const ticket = this.tickets.create({ cif: this.cif(), ...this.form.getRawValue() });
    this.router.navigate(['/tickets', ticket.id]);
  }

  cancel() {
    this.router.navigate(['/customer-search', this.cif()]);
  }
}
