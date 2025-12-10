import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ObjectBrowserComponent } from './object-browser.component';

describe('ObjectBrowserComponent', () => {
  let fixture: ComponentFixture<ObjectBrowserComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ObjectBrowserComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(ObjectBrowserComponent);
  });

  it('renders folders and files', () => {
    fixture.componentInstance.folders = [{ name: 'logs', fullPath: 'logs/' }];
    fixture.componentInstance.objects = [{ key: 'logs/app.log', name: 'app.log', sizeBytes: 1000, lastModified: new Date().toISOString(), contentType: 'text/plain' }];
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('logs');
    expect(compiled.textContent).toContain('app.log');
  });

  it('builds breadcrumbs from prefix', () => {
    fixture.componentInstance.currentPrefix = 'logs/app/2025/';
    fixture.detectChanges();
    const crumbs = fixture.componentInstance.breadcrumbs;
    expect(crumbs.map(c => c.name)).toEqual(['logs', 'app', '2025']);
  });

  it('selects all rows via header checkbox', () => {
    const component = fixture.componentInstance;
    component.folders = [{ name: 'logs', fullPath: 'logs/' }];
    component.objects = [{ key: 'logs/app.log', name: 'app.log', sizeBytes: 1000, lastModified: new Date().toISOString(), contentType: 'text/plain' }];
    const emitted: string[][] = [];
    component.selectionChange.subscribe(val => emitted.push(val));
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const headerCheckbox = compiled.querySelector('.row.header input') as HTMLInputElement;
    headerCheckbox.click();
    fixture.detectChanges();
    expect(emitted[0].length).toBe(2);
  });
});
