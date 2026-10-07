import type {
  AuthUser, League, Member, Team, RosterEntry, RosterTransaction, Matchup, Standing, Player, UserSummary, Page, PlayerQuery, SyncResult,
} from '../types';

const BASE: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;
  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

let token: string | null = null;
let onUnauthorized: (() => void) | null = null;
export const setToken = (t: string | null) => { token = t; };
export const setUnauthorizedHandler = (fn: (() => void) | null) => { onUnauthorized = fn; };

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (options.body) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(`${BASE}${path}`, { ...options, headers });
  } catch {
    throw new ApiError('Cannot reach the server. Please try again.', 0);
  }
  if (res.status === 204) return undefined as T;

  const body = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && token && onUnauthorized) onUnauthorized(); // expired or invalid session
    throw new ApiError(body?.message ?? 'Request failed', res.status, body?.fieldErrors);
  }
  return body as T;
}



const json = (body: unknown): RequestInit => ({ body: JSON.stringify(body) });

export const authApi = {
  register: (b: { email: string; displayName: string; password: string }) =>
    request<AuthUser>('/api/auth/register', { method: 'POST', ...json(b) }),
  login: (b: { email: string; password: string }) =>
    request<AuthUser>('/api/auth/login', { method: 'POST', ...json(b) }),
};

export const leaguesApi = {
  list: (page = 0, size = 20) => request<Page<League>>(`/api/leagues?page=${page}&size=${size}`),
  get: (id: number) => request<League>(`/api/leagues/${id}`),
  standings: (id: number) => request<Standing[]>(`/api/leagues/${id}/standings`),
  create: (b: { name: string; season: number }) => request<League>('/api/leagues', { method: 'POST', ...json(b) }),
  join: (joinCode: string) => request<League>('/api/leagues/join', { method: 'POST', ...json({ joinCode }) }),
  members: (id: number) => request<Member[]>(`/api/leagues/${id}/members`),
  removeMember: (id: number, userId: number) => request<void>(`/api/leagues/${id}/members/${userId}`, { method: 'DELETE' }),
  lock: (id: number, locked: boolean) => request<League>(`/api/leagues/${id}/lock?locked=${locked}`, { method: 'PUT' }),
  teams: (id: number) => request<Team[]>(`/api/leagues/${id}/teams`),
  createTeam: (id: number, name: string) => request<Team>(`/api/leagues/${id}/teams`, { method: 'POST', ...json({ name }) }),
  matchups: (id: number) => request<Matchup[]>(`/api/leagues/${id}/matchups`),
  generate: (id: number) => request<Matchup[]>(`/api/leagues/${id}/matchups/generate`, { method: 'POST' }),
};

export const matchupsApi = {
  calculate: (id: number) => request<Matchup>(`/api/matchups/${id}/calculate`, { method: 'PUT' }),
};

export const teamsApi = {
  get: (id: number) => request<Team>(`/api/teams/${id}`),
  roster: (id: number) => request<RosterEntry[]>(`/api/teams/${id}/roster`),
  addPlayer: (teamId: number, playerId: number, positionSlot: string) =>
    request<RosterEntry>(`/api/teams/${teamId}/roster/players/${playerId}`, { method: 'POST', ...json({ positionSlot }) }),
  dropPlayer: (teamId: number, playerId: number) =>
    request<void>(`/api/teams/${teamId}/roster/players/${playerId}`, { method: 'DELETE' }),
  transactions: (id: number) => request<RosterTransaction[]>(`/api/teams/${id}/transactions`),
};

export const meApi = {
  leagues: () => request<League[]>('/api/me/leagues'),
  teams: () => request<Team[]>('/api/me/teams'),
};

export const playersApi = {
  list: (q: PlayerQuery) => {
    const p = new URLSearchParams();
    if (q.name) p.set('name', q.name);
    if (q.position) p.set('position', q.position);
    if (q.team) p.set('team', q.team);
    p.set('page', String(q.page ?? 0));
    return request<Page<Player>>(`/api/players?${p.toString()}`);
  },
  get: (id: number) =>
      request<Player>(`/api/players/${id}`),

  getStats: (playerId: number) =>
      request<any[]>(`/api/players/${playerId}/stats`)
};

export const adminApi = {
  users: (page = 0) => request<Page<UserSummary>>(`/api/admin/users?page=${page}`),
  setStatus: (id: number, enabled: boolean) =>
    request<UserSummary>(`/api/admin/users/${id}/status?enabled=${enabled}`, { method: 'PUT' }),
  syncPlayers: () => request<SyncResult>('/api/admin/players/sync', { method: 'POST' }),
  syncStats: (season?: number, week?: number) => {
    const p = new URLSearchParams();
    if (season !== undefined && week !== undefined) {
      p.set('season', String(season));
      p.set('week', String(week));
    }
    const q = p.toString();
    return request<SyncResult>(`/api/admin/stats/sync${q ? `?${q}` : ''}`, { method: 'POST' });
  },
};


