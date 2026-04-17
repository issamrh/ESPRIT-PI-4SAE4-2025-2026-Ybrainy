import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PostRequest, PostResponse } from '../models/forum.models';

@Injectable({ providedIn: 'root' })
export class PostApiService {
  private readonly base = '/api/posts';

  constructor(private http: HttpClient) {}

  list(): Observable<PostResponse[]> {
    return this.http.get<PostResponse[]>(this.base);
  }

  getById(id: number): Observable<PostResponse> {
    return this.http.get<PostResponse>(`${this.base}/${id}`);
  }

  listByThread(threadId: number): Observable<PostResponse[]> {
    return this.http.get<PostResponse[]>(`${this.base}/thread/${threadId}`);
  }

  listByAuthor(authorId: number): Observable<PostResponse[]> {
    return this.http.get<PostResponse[]>(`${this.base}/author/${authorId}`);
  }

  countByThread(threadId: number): Observable<number> {
    return this.http.get<number>(`${this.base}/thread/${threadId}/count`);
  }

  create(input: PostRequest, image?: File | null, file?: File | null): Observable<PostResponse> {
    return this.http.post<PostResponse>(this.base, input);
  }

  update(id: number, input: PostRequest, image?: File | null, file?: File | null): Observable<PostResponse> {
    return this.http.put<PostResponse>(`${this.base}/${id}`, input);
  }

  delete(id: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`, { params: { userId: String(userId) } });
  }
}
