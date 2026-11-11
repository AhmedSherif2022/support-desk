import { Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SignInError, SignInRequest } from '../../../../core/theme/auth/auth.models';
type Field = 'username' | 'password';

@Component({
  selector: 'app-sign-in-form',
  imports: [ReactiveFormsModule],
  templateUrl: './sign-in-form.html',
  host: { class: 'block' },
})
export class SignInForm {
  loading = input(false);
  error = input<SignInError>(null);
  submitted = output<SignInRequest>();

  showPassword = signal(false);

  form = inject(NonNullableFormBuilder).group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  private messages: Record<Field, string> = {
    username: 'Please enter your username.',
    password: 'Please enter your password.',
  };

  errorFor(name: Field): string | null {
    const c = this.form.controls[name];
    return c.hasError('required') && (c.dirty || c.touched) ? this.messages[name] : null;
  }


  invalid(name: Field) {
    const c = this.form.controls[name];
    return c.invalid && (c.dirty || c.touched);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(this.form.getRawValue());
  }
}