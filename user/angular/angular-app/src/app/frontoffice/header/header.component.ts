import { AfterViewInit, ChangeDetectorRef, Component, HostListener, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import {
  getDisplayName,
  getRealmRoles,
  isAuthenticated,
  logout,
  redirectToAppLogin
} from '../../auth/keycloak.service';
import { UserSessionService } from '../../tracking/user-session.service';
import { UserService } from '../services/user.service';
import { CartService } from '../services/cart.service';
import { CartOverlayService, CartOverlayTab } from '../services/cart-overlay.service';

declare var $: any;

@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  host: { style: 'display:block' }
})
export class HeaderComponent implements AfterViewInit, OnDestroy {
  readonly assets = 'assets/frontoffice/www.ciklum.com/';

  isHome = true;
  menuOpen = false;
  showModeDropdown = false;
  cartItemCount = 0;
  headerProfileImage: string | null = null;
  headerProfileName: string | null = null;
  checkoutInProgress = false;
  showCheckoutToast = false;
  checkoutToastType: 'success' | 'error' = 'success';
  checkoutToastTitle = '';
  checkoutToastMessage = '';

  private readonly subscriptions = new Subscription();
  private checkoutToastTimer: ReturnType<typeof setTimeout> | null = null;
  private processingStripeSessionId: string | null = null;

  constructor(
    private readonly router: Router,
    private readonly frontofficeUserService: UserService,
    private readonly userSession: UserSessionService,
    private readonly cdr: ChangeDetectorRef,
    private readonly cartService: CartService,
    private readonly cartOverlay: CartOverlayService
  ) {}

  get authenticated(): boolean {
    return isAuthenticated();
  }

  get displayName(): string | null {
    return getDisplayName();
  }

  get resolvedDisplayName(): string | null {
    return this.headerProfileName || this.displayName;
  }

  get isAdminUser(): boolean {
    return getRealmRoles().some((role) => String(role).toUpperCase() === 'ADMIN');
  }

  get currentMode(): string {
    return this.userSession.getMode();
  }

  get currentRole(): string {
    return this.userSession.get()?.role || 'STUDENT';
  }

  get availableModes(): Array<'STUDENT' | 'INSTRUCTOR' | 'ADMIN'> {
    return this.userSession.getAvailableModes();
  }

  getModeIcon(mode: string): string {
    if (mode === 'STUDENT') return 'S';
    if (mode === 'INSTRUCTOR') return 'I';
    if (mode === 'ADMIN') return 'A';
    return 'U';
  }

  getModeLabel(mode: string): string {
    if (mode === 'STUDENT') return 'Student Mode';
    if (mode === 'INSTRUCTOR') return 'Instructor Mode';
    if (mode === 'ADMIN') return 'Admin Mode';
    return 'User';
  }

  switchMode(mode: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'): void {
    if (!this.userSession.canAccessMode(mode)) return;

    this.userSession.setMode(mode);
    this.showModeDropdown = false;

    if (mode === 'STUDENT') {
      this.router.navigate(['/courses']);
    } else if (mode === 'INSTRUCTOR') {
      this.router.navigate(['/dashboard/courses']);
    } else if (mode === 'ADMIN') {
      this.router.navigate(['/dashboard']);
    }

    this.cdr.detectChanges();
  }

  toggleModeDropdown(): void {
    this.showModeDropdown = !this.showModeDropdown;
  }

  onLogin(): void {
    redirectToAppLogin(window.location.href);
  }

  async onLogout(): Promise<void> {
    await logout();
  }

