export type AccessLevel = 'READ_ONLY' | 'READ_WRITE';

export interface UserSession {
  username: string;
  accessLevel: AccessLevel;
}
