import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { InteractionTrackingService } from './tracking/interaction-tracking.service';

describe('AppComponent', () => {
  let tracking: jasmine.SpyObj<InteractionTrackingService>;

  beforeEach(async () => {
    tracking = jasmine.createSpyObj<InteractionTrackingService>('InteractionTrackingService', ['init']);

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        { provide: InteractionTrackingService, useValue: tracking },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should initialize interaction tracking on startup', () => {
    TestBed.createComponent(AppComponent);
    expect(tracking.init).toHaveBeenCalled();
  });
});
