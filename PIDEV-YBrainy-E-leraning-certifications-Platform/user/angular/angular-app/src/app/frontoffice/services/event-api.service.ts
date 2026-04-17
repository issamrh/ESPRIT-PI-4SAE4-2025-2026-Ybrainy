import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event, Inscription } from '../../models/event.model';

export interface DescriptionGenerationResponse {
  description: string;
  generatedByAi: boolean;
}

export interface ImageGenerationResponse {
  imageUrl: string;
  generatedByAi: boolean;
}

export interface EventAnalytics {
  totalEvents: number;
  upcomingEvents: number;
  ongoingEvents: number;
  completedEvents: number;
  cancelledEvents: number;
  totalCapacity: number;
  eventsByType: { [key: string]: number };
}

export interface EventAssignmentResponse {
  message: string;
  event?: Event;
  studentId?: number;
}

/**
 * Service for event management. Calls the event-service backend directly via
 * the Angular dev-server proxy (/Event/** → http://localhost:8081).
 */
@Injectable({ providedIn: 'root' })
export class EventApiService {
  private readonly base = '/Event';

  constructor(private http: HttpClient) {}

  // ==================== CRUD ====================

  getAll(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.base}/all`);
  }

  getById(id: number): Observable<Event> {
    return this.http.get<Event>(`${this.base}/get/${id}`);
  }

  create(event: Partial<Event>): Observable<Event> {
    return this.http.post<Event>(`${this.base}/add`, event);
  }

  update(event: Event): Observable<Event> {
    return this.http.put<Event>(`${this.base}/update`, event);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/delete/${id}`);
  }

  // ==================== AI FEATURES ====================

  generateDescription(name: string, type: string): Observable<DescriptionGenerationResponse> {
    return this.http.post<DescriptionGenerationResponse>(`${this.base}/generate-description`, { name, type });
  }

  generateImage(name: string, description: string, type: string): Observable<ImageGenerationResponse> {
    return this.http.post<ImageGenerationResponse>(`${this.base}/generate-image`, { name, description, type });
  }

  uploadImage(imageFile: File): Observable<ImageGenerationResponse> {
    const formData = new FormData();
    formData.append('image', imageFile);
    return this.http.post<ImageGenerationResponse>(`${this.base}/upload-image`, formData);
  }

  getGeneratedImageUrl(fileName: string): string {
    return `${this.base}/generated-images/${fileName}`;
  }

  // ==================== ANALYTICS ====================

  getAnalytics(range: string = 'week'): Observable<EventAnalytics> {
    return this.http.get<EventAnalytics>(`${this.base}/analytics?range=${range}`);
  }

  // ==================== ASSIGNMENT ====================

  assignStudentToEvent(eventId: number, studentId: number): Observable<EventAssignmentResponse> {
    return this.http.post<EventAssignmentResponse>(`${this.base}/${eventId}/assign/${studentId}`, {});
  }
}
