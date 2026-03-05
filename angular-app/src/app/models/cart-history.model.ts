export type CartAction = 'ADD_ITEM' | 'REMOVE_ITEM' | 'CLEAR_CART' | 'CHECKOUT' | 'CART_CREATED';

export type CartStatus = 'ACTIVE' | 'CHECKED_OUT' | 'CANCELLED';

export interface CartHistoryRecord {
  id: number;
  cartId: number;
  cartItemId?: number;
  action: CartAction;
  packTitle?: string;
  quantity?: number;
  totalAmount: number;
  cartStatus: CartStatus;
  description: string;
  createdAt: string;
}

export interface PackOrderRow extends CartHistoryRecord {
  completed: boolean;
  checkedOut: boolean;
}
