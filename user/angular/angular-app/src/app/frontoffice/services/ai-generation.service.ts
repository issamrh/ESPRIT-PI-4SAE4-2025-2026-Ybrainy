import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AiGenerateResponse {
  body: string;
}

export interface AiChatHistoryItem {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiChatThreadSummary {
  id: number;
  title: string;
}

export interface AiChatApiResponse {
  reply: string;
  relatedThreads: AiChatThreadSummary[];
  hasDraft: boolean;
  draftTitle: string | null;
  draftBody: string | null;
}

@Injectable({ providedIn: 'root' })
export class AiGenerationService {
  private readonly base = '/api/ai';

  constructor(private http: HttpClient) {}

  /** Generate a well-structured question body for a new thread. */
  generateThreadBody(title: string): Observable<AiGenerateResponse> {
    return this.http.post<AiGenerateResponse>(`${this.base}/generate-thread-body`, { title });
  }

  /** Generate a helpful answer for a post, based on the thread title and body. */
  generatePostBody(threadTitle: string, threadBody: string): Observable<AiGenerateResponse> {
    return this.http.post<AiGenerateResponse>(`${this.base}/generate-post-body`, {
      threadTitle,
      threadBody,
    });
  }

  /** Chat with yForumy AI assistant. */
  chat(message: string, history: AiChatHistoryItem[]): Observable<AiChatApiResponse> {
    return this.http.post<AiChatApiResponse>(`${this.base}/chat`, { message, history });
  }
}
  