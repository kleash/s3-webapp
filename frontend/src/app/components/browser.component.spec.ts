import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BrowserComponent } from './browser.component';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { of } from 'rxjs';

class AuthStub {
  currentUser = { username: 'bob', accessLevel: 'READ_WRITE' as const };
  currentUser$ = of(this.currentUser);
}

describe('BrowserComponent', () => {
  let fixture: ComponentFixture<BrowserComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BrowserComponent, HttpClientTestingModule],
      providers: [{ provide: AuthService, useClass: AuthStub }]
    }).compileComponents();
    fixture = TestBed.createComponent(BrowserComponent);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads buckets and selects first', () => {
    fixture.detectChanges();

    const bucketsReq = httpMock.expectOne(`${environment.apiBaseUrl}/buckets`);
    bucketsReq.flush([{ id: 'a', name: 'A', bucketName: 'bucket-a' }]);

    const listReq = httpMock.expectOne(`${environment.apiBaseUrl}/buckets/a/objects`);
    listReq.flush({ currentPrefix: '', folders: [], objects: [] });

    expect(fixture.componentInstance.buckets.length).toBe(1);
    expect(fixture.componentInstance.selectedBucketId).toBe('a');
  });

  it('sends bulk delete with keys and prefixes', () => {
    fixture.detectChanges();
    const bucketsReq = httpMock.expectOne(`${environment.apiBaseUrl}/buckets`);
    bucketsReq.flush([]);

    const comp = fixture.componentInstance;
    comp.selectedBucketId = 'demo';
    comp.selectedKeys = ['file.txt', 'folder/'];
    comp.bulkDelete();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/buckets/demo/objects`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body.keys).toContain('file.txt');
    expect(req.request.body.prefixes).toContain('folder/');
    req.flush([]);

    const reload = httpMock.expectOne(`${environment.apiBaseUrl}/buckets/demo/objects`);
    reload.flush({ currentPrefix: '', folders: [], objects: [] });
  });
});
