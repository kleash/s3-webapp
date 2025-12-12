import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BucketSelectorComponent } from './bucket-selector.component';
import { SearchBarComponent } from './search-bar.component';
import { ObjectBrowserComponent } from './object-browser.component';
import { OperationsPanelComponent } from './operations-panel.component';
import { ApiService } from '../services/api.service';
import { Bucket } from '../models/bucket';
import { FolderItem } from '../models/folder-item';
import { ObjectItem } from '../models/object-item';
import { FolderSizeResponse } from '../models/folder-size-response';
import { BulkCopyMoveRequest } from '../models/bulk-copy-move-request';
import { BulkOperationResult } from '../models/bulk-operation-result';
import { FolderCopyRequest } from '../models/folder-copy-request';
import { FolderSizeEvent } from '../models/folder-size-event';
import { FolderSizeJob } from '../models/folder-size-job';
import { forkJoin, Subscription } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { AccessLevel } from '../models/user-session';

@Component({
  selector: 'app-browser',
  standalone: true,
  imports: [CommonModule, FormsModule, BucketSelectorComponent, SearchBarComponent, ObjectBrowserComponent, OperationsPanelComponent],
  templateUrl: './browser.component.html',
  styleUrls: ['./browser.component.scss']
})
export class BrowserComponent implements OnInit, OnDestroy {
  title = 'S3 Browser';
  buckets: Bucket[] = [];
  selectedBucketId = '';
  currentPrefix = '';
  folders: FolderItem[] = [];
  objects: ObjectItem[] = [];
  nextPageToken: string | null = null;
  loading = false;
  selectedKey = '';
  selectedKeys: string[] = [];
  bulkTargetPrefix = '';
  bulkOverwrite = false;
  statusMessage = '';
  folderSizeInfo?: FolderSizeResponse;
  folderSizeJob?: FolderSizeJob;
  folderSizeStream?: Subscription;
  folderSizeLoading = false;
  isSearchMode = false;

  constructor(private api: ApiService, private auth: AuthService) {}

  ngOnInit() {
    this.loadBuckets();
  }

  ngOnDestroy() {
    this.clearFolderSizeState();
  }

  get accessLevel(): AccessLevel | null {
    return this.auth.currentUser?.accessLevel ?? null;
  }

  get accessLabel(): string {
    if (this.accessLevel === 'READ_WRITE') return 'Read & write';
    if (this.accessLevel === 'READ_ONLY') return 'Read-only';
    return '';
  }

  get canWrite(): boolean {
    return this.accessLevel === 'READ_WRITE';
  }

  loadBuckets() {
    this.api.getBuckets().subscribe({
      next: buckets => {
        this.buckets = buckets;
        if (buckets.length) {
          this.onBucketChange(buckets[0].id);
        }
      },
      error: () => this.statusMessage = 'Failed to load buckets'
    });
  }

  onBucketChange(id: string) {
    this.selectedBucketId = id;
    this.currentPrefix = '';
    this.selectedKey = '';
    this.isSearchMode = false;
    this.clearFolderSizeState();
    this.loadObjects('');
  }

  onSearch(event: { query: string; withinPrefix: boolean }) {
    if (!event.query) {
      return this.resetSearch();
    }
    this.loading = true;
    this.isSearchMode = true;
    this.clearFolderSizeState();
    const prefix = event.withinPrefix ? this.currentPrefix : '';
    this.api.search(this.selectedBucketId, event.query, prefix).subscribe({
      next: res => {
        this.folders = res.folders || [];
        this.objects = res.objects || [];
        this.nextPageToken = null;
        this.folderSizeInfo = undefined;
        this.loading = false;
      },
      error: () => {
        this.statusMessage = 'Search failed';
        this.loading = false;
      }
    });
  }

  resetSearch() {
    this.isSearchMode = false;
    this.loadObjects(this.currentPrefix);
  }

  loadObjects(prefix: string, pageToken?: string, append = false) {
    this.loading = true;
    if (!append) {
      this.clearFolderSizeState();
    } else {
      this.folderSizeInfo = undefined;
    }
    this.api.listObjects(this.selectedBucketId, prefix, pageToken).subscribe({
      next: res => {
        this.currentPrefix = res.currentPrefix || '';
        this.folders = append ? [...this.folders, ...res.folders] : res.folders || [];
        this.objects = append ? [...this.objects, ...res.objects] : res.objects || [];
        this.nextPageToken = res.nextPageToken || null;
        if (!append) {
          this.selectedKey = '';
          this.selectedKeys = [];
        }
        this.loading = false;
      },
      error: () => {
        this.statusMessage = 'Failed to load objects';
        this.loading = false;
      }
    });
  }

