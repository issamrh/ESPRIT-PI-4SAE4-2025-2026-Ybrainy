import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { InteractionTrackingService } from './tracking/interaction-tracking.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        {
          provide: InteractionTrackingService,
          useValue: { init: jasmine.createSpy('init') },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have the 'angular-app' title`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('angular-app');
  });

  // --- JASMINE MOCKING TEMPLATE EXAMPLE ---
  it('should demonstrate how to test with a mocked service (Template)', () => {
    // 1. Arrange: Setup your test data and get your mock service
    const mockService = TestBed.inject(InteractionTrackingService);
    
    // 2. Act: Trigger the behavior you want to test
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges(); // This triggers ngOnInit where services are usually called
    
    // 3. Assert: Verify the expected outcomes
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
    
    // Here we verify that the mocked method was called
    // expect(mockService.init).toHaveBeenCalled();
  });
});
