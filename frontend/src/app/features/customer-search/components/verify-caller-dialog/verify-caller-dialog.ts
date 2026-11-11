import { Component, computed, input, output, signal } from '@angular/core';
import { Customer, maskMobile } from '../../customer.models';

@Component({
  selector: 'app-verify-caller-dialog',
  templateUrl: './verify-caller-dialog.html',
  host: { '(document:keydown.escape)': 'closed.emit()' }, // Esc closes the popup
})
export class VerifyCallerDialog {
  customer = input.required<Customer>();
  confirmed = output<Customer>();
  failed = output<Customer>();
  closed = output<void>();

  // The three checks
  nameDob = signal(false);
  city = signal(false);
  otp = signal(false);

  checkedCount = computed(() => [this.nameDob(), this.city(), this.otp()].filter(Boolean).length);
  /** Two of the three checks must pass. */
  canConfirm = computed(() => this.checkedCount() >= 3);

  firstName = computed(() => this.customer().name.split(' ')[0]);
  maskedMobile = computed(() => maskMobile(this.customer().mobile));
}
