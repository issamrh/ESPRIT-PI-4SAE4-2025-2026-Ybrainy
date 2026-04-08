export enum CartStatus {
    ACTIVE = 'ACTIVE',
    CHECKED_OUT = 'CHECKED_OUT',
    CANCELLED = 'CANCELLED'
}

export interface CartItem {
    id: number;
    packId: number;
    packTitle: string;
    priceAtPurchase: number;
    quantity: number;
    subtotal: number;
}

export interface Cart {
    id: number;
    userId: number;
    status: CartStatus;
    totalAmount: number;
    items: CartItem[];
}

export enum CartAction {
    ADD_ITEM = 'ADD_ITEM',
    REMOVE_ITEM = 'REMOVE_ITEM',
    CLEAR_CART = 'CLEAR_CART',
    CHECKOUT = 'CHECKOUT',
    CART_CREATED = 'CART_CREATED'
}

export interface CartHistory {
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

export interface StripeCheckoutSession {
    sessionId: string;
    checkoutUrl: string;
    publishableKey: string;
}
