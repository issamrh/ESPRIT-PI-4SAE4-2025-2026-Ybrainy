import { Component, OnInit } from '@angular/core';
import { Cart, CartItem, CartStatus } from '../../models/cart.model';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: false,
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {
  cart: Cart | null = null;
  loading = true;
  error = '';
  feedback = '';
  removingItemId: number | null = null;
  clearing = false;

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.loadCart();
  }

  get hasItems(): boolean {
    return (this.cart?.items?.length ?? 0) > 0;
  }

  get totalItems(): number {
    return (this.cart?.items ?? []).reduce((sum, item) => sum + item.quantity, 0);
  }

  get totalAmount(): number {
    return this.cart?.totalAmount ?? 0;
  }

  loadCart(): void {
    this.loading = true;
    this.error = '';

    this.cartService.loadCart().subscribe({
      next: (cart) => {
        this.cart = cart;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        if (err?.status === 404) {
          this.cart = null;
          return;
        }
        this.error = err?.error?.message || 'Unable to load your cart right now.';
      }
    });
  }

  removeItem(item: CartItem): void {
    this.feedback = '';
    this.error = '';
    this.removingItemId = item.id;

    this.cartService.removeFromCart(item.id).subscribe({
      next: (cart) => {
        this.cart = cart;
        this.feedback = `"${item.packTitle}" was removed from your cart.`;
        this.removingItemId = null;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to remove this pack from your cart.';
        this.removingItemId = null;
      }
    });
  }

  clearCart(): void {
    this.feedback = '';
    this.error = '';
    this.clearing = true;

    this.cartService.clearCart().subscribe({
      next: (cart) => {
        this.cart = cart;
        this.feedback = 'Your cart is now empty.';
        this.clearing = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to clear your cart.';
        this.clearing = false;
      }
    });
  }

  getStatusLabel(status: CartStatus | undefined): string {
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
}
