import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CopyMoveRequest } from '../models/copy-move-request';

@Component({
  selector: 'app-operations-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="panel">
      <h3>Operations</h3>
      <p class="hint">Selected: <strong>{{ selectedKey || 'None' }}</strong></p>

      <div class="block">
        <label>Target key</label>
        <input type="text" [(ngModel)]="targetKey" placeholder="folder/target.txt" name="targetKey" />
        <label class="checkbox">
          <input type="checkbox" [(ngModel)]="overwrite" name="overwrite" /> Allow overwrite
        </label>
        <div class="actions">
          <button [disabled]="!selectedKey || !targetKey" (click)="emitCopy(false)">Copy</button>
          <button [disabled]="!selectedKey || !targetKey" (click)="emitCopy(true)" class="alt">Move</button>
        </div>
      </div>

      <div class="block">
        <button [disabled]="!selectedKey" (click)="deleteObject.emit(selectedKey)">Delete object</button>
      </div>

      <div class="block">
        <label>Folder prefix</label>
        <input type="text" [(ngModel)]="folderPrefix" name="folderPrefix" />
        <div class="actions">
          <button (click)="deleteFolder.emit(folderPrefix)" [disabled]="!folderPrefix">Delete folder</button>
          <button (click)="folderSize.emit(folderPrefix)" [disabled]="!folderPrefix" class="alt">Folder size</button>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .panel { background: #0b1c2c; color: #f7faff; padding: 1rem; border-radius: 12px; min-width: 260px; }
      h3 { margin-top: 0; }
      .block { margin-top: 0.75rem; padding: 0.75rem; background: rgba(255,255,255,0.04); border-radius: 10px; }
      label { display: block; margin-bottom: 0.3rem; font-size: 0.9rem; color: #a0b2c0; }
      input { width: 100%; padding: 0.45rem 0.6rem; border-radius: 8px; border: 1px solid #2f4356; background: rgba(255,255,255,0.08); color: #fff; }
      .actions { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
      button { flex: 1; padding: 0.5rem 0.6rem; border-radius: 8px; border: none; background: #14b8ff; color: #0b1c2c; cursor: pointer; font-weight: 600; }
      button.alt { background: #ffe45e; color: #0b1c2c; }
      button:disabled { opacity: 0.5; cursor: not-allowed; }
      .checkbox { display: flex; align-items: center; gap: 0.35rem; margin-top: 0.35rem; }
      .hint strong { color: #fff; }
    `
  ]
})
export class OperationsPanelComponent implements OnChanges {
  @Input() selectedKey = '';
  @Input() currentPrefix = '';
  @Output() copy = new EventEmitter<CopyMoveRequest>();
  @Output() move = new EventEmitter<CopyMoveRequest>();
  @Output() deleteObject = new EventEmitter<string>();
  @Output() deleteFolder = new EventEmitter<string>();
  @Output() folderSize = new EventEmitter<string>();

  targetKey = '';
  overwrite = false;
  folderPrefix = '';

  ngOnChanges(changes: SimpleChanges) {
    if (this.selectedKey && this.selectedKey.endsWith('/')) {
      this.folderPrefix = this.selectedKey;
    } else if (this.currentPrefix) {
      this.folderPrefix = this.currentPrefix;
    }
  }

  emitCopy(isMove: boolean) {
    const payload: CopyMoveRequest = {
      sourceKey: this.selectedKey,
      targetKey: this.targetKey,
      overwrite: this.overwrite
    };
    (isMove ? this.move : this.copy).emit(payload);
  }
}