  getAvatarInitials(): string {
    const source = (this.resolvedDisplayName || '').trim();
    if (!source) return '?';
    const parts = source.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
    }
    return (parts[0]?.slice(0, 2) || '?').toUpperCase();
  }

  ngAfterViewInit(): void {
    this.syncIsHomeFromUrl(this.router.url);
    this.handleStripeCheckoutReturn(this.router.url);
    this.loadHeaderUserData();

    this.subscriptions.add(
      this.cartService.cart$.subscribe((cart) => {
        this.cartItemCount = (cart?.items ?? []).reduce((count, item) => count + item.quantity, 0);
      })
    );

    this.syncCartState();

    this.subscriptions.add(
      this.router.events
        .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
        .subscribe((event) => {
          this.syncIsHomeFromUrl(event.urlAfterRedirects);
          this.handleStripeCheckoutReturn(event.urlAfterRedirects);
          this.loadHeaderUserData();
          this.syncCartState();
          this.showModeDropdown = false;
        })
    );

    this.initStickyHeader();
    this.initMegaMenu();
    this.initMobileNav();
  }

  ngOnDestroy(): void {
    $(window).off('scroll.headerSticky');

    if (this.checkoutToastTimer) {
      clearTimeout(this.checkoutToastTimer);
      this.checkoutToastTimer = null;
    }

    this.subscriptions.unsubscribe();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (!target?.closest('.mode-switcher-container')) {
      this.showModeDropdown = false;
    }
  }

  openCartOverlay(tab: CartOverlayTab = 'cart'): void {
    this.showModeDropdown = false;

    if (this.menuOpen) {
      this.toggleMenu();
    }

    if (tab === 'history') {
      this.cartOverlay.openHistory();
      return;
    }

    this.cartOverlay.openCart();
  }

  closeCheckoutToast(): void {
    this.showCheckoutToast = false;

    if (this.checkoutToastTimer) {
      clearTimeout(this.checkoutToastTimer);
      this.checkoutToastTimer = null;
    }
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
    $('.menu').toggleClass('show-menu');
    $('.nav-wrapper').toggleClass('show-menu');
    const rt = window.innerWidth;
    const menuBtnX = $('.js-nav-toggle').offset()?.left || 0;
    $('.js-nav-toggle').css('right', 0);
    $('.show-menu .js-nav-toggle').css('right', -(rt - menuBtnX - 50));
  }

  private initStickyHeader(): void {
    function fixedHeader() {
      const sticky = $('#header');
      const scroll = $(window).scrollTop();
      if (scroll >= 10) sticky.addClass('fixHeader');
      else sticky.removeClass('fixHeader');
    }

    $(window).on('scroll.headerSticky', fixedHeader);
    fixedHeader();
  }

  private syncIsHomeFromUrl(url: string): void {
    const clean = (url || '').split('?')[0].split('#')[0];
    this.isHome = clean === '' || clean === '/';
  }

  private loadHeaderUserData(): void {
    if (!this.authenticated) {
      this.headerProfileImage = null;
      this.headerProfileName = null;
      return;
    }

    this.frontofficeUserService.getCurrentUser().subscribe({
      next: (user) => {
        this.headerProfileImage = user.profilePicture ?? null;
        this.headerProfileName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim() || user.username || null;
      },
      error: () => {
        this.headerProfileImage = null;
        this.headerProfileName = null;
      }
    });
  }

  private syncCartState(): void {
    if (!this.authenticated) {
      this.cartItemCount = 0;
      return;
    }

    this.cartService.refreshCart();
  }

  private showCheckoutMessage(type: 'success' | 'error', title: string, message: string): void {
    this.checkoutToastType = type;
    this.checkoutToastTitle = title;
    this.checkoutToastMessage = message;
    this.showCheckoutToast = true;

    if (this.checkoutToastTimer) {
      clearTimeout(this.checkoutToastTimer);
    }

    this.checkoutToastTimer = setTimeout(() => this.closeCheckoutToast(), 4000);
  }

  private handleStripeCheckoutReturn(url: string): void {
    const queryIndex = url.indexOf('?');
    if (queryIndex < 0) {
      return;
    }

    const params = new URLSearchParams(url.substring(queryIndex + 1));
    const payment = params.get('payment');

    if (payment === 'cancelled') {
      this.showCheckoutMessage(
        'error',
        'Checkout cancelled',
        'Your payment was cancelled. You can try again anytime.'
      );
      this.clearStripeQueryParams();
      return;
    }

    if (payment !== 'success') {
      return;
    }

    const sessionId = params.get('session_id');
    if (!sessionId) {
      this.showCheckoutMessage(
        'error',
        'Checkout failed',
        'Missing Stripe session id in return URL.'
      );
      this.clearStripeQueryParams();
      return;
    }

    if (this.processingStripeSessionId === sessionId || this.checkoutInProgress) {
      return;
    }

    this.processingStripeSessionId = sessionId;
    this.checkoutInProgress = true;

    this.cartService.confirmStripeCheckout(sessionId).subscribe({
      next: () => {
        this.showCheckoutMessage(
          'success',
          'Enrollment confirmed',
          'Your learning pack checkout was completed successfully.'
        );
        this.checkoutInProgress = false;
        this.processingStripeSessionId = null;
        this.clearStripeQueryParams();
      },
      error: (err) => {
        this.checkoutInProgress = false;
        this.processingStripeSessionId = null;
        console.error('Stripe checkout confirmation failed', err);
        this.showCheckoutMessage(
          'error',
          'Checkout failed',
          err.error?.message || err.message || 'Payment verification failed. Please contact support.'
        );
        this.clearStripeQueryParams();
      }
    });
  }

  private clearStripeQueryParams(): void {
    const cleanPath = this.router.url.split('?')[0] || '/';
    this.router.navigateByUrl(cleanPath, { replaceUrl: true });
  }

  private initMegaMenu(): void {
    const dropLinks = document.querySelectorAll('.drop-list-links');
    const dropList = document.querySelectorAll('.drop-list-tabs li');
    dropList.forEach((element: any, index: number) => {
      $(element).mouseenter(function () {
        $('.drop-list-tabs li').removeClass('active');
        $(element).addClass('active');
        $('.drop-list-links').removeClass('active');
        $(dropLinks[index]).addClass('active');
      });
    });

    $('.drop-big').mouseenter(function (this: any) {
      const allList = $(this).find('.drop-list-tabs li');
      const allListTab = $(this).find('.drop-list-links');
      $(allList).removeClass('active');
      $(allListTab).removeClass('active');
      $(allList[0]).addClass('active');
      $(allListTab[0]).addClass('active');
    });

    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach((item: any) => {
      if (item.querySelector('.dropdown') !== null) {
        item.classList.add('dr-icon');
      }
    });
  }

  private initMobileNav(): void {
    if ($(window).outerWidth() >= 990) return;

    const initElem = $('nav');
    if (!initElem.length) return;

    let curLevel = 0;
    let curItem: any = null;

    initElem.on('click', '.has-dropdown > a', function (event: any) {
      event.preventDefault();
      curItem = $(event.target).closest('li');
      curLevel += 1;
      curItem.addClass('nav-dropdown-open nav-dropdown-active');
      updateMenuTitle();
      slideMenu();
    });

    initElem.on('click', '.nav-toggle', function () {
      if (curItem) {
        curItem.removeClass('nav-dropdown-open nav-dropdown-active');
        curItem = curItem.parent().closest('li');
        if (curItem.length) {
          curItem.addClass('nav-dropdown-open nav-dropdown-active');
        }
      }
      curLevel = curLevel > 0 ? curLevel - 1 : 0;
      updateMenuTitle();
      slideMenu();
    });

    function updateMenuTitle() {
      let title = 'Menu';
      if (curLevel > 0 && curItem && curItem.length) {
        title = curItem.children('a').text();
        initElem.find('.nav-toggle').addClass('back-visible');
      } else {
        initElem.find('.nav-toggle').removeClass('back-visible');
      }
      $('.nav-title').text(title);
    }

    function slideMenu() {
      initElem.children('ul').css({
        transform: 'translateX(-' + curLevel * 100 + '%)'
      });
    }

    updateMenuTitle();
  }
}
