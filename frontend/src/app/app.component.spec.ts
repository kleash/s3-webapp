import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { environment } from '../environments/environment';

describe('AppComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, HttpClientTestingModule]
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads buckets and selects first', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const bucketsReq = httpMock.expectOne(`${environment.apiBaseUrl}/buckets`);
    bucketsReq.flush([{ id: 'a', name: 'A', bucketName: 'bucket-a' }]);

    const listReq = httpMock.expectOne(`${environment.apiBaseUrl}/buckets/a/objects`);
    listReq.flush({ currentPrefix: '', folders: [], objects: [] });

    expect(fixture.componentInstance.buckets.length).toBe(1);
    expect(fixture.componentInstance.selectedBucketId).toBe('a');
  });

  it('sends bulk delete with keys and prefixes', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const comp = fixture.componentInstance;
    comp.selectedBucketId = 'demo';
    comp.selectedKeys = ['file.txt', 'folder/'];
    comp.bulkDelete();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/buckets/demo/objects`);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.body.keys).toContain('file.txt');
    expect(req.request.body.prefixes).toContain('folder/');
    req.flush([]);

    const reloadReq = httpMock.expectOne(`${environment.apiBaseUrl}/buckets/demo/objects`);
    reloadReq.flush({ currentPrefix: '', folders: [], objects: [] });
  });
});
