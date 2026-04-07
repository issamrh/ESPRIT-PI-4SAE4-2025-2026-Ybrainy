import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RecommendationSummary } from '../models/recommendation.model';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private readonly apiBase = environment.packApiUrl || `${environment.apiBaseUrl}/api`;
  private readonly apiUrl = `${this.apiBase}/recommendations`;

  constructor(private http: HttpClient) {}

  getSummary(limit = 10): Observable<RecommendationSummary> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<RecommendationSummary>(`${this.apiUrl}/summary`, { params });
  }
}
