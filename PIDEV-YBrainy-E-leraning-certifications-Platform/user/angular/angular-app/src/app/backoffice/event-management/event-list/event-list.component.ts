import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Event, EventStatut, EventType } from '../../../models/event.model';
import { EventApiService } from '../../../frontoffice/services/event-api.service';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './event-list.component.html',
  styleUrl: './event-list.component.css'
})
export class EventListComponent implements OnInit {
  events: Event[] = [];
  filteredEvents: Event[] = [];
  loading = true;
  error = '';
  deleteError = '';
  deletingId: number | null = null;
  searchTerm = '';
  filterType = '';
  filterStatus = '';

  readonly eventTypes = Object.values(EventType);
  readonly eventStatuses = Object.values(EventStatut);

  constructor(private eventApi: EventApiService, private router: Router) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading = true;
    this.error = '';
    this.eventApi.getAll().subscribe({
      next: (events) => {
        this.events = events;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load events. Make sure the event service is running on port 8081.';
        console.error(err);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    let result = [...this.events];
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(e =>
        e.name?.toLowerCase().includes(term) ||
        e.location?.toLowerCase().includes(term) ||
        e.description?.toLowerCase().includes(term)
      );
    }
    if (this.filterType) {
      result = result.filter(e => e.type === this.filterType);
    }
    if (this.filterStatus) {
      result = result.filter(e => e.statut === this.filterStatus);
    }
    this.filteredEvents = result;
  }

  onSearch(): void {
    this.applyFilters();
  }

  createEvent(): void {
    this.router.navigate(['/dashboard/events/new']);
  }

  editEvent(event: Event): void {
    this.router.navigate(['/dashboard/events', event.idEvent, 'edit']);
  }

  deleteEvent(event: Event): void {
    if (!confirm(`Delete event "${event.name}"? This cannot be undone.`)) return;
    this.deletingId = event.idEvent;
    this.deleteError = '';
    this.eventApi.delete(event.idEvent).subscribe({
      next: () => {
        this.events = this.events.filter(e => e.idEvent !== event.idEvent);
        this.applyFilters();
        this.deletingId = null;
      },
      error: (err) => {
        this.deleteError = 'Failed to delete event. Please try again.';
        console.error(err);
        this.deletingId = null;
      }
    });
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case EventStatut.UPCOMING: return 'badge-upcoming';
      case EventStatut.ONGOING: return 'badge-ongoing';
      case EventStatut.COMPLETED: return 'badge-completed';
      case EventStatut.CANCELLED: return 'badge-cancelled';
      default: return 'badge-default';
    }
  }

  getTypeClass(type: string): string {
    switch (type) {
      case EventType.WORKSHOP: return 'type-workshop';
      case EventType.SEMINAR: return 'type-seminar';
      case EventType.WEBINAR: return 'type-webinar';
      case EventType.COURSE: return 'type-course';
      default: return 'type-default';
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }
}
