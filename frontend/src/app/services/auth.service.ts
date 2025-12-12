import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { UserSession } from '../models/user-session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;
  private currentUserSubject = new BehaviorSubject<UserSession | null>(null);

  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  loadSession(): Observable<UserSession | null> {
    if (this.currentUserSubject.value) {
      return of(this.currentUserSubject.value);
    }
    return this.http.get<UserSession>(`${this.baseUrl}/me`).pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError(() => {
        this.currentUserSubject.next(null);
        return of(null);
      })
    );
  }

  login(username: string, password: string): Observable<UserSession> {
    return this.http.post<UserSession>(`${this.baseUrl}/login`, { username, password }).pipe(
      tap(user => this.currentUserSubject.next(user))
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => this.currentUserSubject.next(null))
    );
  }

  get currentUser(): UserSession | null {
    return this.currentUserSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this.currentUserSubject.value;
  }
}
