export interface AuthUser { userId: number; token: string; email: string; displayName: string; roles: string[] }
export interface League { id: number; name: string; season: number; rostersLocked: boolean; memberCount: number; joinCode?: string }
export interface Member { userId: number; displayName: string; commissioner: boolean }
export interface Team { id: number; name: string; leagueId: number; owner: string; wins: number; losses: number; ties: number; pointsFor: number }
export interface RosterEntry { playerId: number; name: string; position: string; nflTeam: string | null; slot: string }
export interface Matchup {
  id: number; week: number;
  homeTeamId: number; homeTeam: string; awayTeamId: number; awayTeam: string;
  homeScore: number | null; awayScore: number | null; finalized: boolean;
}
export interface Standing { rank: number; teamId: number; teamName: string; owner: string; wins: number; losses: number; ties: number; pointsFor: number }
export interface Player { id: number; externalId: string | null; name: string; position: string; nflTeam: string | null }
export interface UserSummary { id: number; email: string; displayName: string; enabled: boolean; roles: string[] }
export interface Page<T> { content: T[]; totalPages: number; number: number; totalElements: number }
export interface PlayerQuery { name?: string; position?: string; team?: string; page?: number }
export interface RosterTransaction { id: number; type: 'ADD' | 'DROP'; playerId: number; playerName: string; position: string; slot: string | null; madeBy: string; at: string }
export interface SyncResult { playersCreated: number; playersUpdated: number; statLinesCreated: number; statLinesUpdated: number; skippedUnknownPlayers: number }

export interface WeeklyPlayerStats {
  season: number;
  week: number;

  passingYards: number;
  passingTouchdowns: number;
  interceptions: number;

  rushingYards: number;
  rushingTouchdowns: number;

  receptions: number;
  receivingYards: number;
  receivingTouchdowns: number;

  fumblesLost: number;
  fantasyPoints: number;
}

export interface PlayerStats {
  playerId: number;
  name: string;
  position: string;
  nflTeam: string | null;
  totalFantasyPoints: number;
  weeklyStats: WeeklyPlayerStats[];
}
