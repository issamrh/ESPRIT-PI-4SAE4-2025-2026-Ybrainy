import { Component, AfterViewInit, OnDestroy, ElementRef, HostListener } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, interval, Subscription } from 'rxjs';
import { AuthService, AuthUser } from '../services/auth.service';
import { NotificationApiService } from '../services/notification-api.service';
import { NotificationResponse } from '../models/forum.models';
import { MessageApiService } from '../services/message-api.service';

declare var $: any;

@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  host: { 'style': 'display:block' }
})
export class HeaderComponent implements AfterViewInit, OnDestroy {
  readonly assets = 'assets/frontoffice/www.ciklum.com/';
  isHome = true;
  menuOpen = false;
  currentUser: AuthUser | null = null;

  // Notifications
  notifOpen = false;
  notifications: NotificationResponse[] = [];
  unreadCount = 0;

  // Messages
  msgUnreadCount = 0;

  private readonly subscriptions = new Subscription();

  constructor(
    private el: ElementRef,
    private router: Router,
    public auth: AuthService,
    private notifApi: NotificationApiService,
    private msgApi: MessageApiService
  ) {}

  get userInitials(): string {
    const name = this.currentUser?.username ?? '';
    return name.slice(0, 2).toUpperCase() || '?';
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  // ── Notifications ────────────────────────────────────

  toggleNotifications(): void {
    this.notifOpen = !this.notifOpen;
    if (this.notifOpen) {
      this.loadNotifications();
    }
  }

  loadNotifications(): void {
    const uid = this.auth.currentUserId;
    if (!uid) return;
    this.notifApi.getAll(uid).subscribe({
      next: (list) => {
        this.notifications = list.slice(0, 20);
        this.unreadCount = list.filter((n) => !n.read).length;
      },
      error: () => {},
    });
  }

  markAllRead(): void {
    const uid = this.auth.currentUserId;
    if (!uid) return;
    this.notifApi.markAllAsRead(uid).subscribe({
      next: () => {
        this.notifications = this.notifications.map((n) => ({ ...n, read: true }));
        this.unreadCount = 0;
      },
      error: () => {},
    });
  }

  openNotif(n: NotificationResponse): void {
    if (!n.read) {
      this.notifApi.markAsRead(n.id).subscribe({
        next: () => {
          n.read = true;
          this.unreadCount = Math.max(0, this.unreadCount - 1);
        },
        error: () => {},
      });
    }
    this.notifOpen = false;
    if (n.threadId) {
      this.router.navigate(['/forum', n.threadId]);
    }
  }

  notifIcon(type: string): string {
    switch (type) {
      case 'POST_CREATED': return '💬';
      case 'COMMENT_CREATED': return '🗨️';
      case 'VOTED': return '⬆️';
      case 'REACTED': return '❤️';
      default: return '🔔';
    }
  }

  formatNotifDate(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    const diff = Math.floor((now.getTime() - d.getTime()) / 1000);
    if (diff < 60) return 'just now';
    if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
    if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
    return Math.floor(diff / 86400) + 'd ago';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.notifOpen && !this.el.nativeElement.querySelector('.header-notif-wrap')?.contains(event.target)) {
      this.notifOpen = false;
    }
  }

  private pollUnreadCount(): void {
    const uid = this.auth.currentUserId;
    if (!uid) return;
    this.notifApi.getUnreadCount(uid).subscribe({
      next: (count) => (this.unreadCount = count),
      error: () => {},
    });
    this.msgApi.getUnreadCount(uid).subscribe({
      next: (res) => (this.msgUnreadCount = res.count),
      error: () => {},
    });
  }

  // ── Lifecycle ────────────────────────────────────────

  ngAfterViewInit(): void {
    this.subscriptions.add(
      this.auth.currentUser$.subscribe((u) => {
        this.currentUser = u;
        if (u) {
          this.pollUnreadCount();
        } else {
          this.unreadCount = 0;
          this.notifications = [];
          this.notifOpen = false;
          this.msgUnreadCount = 0;
        }
      })
    );

    // Poll unread count every 30s
    this.subscriptions.add(
      interval(30_000).subscribe(() => this.pollUnreadCount())
    );

    this.syncIsHomeFromUrl(this.router.url);
    this.subscriptions.add(
      this.router.events
        .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
        .subscribe((e) => {
          this.syncIsHomeFromUrl(e.urlAfterRedirects);
          this.notifOpen = false;
        })
    );

    this.initStickyHeader();
    this.initMegaMenu();
    this.initMobileNav();
  }

  ngOnDestroy(): void {
    $(window).off('scroll.headerSticky');
    this.subscriptions.unsubscribe();
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

  private initMegaMenu(): void {
    const dropLinks = document.querySelectorAll('.drop-list-links');
    const dropList = document.querySelectorAll('.drop-list-tabs li');
    dropList.forEach((element: any, i: number) => {
      $(element).mouseenter(function () {
        $('.drop-list-tabs li').removeClass('active');
        $(element).addClass('active');
        $('.drop-list-links').removeClass('active');
        $(dropLinks[i]).addClass('active');
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

    initElem.on('click', '.has-dropdown > a', function (e: any) {
      e.preventDefault();
      curItem = $(e.target).closest('li');
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
        transform: 'translateX(-' + curLevel * 100 + '%)',
      });
    }

    updateMenuTitle();
  }
}
