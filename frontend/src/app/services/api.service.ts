import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Bucket } from '../models/bucket';
import { ObjectListResponse } from '../models/object-list-response';
import { CopyMoveRequest } from '../models/copy-move-request';
import { FolderSizeResponse } from '../models/folder-size-response';
import { BulkCopyMoveRequest } from '../models/bulk-copy-move-request';
import { BulkOperationResult } from '../models/bulk-operation-result';
import { FolderCopyRequest } from '../models/folder-copy-request';
import { FolderOperationResult } from '../models/folder-operation-result';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  getBuckets(): Observable<Bucket[]> {
    return this.http.get<Bucket[]>(`${this.baseUrl}/buckets`);
  }

  listObjects(bucketId: string, prefix: string = '', pageToken?: string): Observable<ObjectListResponse> {
    let params = new HttpParams();
    if (prefix) params = params.set('prefix', prefix);
    if (pageToken) params = params.set('pageToken', pageToken);
    return this.http.get<ObjectListResponse>(`${this.baseUrl}/buckets/${bucketId}/objects`, { params });
  }

  search(bucketId: string, query: string, prefix: string = ''): Observable<ObjectListResponse> {
    let params = new HttpParams().set('query', query);
    if (prefix) params = params.set('prefix', prefix);
    return this.http.get<ObjectListResponse>(`${this.baseUrl}/buckets/${bucketId}/search`, { params });
  }

  download(bucketId: string, key: string): Observable<Blob> {
    const params = new HttpParams().set('key', key);
    return this.http.get(`${this.baseUrl}/buckets/${bucketId}/objects/download`, {
      params,
      responseType: 'blob'
    });
  }

  copy(bucketId: string, body: CopyMoveRequest) {
    return this.http.post(`${this.baseUrl}/buckets/${bucketId}/objects/copy`, body);
  }

  move(bucketId: string, body: CopyMoveRequest) {
    return this.http.post(`${this.baseUrl}/buckets/${bucketId}/objects/move`, body);
  }

  bulkCopy(bucketId: string, body: BulkCopyMoveRequest) {
    return this.http.post<BulkOperationResult[]>(`${this.baseUrl}/buckets/${bucketId}/objects/bulk-copy`, body);
  }

  bulkMove(bucketId: string, body: BulkCopyMoveRequest) {
    return this.http.post<BulkOperationResult[]>(`${this.baseUrl}/buckets/${bucketId}/objects/bulk-move`, body);
  }

  copyFolder(bucketId: string, body: FolderCopyRequest) {
    return this.http.post<FolderOperationResult>(`${this.baseUrl}/buckets/${bucketId}/folders/copy`, body);
  }

  moveFolder(bucketId: string, body: FolderCopyRequest) {
    return this.http.post<FolderOperationResult>(`${this.baseUrl}/buckets/${bucketId}/folders/move`, body);
  }

  deleteObjects(bucketId: string, keys: string[], prefixes: string[] = []) {
    return this.http.request('delete', `${this.baseUrl}/buckets/${bucketId}/objects`, {
      body: { keys, prefixes }
    });
  }

  deleteFolder(bucketId: string, prefix: string) {
    return this.http.request('delete', `${this.baseUrl}/buckets/${bucketId}/folders`, {
      body: { prefix }
    });
  }

  folderSize(bucketId: string, prefix: string): Observable<FolderSizeResponse> {
    const params = new HttpParams().set('prefix', prefix);
    return this.http.get<FolderSizeResponse>(`${this.baseUrl}/buckets/${bucketId}/folders/size`, { params });
  }
}
