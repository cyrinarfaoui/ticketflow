import { Component, signal ,inject} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { EventList } from './features/events/event-list/event-list';
import { AuthService } from './core/services/auth.service';

@Component({
  imports: [RouterOutlet,EventList],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('ticketflow-web');
   protected authService = inject(AuthService);
}
