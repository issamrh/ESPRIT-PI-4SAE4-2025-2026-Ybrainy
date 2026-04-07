export enum ExpenseCategory {
    RENT = 'RENT',
    SALARIES = 'SALARIES',
    MARKETING = 'MARKETING',
    EQUIPMENT = 'EQUIPMENT',
    SOFTWARE = 'SOFTWARE',
    UTILITIES = 'UTILITIES',
    INFRASTRUCTURE = 'INFRASTRUCTURE',
    OTHER = 'OTHER'
}

export enum ExpenseStatus {
    PENDING = 'PENDING',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    PAID = 'PAID'
}

export interface Income {
    id?: number;
    sourceType: string; // 'MANUAL' or 'PACK_PURCHASE'
    referenceId?: number;
    description?: string;
    amount: number;
    currency: string;
    paymentMethod: string;
    receivedDate?: string;
    createdAt?: string;
}

export interface Expense {
    id?: number;
    title: string;
    description?: string;
    amount: number;
    currency: string;
    category: ExpenseCategory;
    status: ExpenseStatus;
    expenseDate: string;
    createdAt?: string;
}
/* ─── Twelve Data API Types ─── */
export interface TwelveDataQuote {
    symbol: string;
    name?: string;
    exchange?: string;
    currency?: string;
    type?: string;
    price?: number;
    previous_close?: number;
    updated_at?: string;
}

export interface TwelveDataTimeSeriesValue {
    datetime: string;
    open: string | number;
    high: string | number;
    low: string | number;
    close: string | number;
    volume: string | number;
}

export interface TwelveDataTimeSeries {
    meta?: {
        symbol: string;
        interval: string;
        currency?: string;
    };
    status: string;
    values?: TwelveDataTimeSeriesValue[];
}