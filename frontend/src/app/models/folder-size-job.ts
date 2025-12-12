export type FolderSizeStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELED';

export interface FolderSizeJob {
  id: string;
  bucketId: string;
  prefix: string;
  status: FolderSizeStatus;
  objectsScanned: number;
  totalSizeBytes: number;
  partial: boolean;
  partialReason?: string;
  message?: string;
  startedAt?: string;
  finishedAt?: string;
}
