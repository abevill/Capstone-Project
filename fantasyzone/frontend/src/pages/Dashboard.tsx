import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import Async from '../components/Async';
import Card from '../components/Card';
import Table, { type Column } from '../components/Table';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../hooks/useFetch';
import { meApi } from '../services/api';
import { record, score } from '../utils/format';
import type { Team } from '../types';

export default function Dashboard() {
  const { user } = useAuth();
  const myLeagues = useFetch(() => meApi.leagues(), []);
  const myTeams = useFetch(() => meApi.teams(), []);

  const leagueNames = useMemo(
    () => new Map((myLeagues.data ?? []).map((l) => [l.id, l.name] as const)),
    [myLeagues.data],
  );

  const teamColumns: Column<Team>[] = [
    { header: 'Team', render: (t) => <Link to={`/team/${t.id}/roster`}>{t.name}</Link> },
    { header: 'League', render: (t) => <Link to={`/leagues/${t.leagueId}`}>{leagueNames.get(t.leagueId) ?? `League ${t.leagueId}`}</Link> },
    { header: 'Record', render: (t) => record(t.wins, t.losses, t.ties) },
    { header: 'Points', render: (t) => score(t.pointsFor) },
  ];

  return (
    <>
      <h1>Welcome back, {user?.displayName}</h1>
      <div className="row-actions">
        <Link to="/leagues/create" className="btn btn-primary">Create a league</Link>
        <Link to="/leagues" className="btn btn-secondary">Find a league</Link>
        <Link to="/players" className="btn btn-secondary">Browse players</Link>
      </div>

      <div className="grid">
        <Card title="My teams">
          <Async state={myTeams}>
            {(teams) => <Table caption="My teams" columns={teamColumns} rows={teams} rowKey={(t) => t.id}
                               emptyText="You have no teams yet. Open a league and create one." />}
          </Async>
        </Card>
        <Card title="My leagues">
          <Async state={myLeagues}>
            {(leagues) => leagues.length === 0
              ? <p className="muted">You have not joined a league yet.</p>
              : <ul>{leagues.map((l) => (
                  <li key={l.id}>
                    <Link to={`/leagues/${l.id}`}>{l.name}</Link>{' '}
                    <span className="muted">({l.season}, {l.memberCount} members)</span>
                    {l.joinCode && <div>Join code: <strong className="code">{l.joinCode}</strong></div>}
                  </li>))}</ul>}
          </Async>
        </Card>
      </div>
    </>
  );
}
