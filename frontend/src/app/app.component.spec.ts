import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';
import { of } from 'rxjs';

describe('AppComponent shell', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['loadSession', 'logout'], { currentUser$: of(null) });
    authService.loadSession.and.returnValue(of(null));
    authService.logout.and.returnValue(of(void 0));
    await TestBed.configureTestingModule({
      imports: [AppComponent, RouterTestingModule]
    }).overrideProvider(AuthService, { useValue: authService }).compileComponents();
  });

  it('initializes session on start', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(authService.loadSession).toHaveBeenCalled();
  });
});
