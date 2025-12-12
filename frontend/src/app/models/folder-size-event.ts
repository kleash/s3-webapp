import { FolderSizeJob } from './folder-size-job';

export type FolderSizeEventType = 'SNAPSHOT' | 'STARTED' | 'PROGRESS' | 'PARTIAL' | 'COMPLETED' | 'FAILED' | 'CANCELED';

export interface FolderSizeEvent {
  event: FolderSizeEventType;
  job: FolderSizeJob;
}
