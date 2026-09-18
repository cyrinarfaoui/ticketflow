import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EventService } from '../../../core/services/event.service';

@Component({
  selector: 'app-event-form',
  imports: [ReactiveFormsModule],
  templateUrl: './event-form.html',
  styleUrl: './event-form.scss'
})
export class EventForm {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private router = inject(Router);

  form = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    date: ['', Validators.required],
    totalSeats: [1, [Validators.required, Validators.min(1)]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.eventService.createEvent({
      title: raw.title!,
      description: raw.description ?? '',
      date: new Date(raw.date!).toISOString(),
      totalSeats: raw.totalSeats!
    }).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => console.error('Erreur lors de la création', err)
    });
  }
}