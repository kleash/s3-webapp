import { BulkOperationResult } from './bulk-operation-result';

export interface FolderOperationResult {
  sourcePrefix: string;
  targetPrefix: string;
  totalObjects: number;
  copied: number;
  skipped: number;
  errors: BulkOperationResult[];
}
