import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Income, Expense } from '../models/finance.model';

export interface ScraperRunStatus {
  state: string;
  message: string;
  running: boolean;
  lastStartedAt: string | null;
  lastFinishedAt: string | null;
  lastExitCode: number | null;
  lastError: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class FinanceService {
  private apiUrl = environment.financeApiUrl || `${environment.apiBaseUrl}/api/finance`;
  private twelveDataApiKey = '25bff65f25b84c3dbed730c207e431b8';
  private twelveDataApiUrl = 'https://api.twelvedata.com';

  constructor(private http: HttpClient) {}

  getAllIncomes(): Observable<Income[]> {
    return this.http.get<Income[]>(`${this.apiUrl}/incomes`);
  }

  createIncome(income: Income): Observable<Income> {
    return this.http.post<Income>(`${this.apiUrl}/incomes`, income);
  }

  updateIncome(id: number, income: Income): Observable<Income> {
    return this.http.put<Income>(`${this.apiUrl}/incomes/${id}`, income);
  }

  deleteIncome(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/incomes/${id}`);
  }

  getAllExpenses(): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.apiUrl}/expenses`);
  }

  createExpense(expense: Expense): Observable<Expense> {
    return this.http.post<Expense>(`${this.apiUrl}/expenses`, expense);
  }

  updateExpense(id: number, expense: Expense): Observable<Expense> {
    return this.http.put<Expense>(`${this.apiUrl}/expenses/${id}`, expense);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/expenses/${id}`);
  }

  runScraper(): Observable<ScraperRunStatus> {
    return this.http.post<ScraperRunStatus>(`${this.apiUrl}/scraper/run`, {});
  }

  getScraperStatus(): Observable<ScraperRunStatus> {
    return this.http.get<ScraperRunStatus>(`${this.apiUrl}/scraper/status`);
  }

  getStockQuote(symbol: string): Observable<any> {
    return this.http.get(`${this.twelveDataApiUrl}/quote`, {
      params: {
        symbol: symbol.toUpperCase(),
        apikey: this.twelveDataApiKey
      }
    });
  }

  getTimeSeries(symbol: string, interval: string = '1day', outputsize: number = 30): Observable<any> {
    return this.http.get(`${this.twelveDataApiUrl}/time_series`, {
      params: {
        symbol: symbol.toUpperCase(),
        interval: interval,
        outputsize: outputsize.toString(),
        apikey: this.twelveDataApiKey
      }
    });
  }

  getCryptoQuote(symbol: string): Observable<any> {
    return this.http.get(`${this.twelveDataApiUrl}/quote`, {
      params: {
        symbol: symbol.toUpperCase(),
        apikey: this.twelveDataApiKey
      }
    });
  }

  getSymbols(exchange?: string): Observable<any> {
    const params: any = { apikey: this.twelveDataApiKey };
    if (exchange) {
      params.exchange = exchange;
    }
    return this.http.get(`${this.twelveDataApiUrl}/symbol_list`, { params });
  }
}
