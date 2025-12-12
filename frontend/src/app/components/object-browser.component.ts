import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FolderItem } from '../models/folder-item';
import { ObjectItem } from '../models/object-item';

@Component({
  selector: 'app-object-browser',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="browser">
      <div class="breadcrumb" *ngIf="breadcrumbs.length">
        <span class="crumb" (click)="navigateTo('')">root</span>
        <span *ngFor="let crumb of breadcrumbs; let i = index">
          / <span class="crumb" (click)="navigateTo(crumb.path)">{{ crumb.name }}</span>
        </span>
      </div>

      <div class="table" [class.loading]="loading">
        <div class="row header">
          <div class="cell checkbox">
            <input type="checkbox" [checked]="allSelected" (change)="toggleSelectAll($event)" [disabled]="!canWrite" />
          </div>
          <div class="cell">Name</div>
          <div class="cell type">Type</div>
          <div class="cell size">Size</div>
          <div class="cell">Last Modified</div>
          <div class="cell actions">Actions</div>
        </div>

        <div class="row" *ngFor="let folder of folders">
          <div class="cell checkbox">
            <input type="checkbox" [checked]="isSelected(folder.fullPath)" (change)="toggle(folder.fullPath, $event)" [disabled]="!canWrite" />
          </div>
          <div class="cell"><span class="chip">Folder</span>{{ folder.name }}</div>
          <div class="cell type">Folder</div>
          <div class="cell size">-</div>
          <div class="cell">-</div>
          <div class="cell actions">
            <button (click)="navigate.emit(folder.fullPath); $event.stopPropagation();">Open</button>
          </div>
        </div>

        <div class="row" *ngFor="let obj of objects">
          <div class="cell checkbox">
            <input type="checkbox" [checked]="isSelected(obj.key)" (change)="toggle(obj.key, $event)" [disabled]="!canWrite" />
          </div>
          <div class="cell">{{ obj.name }}</div>
          <div class="cell type">File</div>
          <div class="cell size">{{ humanSize(obj.sizeBytes) }}</div>
          <div class="cell">{{ obj.lastModified | date:'medium' }}</div>
          <div class="cell actions">
            <button (click)="download.emit(obj.key); $event.stopPropagation();">Download</button>
          </div>
        </div>

        <div class="empty" *ngIf="!loading && folders.length === 0 && objects.length === 0">No objects</div>
      </div>
      <div class="pagination" *ngIf="nextPageToken">
        <button (click)="loadMore.emit(nextPageToken)">Load more</button>
      </div>
    </div>
  `,
  styles: [
    `
      .browser { margin-top: 1rem; }
      .breadcrumb { margin-bottom: 0.5rem; color: #4a5c6a; }
      .crumb { cursor: pointer; color: #0b84ff; }
      .table { border: 1px solid #e0e4eb; border-radius: 10px; overflow: hidden; }
      .row { display: grid; grid-template-columns: 0.5fr 3fr 1fr 1fr 2fr 2fr; padding: 0.6rem 0.8rem; align-items: center; border-bottom: 1px solid #eef1f5; }
      .row.header { background: #f7f9fc; font-weight: 600; }
      .row:hover { background: #f3f7ff; }
      .row.loading { opacity: 0.5; }
      .cell.actions { display: flex; gap: 0.5rem; justify-content: flex-end; }
      .cell.checkbox { display: flex; justify-content: center; }
      button { padding: 0.35rem 0.65rem; border-radius: 8px; border: 1px solid #d5d8de; background: #0b84ff; color: #fff; cursor: pointer; }
      .chip { background: #eef5ff; color: #0b4bb3; padding: 0.15rem 0.4rem; border-radius: 6px; margin-right: 0.35rem; font-size: 0.8rem; }
      .empty { padding: 1rem; text-align: center; color: #5b6570; }
      .pagination { margin-top: 0.5rem; text-align: center; }
    `
  ]
})
export class ObjectBrowserComponent implements OnChanges {
  @Input() folders: FolderItem[] = [];
  @Input() objects: ObjectItem[] = [];
  @Input() currentPrefix = '';
  @Input() loading = false;
  @Input() nextPageToken?: string | null;
  @Input() selectedKeys: string[] = [];
  @Input() canWrite = true;
  @Output() navigate = new EventEmitter<string>();
  @Output() download = new EventEmitter<string>();
  @Output() selectionChange = new EventEmitter<string[]>();
  @Output() loadMore = new EventEmitter<string>();

  private selectedSet = new Set<string>();

  get breadcrumbs() {
    const parts = this.currentPrefix.split('/').filter(Boolean);
    const crumbs: { name: string; path: string }[] = [];
    let acc = '';
    for (const part of parts) {
      acc += part + '/';
      crumbs.push({ name: part, path: acc });
    }
    return crumbs;
  }

  humanSize(bytes: number): string {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let i = 0;
    let val = bytes;
    while (val >= 1024 && i < units.length - 1) {
      val /= 1024;
      i++;
    }
    return `${val.toFixed(1)} ${units[i]}`;
  }

  navigateTo(prefix: string) {
    this.navigate.emit(prefix);
  }

  isSelected(key: string): boolean {
    return this.selectedSet.has(key);
  }

  toggle(key: string, event: Event) {
    if (!this.canWrite) return;
    const checked = (event.target as HTMLInputElement).checked;
    const next = new Set(this.selectedSet);
    if (checked) {
      next.add(key);
    } else {
      next.delete(key);
    }
    this.selectedSet = next;
    this.selectionChange.emit(Array.from(this.selectedSet));
  }

  toggleSelectAll(event: Event) {
    if (!this.canWrite) return;
    const checked = (event.target as HTMLInputElement).checked;
    const allKeys = [...this.folders.map(f => f.fullPath), ...this.objects.map(o => o.key)];
    this.selectedSet = checked ? new Set(allKeys) : new Set<string>();
    this.selectionChange.emit(Array.from(this.selectedSet));
  }

  get allSelected(): boolean {
    const allKeys = [...this.folders.map(f => f.fullPath), ...this.objects.map(o => o.key)];
    return allKeys.length > 0 && allKeys.every(k => this.selectedSet.has(k));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedKeys']) {
      this.selectedSet = new Set(this.selectedKeys);
    }
    if (changes['canWrite'] && this.canWrite === false) {
      this.selectedSet = new Set<string>();
      this.selectionChange.emit([]);
    }
  }
}
