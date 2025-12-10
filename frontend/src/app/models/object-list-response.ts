import { FolderItem } from './folder-item';
import { ObjectItem } from './object-item';

export interface ObjectListResponse {
  currentPrefix: string;
  folders: FolderItem[];
  objects: ObjectItem[];
  nextPageToken?: string | null;
}
