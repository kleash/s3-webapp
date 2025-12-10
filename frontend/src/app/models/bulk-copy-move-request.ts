import { BulkCopyMoveItem } from './bulk-copy-move-item';

export interface BulkCopyMoveRequest {
  items: BulkCopyMoveItem[];
  overwrite: boolean;
}