  navigateTo(prefix: string) {
    this.isSearchMode = false;
    this.selectedKey = '';
    this.selectedKeys = [];
    this.loadObjects(prefix);
  }

  onSelectionChange(keys: string[]) {
    this.selectedKeys = keys;
    this.selectedKey = keys[0] || '';
  }

  download(key: string) {
    this.api.download(this.selectedBucketId, key).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = key.split('/').pop() || 'download';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.statusMessage = 'Download failed'
    });
  }

  copy(req: { sourceKey: string; targetKey: string; overwrite: boolean }) {
    if (!this.canWrite) {
      this.statusMessage = 'You have read-only access';
      return;
    }
    this.api.copy(this.selectedBucketId, req).subscribe({
      next: () => {
        this.statusMessage = 'Copied successfully';
        this.loadObjects(this.currentPrefix);
      },
      error: () => this.statusMessage = 'Copy failed'
    });
  }

  move(req: { sourceKey: string; targetKey: string; overwrite: boolean }) {
    if (!this.canWrite) {
      this.statusMessage = 'You have read-only access';
      return;
    }
    this.api.move(this.selectedBucketId, req).subscribe({
      next: () => {
        this.statusMessage = 'Moved successfully';
        this.loadObjects(this.currentPrefix);
      },
      error: () => this.statusMessage = 'Move failed'
    });
  }

  deleteObject(key: string) {
    if (!this.canWrite) {
      this.statusMessage = 'You have read-only access';
      return;
    }
    this.api.deleteObjects(this.selectedBucketId, [key]).subscribe({
      next: () => {
        this.statusMessage = 'Deleted';
        this.loadObjects(this.currentPrefix);
      },
      error: () => this.statusMessage = 'Delete failed'
    });
  }

  deleteFolder(prefix: string) {
    if (!this.canWrite) {
      this.statusMessage = 'You have read-only access';
      return;
    }
    this.api.deleteFolder(this.selectedBucketId, prefix).subscribe({
      next: () => {
        this.statusMessage = `Folder ${prefix} deleted`;
        if (this.currentPrefix.startsWith(prefix)) {
          this.navigateTo('');
        } else {
          this.loadObjects(this.currentPrefix);
        }
      },
      error: () => this.statusMessage = 'Failed to delete folder'
    });
  }

  requestFolderSize(prefix: string) {
    if (!prefix) return;
    this.clearFolderSizeState();
    this.folderSizeLoading = true;
    this.api.startFolderSize(this.selectedBucketId, prefix).subscribe({
      next: res => {
        this.folderSizeJob = res.job;
        this.folderSizeStream = this.api.streamFolderSize(res.websocketPath).subscribe({
          next: event => this.handleFolderSizeEvent(event),
          error: () => {
            this.statusMessage = 'Size stream failed';
            this.folderSizeLoading = false;
          },
          complete: () => {
            this.folderSizeLoading = false;
            this.folderSizeStream = undefined;
          }
        });
      },
      error: () => {
        this.statusMessage = 'Size lookup failed';
        this.folderSizeLoading = false;
      }
    });
  }

  loadMore(token: string) {
    this.loadObjects(this.currentPrefix, token, true);
  }

  bulkDelete() {
    if (!this.canWrite) {
      this.statusMessage = 'You have read-only access';
      return;
    }
    const { files, folders } = this.splitSelection();
    if (!files.length && !folders.length) {
      this.statusMessage = 'No items selected';
      return;
    }
    this.api.deleteObjects(this.selectedBucketId, files, folders).subscribe({
      next: (res: any) => {
        const count = Array.isArray(res) ? res.length : (res?.deleted?.length || 0);
        this.statusMessage = `Deleted ${count} items`;
        this.loadObjects(this.currentPrefix);
      },
      error: () => this.statusMessage = 'Bulk delete failed'
    });
  }

  bulkCopy() {
    this.runBulkCopyMove(false);
  }

  bulkMove() {
    this.runBulkCopyMove(true);
  }

  cancelFolderSize() {
    if (!this.folderSizeJob) return;
    this.api.cancelFolderSize(this.selectedBucketId, this.folderSizeJob.id).subscribe({
      next: job => {
        this.folderSizeJob = job;
        this.folderSizeLoading = false;
        this.folderSizeStream?.unsubscribe();
        this.folderSizeStream = undefined;
      },
      error: () => this.statusMessage = 'Cancel failed'
    });
  }

  isFolderSizeRunning(): boolean {
    return !!this.folderSizeJob && (this.folderSizeJob.status === 'QUEUED' || this.folderSizeJob.status === 'RUNNING');
  }

  private handleFolderSizeEvent(event: FolderSizeEvent) {
    this.folderSizeJob = event.job;
    if (event.job.status === 'COMPLETED') {
      this.folderSizeInfo = {
        prefix: event.job.prefix,
        objectCount: event.job.objectsScanned,
        totalSizeBytes: event.job.totalSizeBytes,
        partial: event.job.partial,
        message: event.job.message
      };
      this.folderSizeLoading = false;
    } else if (event.job.status === 'FAILED') {
      this.folderSizeInfo = undefined;
      this.statusMessage = event.job.message || 'Folder size failed';
      this.folderSizeLoading = false;
    } else if (event.job.status === 'CANCELED') {
      this.folderSizeInfo = undefined;
      this.statusMessage = 'Folder size canceled';
      this.folderSizeLoading = false;
    }
  }

  private clearFolderSizeState() {
    if (this.folderSizeStream) {
      this.folderSizeStream.unsubscribe();
      this.folderSizeStream = undefined;
    }
    this.folderSizeJob = undefined;
    this.folderSizeInfo = undefined;
    this.folderSizeLoading = false;
  }

  private runBulkCopyMove(isMove: boolean) {
    if (!this.canWrite) {
      this.statusMessage = 'You have read-only access';
      return;
    }
    const { files, folders } = this.splitSelection();
    if (!files.length && !folders.length) {
      this.statusMessage = 'No items selected';
      return;
    }
    const targetPrefix = this.normalizePrefix(this.bulkTargetPrefix);
    if (!targetPrefix) {
      this.statusMessage = 'Target prefix required';
      return;
    }

    const ops: any[] = [];
    if (files.length) {
      const items = files.map(key => ({
        sourceKey: key,
        targetKey: targetPrefix + (key.split('/').pop() || key),
      }));
      const body: BulkCopyMoveRequest = { items, overwrite: this.bulkOverwrite };
      ops.push(isMove ? this.api.bulkMove(this.selectedBucketId, body) : this.api.bulkCopy(this.selectedBucketId, body));
    }

    folders.forEach(prefix => {
      const body: FolderCopyRequest = {
        sourcePrefix: prefix,
        targetPrefix: targetPrefix,
        overwrite: this.bulkOverwrite
      };
      ops.push(isMove ? this.api.moveFolder(this.selectedBucketId, body) : this.api.copyFolder(this.selectedBucketId, body));
    });

    forkJoin(ops).subscribe({
      next: results => {
        let copied = 0;
        let skipped = 0;
        let failed = 0;
        results.forEach(r => {
          if (Array.isArray(r)) {
            (r as BulkOperationResult[]).forEach(item => {
              if (item.success) copied++; else failed++;
            });
          } else if (r && 'copied' in r) {
            copied += r.copied || 0;
            skipped += r.skipped || 0;
            failed += (r.errors || []).length;
          }
        });
        this.statusMessage = `${isMove ? 'Moved' : 'Copied'}: ${copied} ok, ${skipped} skipped, ${failed} failed`;
        this.bulkTargetPrefix = '';
        this.bulkOverwrite = false;
        this.loadObjects(this.currentPrefix);
      },
      error: () => this.statusMessage = `Bulk ${isMove ? 'move' : 'copy'} failed`
    });
  }

  private splitSelection(): { files: string[]; folders: string[] } {
    const folders = this.selectedKeys.filter(k => k.endsWith('/'));
    const files = this.selectedKeys.filter(k => !k.endsWith('/'));
    return { files, folders };
  }

  private normalizePrefix(prefix: string): string {
    if (!prefix) return '';
    return prefix.endsWith('/') ? prefix : prefix + '/';
  }
}
