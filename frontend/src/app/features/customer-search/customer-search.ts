import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CustomerService } from '../../core/theme/auth/customer.service';
import { Customer, DigitalStatus, maskMobile } from './customer.models';
import { VerifyCallerDialog } from './components/verify-caller-dialog/verify-caller-dialog';

/** Only digits (CIF) OR only letters and spaces (name), never mixed. */
const SEARCH_PATTERN = /^(\d+|[A-Za-z؀-ۿ\s]+)$/;

@Component({
  selector: 'app-customer-search',
  imports: [ReactiveFormsModule, VerifyCallerDialog],
  templateUrl: './customer-search.html',
  styleUrl: './customer-search.css',
})
export class CustomerSearch {
  private service = inject(CustomerService);
  private router = inject(Router);

  // Start from the last search, so "back" from Customer 360 keeps the results.
  search = new FormControl(this.service.lastQuery(), { nonNullable: true });
  /** Changes only when the user presses Search. */
  query = signal(this.service.lastQuery());

  invalidQuery = computed(() => !!this.query() && !SEARCH_PATTERN.test(this.query()));
  results = computed(() => (this.invalidQuery() ? [] : this.service.search(this.query())));

  /** Customer the verify popup is open for. */
  verifying = signal<Customer | null>(null);

  examples = [
    { cif: '123456', name: 'Ahmed Ali' },
    { cif: '889900', name: 'Mai Adel' },
    { cif: '445566', name: 'Laila Mostafa' },
  ];

  statusStyle: Record<DigitalStatus, string> = {
    active: 'bg-success-tint text-success',
    locked: 'bg-danger-tint text-danger',
    inactive: 'bg-page text-ink-3',
  };

  mask = maskMobile;

  onSearch() {
    const q = this.search.value.trim();
    this.query.set(q);
    this.service.lastQuery.set(q);
  }

  useExample(cif: string) {
    this.search.setValue(cif);
    this.onSearch();
  }

  /** Verification lives in the service, so Customer 360 and New ticket know about it too. */
  isVerified(c: Customer) {
    return this.service.isVerified(c.cif);
  }

  onConfirmed(c: Customer) {
    this.service.markVerified(c.cif);
    this.verifying.set(null);
  }

  onFailed() {
    this.verifying.set(null);
  }

  open360(c: Customer) {
    this.router.navigate(['/customer-search', c.cif]);
  }
}
