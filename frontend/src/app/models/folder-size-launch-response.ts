import { FolderSizeJob } from './folder-size-job';

export interface FolderSizeLaunchResponse {
  job: FolderSizeJob;
  websocketPath: string;
}
