import { Component, OnInit } from '@angular/core';
import { CartAction, CartHistory, CartStatus } from '../../models/cart.model';
import { CartService } from '../../services/cart.service';

type HistoryFilter = 'ALL' | CartAction;

@Component({
  selector: 'app-cart-history',
  standalone: false,
  templateUrl: './cart-history.component.html',
  styleUrls: ['./cart-history.component.css']
})
export class CartHistoryComponent implements OnInit {
  readonly filters: Array<{ label: string; value: HistoryFilter }> = [
    { label: 'All Activity', value: 'ALL' },
    { label: 'Added', value: CartAction.ADD_ITEM },
    { label: 'Removed', value: CartAction.REMOVE_ITEM },
    { label: 'Checked Out', value: CartAction.CHECKOUT }
  ];

  history: CartHistory[] = [];
  loading = true;
  error = '';
  selectedFilter: HistoryFilter = 'ALL';

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  get filteredHistory(): CartHistory[] {
    if (this.selectedFilter === 'ALL') {
      return this.history;
    }
    return this.history.filter((record) => record.action === this.selectedFilter);
  }

  get uniqueCartCount(): number {
    return new Set(this.history.map((record) => record.cartId)).size;
  }

  get checkoutCount(): number {
    return this.history.filter((record) => record.action === CartAction.CHECKOUT).length;
  }

  get addCount(): number {
    return this.history.filter((record) => record.action === CartAction.ADD_ITEM).length;
  }

  loadHistory(): void {
    this.loading = true;
    this.error = '';

    this.cartService.getHistory().subscribe({
      next: (history) => {
        this.history = [...(history ?? [])].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Unable to load your cart history right now.';
        this.loading = false;
      }
    });
  }

  getActionLabel(action: CartAction): string {
    switch (action) {
      case CartAction.ADD_ITEM:
        return 'Added';
      case CartAction.REMOVE_ITEM:
        return 'Removed';
      case CartAction.CLEAR_CART:
        return 'Cleared';
      case CartAction.CHECKOUT:
        return 'Checked Out';
      case CartAction.CART_CREATED:
      default:
        return 'Cart Created';
    }
  }

  getActionTone(action: CartAction): string {
    switch (action) {
      case CartAction.ADD_ITEM:
        return 'tone-positive';
      case CartAction.REMOVE_ITEM:
        return 'tone-warning';
      case CartAction.CHECKOUT:
        return 'tone-strong';
      case CartAction.CLEAR_CART:
        return 'tone-muted';
      case CartAction.CART_CREATED:
      default:
        return 'tone-neutral';
    }
  }

  getStatusLabel(status: CartStatus): string {
    switch (status) {
      case CartStatus.CHECKED_OUT:
        return 'Checked Out';
      case CartStatus.CANCELLED:
        return 'Cancelled';
      case CartStatus.ACTIVE:
      default:
        return 'Active';
    }
  }

  getFallbackDescription(record: CartHistory): string {
    if (record.packTitle) {
      return `${record.packTitle} was updated in your cart.`;
    }
    return 'Your cart activity was recorded successfully.';
  }

  trackById(_: number, record: CartHistory): number {
    return record.id;
  }
}
