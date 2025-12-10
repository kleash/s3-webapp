import { Component, EventEmitter, Output, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <form class="search" (ngSubmit)="submit()">
      <input
        type="text"
        placeholder="Search e.g. trade_2025_*.csv"
        [(ngModel)]="query"
        name="query"
      />
      <label class="checkbox">
        <input type="checkbox" [(ngModel)]="withinPrefix" name="withinPrefix" />
        Current folder only
      </label>
      <button type="submit">Search</button>
      <button type="button" class="secondary" (click)="clear()">Reset</button>
    </form>
  `,
  styles: [
    `
      .search { display: flex; gap: 0.5rem; align-items: center; }
      input[type='text'] { flex: 1; padding: 0.5rem 0.7rem; border-radius: 8px; border: 1px solid #d0d0d0; }
      button { padding: 0.5rem 0.9rem; border-radius: 8px; border: none; background: #0b84ff; color: #fff; }
      .secondary { background: #edf1f7; color: #0b1c2c; border: 1px solid #d0d0d0; }
      .checkbox { display: flex; align-items: center; gap: 0.35rem; font-size: 0.9rem; }
    `
  ]
})
export class SearchBarComponent {
  @Output() search = new EventEmitter<{ query: string; withinPrefix: boolean }>();
  @Output() reset = new EventEmitter<void>();
  @Input() query = '';
  withinPrefix = true;

  submit() {
    this.search.emit({ query: this.query, withinPrefix: this.withinPrefix });
  }

  clear() {
    this.query = '';
    this.reset.emit();
  }
}
