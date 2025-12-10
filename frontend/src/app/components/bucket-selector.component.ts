import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Bucket } from '../models/bucket';

@Component({
  selector: 'app-bucket-selector',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bucket-selector">
      <label for="bucket">Bucket</label>
      <select id="bucket" class="bucket-select" [ngModel]="selectedId" (ngModelChange)="onChange($event)">
        <option *ngFor="let bucket of buckets" [value]="bucket.id">{{ bucket.name }}</option>
      </select>
    </div>
  `,
  styles: [
    `
      .bucket-selector { display: flex; gap: 0.75rem; align-items: center; }
      .bucket-select { padding: 0.4rem 0.6rem; border-radius: 6px; border: 1px solid #ccc; background: #f7f8fb; }
    `
  ]
})
export class BucketSelectorComponent {
  @Input() buckets: Bucket[] = [];
  @Input() selectedId = '';
  @Output() bucketChange = new EventEmitter<string>();

  onChange(id: string) {
    this.bucketChange.emit(id);
  }
}
