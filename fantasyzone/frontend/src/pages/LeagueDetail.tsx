import { useCallback, useMemo, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import Async from '../components/Async';
import Button from '../components/Button';
import Card from '../components/Card';
import Input from '../components/Input';
import JoinLeagueForm from '../components/JoinLeagueForm';
import Select from '../components/Select';
import Table, { type Column } from '../components/Table';
import { ErrorMessage } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../hooks/useFetch';
import { leaguesApi, matchupsApi, meApi } from '../services/api';
import { errMsg, record, score } from '../utils/format';
import type { League, Matchup, Member, Standing, Team } from '../types';

type Tab = 'standings' | 'teams' | 'matchups' | 'members';

export default function LeagueDetail() {
  const id = Number(useParams().leagueId);
  const { user, isAdmin } = useAuth();
  const [tab, setTab] = useState<Tab>('standings');
  const [week, setWeek] = useState('all');
  const [teamName, setTeamName] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const league = useFetch(() => leaguesApi.get(id), [id]);
  const standings = useFetch(() => leaguesApi.standings(id), [id]);
  const myLeagues = useFetch(() => (user ? meApi.leagues() : Promise.resolve([] as League[])), [user?.userId]);
  const myTeams = useFetch(() => (user ? meApi.teams() : Promise.resolve([] as Team[])), [user?.userId]);

  const isMember = !!user && (isAdmin || (myLeagues.data ?? []).some((l) => l.id === id));
  const members = useFetch(() => (isMember ? leaguesApi.members(id) : Promise.resolve([] as Member[])), [id, isMember]);
  const teams = useFetch(() => (isMember ? leaguesApi.teams(id) : Promise.resolve([] as Team[])), [id, isMember]);
  const matchups = useFetch(() => (isMember ? leaguesApi.matchups(id) : Promise.resolve([] as Matchup[])), [id, isMember]);

  const isCommissioner = isAdmin || (members.data ?? []).some((m) => m.userId === user?.userId && m.commissioner);
  const myTeam = (myTeams.data ?? []).find((t) => t.leagueId === id);

  const weeks = useMemo(
    () => Array.from(new Set((matchups.data ?? []).map((m) => m.week))).sort((a, b) => a - b),
    [matchups.data],
  );
  const visibleMatchups = useMemo(
    () => (matchups.data ?? []).filter((m) => week === 'all' || m.week === Number(week)),
    [matchups.data, week],
  );

  const run = useCallback(async (action: () => Promise<unknown>, refresh: Array<() => void>) => {
    setBusy(true);
    setActionError(null);
    try {
      await action();
      refresh.forEach((fn) => fn());
    } catch (e) {
      setActionError(errMsg(e, 'That action failed'));
    } finally {
      setBusy(false);
    }
  }, []);

  const refreshAll = () => [league.reload, standings.reload, teams.reload, members.reload, matchups.reload, myTeams.reload];

  async function createTeam(e: FormEvent) {
    e.preventDefault();
    if (!teamName.trim()) { setActionError('Give your team a name'); return; }
    await run(() => leaguesApi.createTeam(id, teamName.trim()), refreshAll());
    setTeamName('');
  }

  const standingColumns: Column<Standing>[] = [
    { header: '#', render: (s) => s.rank },
    { header: 'Team', render: (s) => (isMember ? <Link to={`/team/${s.teamId}/roster`}>{s.teamName}</Link> : s.teamName) },
    { header: 'Manager', render: (s) => s.owner },
    { header: 'Record', render: (s) => record(s.wins, s.losses, s.ties) },
    { header: 'Points', render: (s) => score(s.pointsFor) },
  ];
  const teamColumns: Column<Team>[] = [
    { header: 'Team', render: (t) => <Link to={`/team/${t.id}/roster`}>{t.name}</Link> },
    { header: 'Manager', render: (t) => t.owner },
    { header: 'Record', render: (t) => record(t.wins, t.losses, t.ties) },
  ];
  const matchupColumns: Column<Matchup>[] = [
    { header: 'Week', render: (m) => m.week },
    { header: 'Home', render: (m) => m.homeTeam },
    { header: 'Score', render: (m) => `${score(m.homeScore)} : ${score(m.awayScore)}` },
    { header: 'Away', render: (m) => m.awayTeam },
    {
      header: 'Status',
      render: (m) => m.finalized
        ? 'Final'
        : isCommissioner
          ? <Button variant="secondary" loading={busy} onClick={() => run(() => matchupsApi.calculate(m.id), refreshAll())}>Calculate score</Button>
          : 'Scheduled',
    },
  ];
  const memberColumns: Column<Member>[] = [
    { header: 'Member', render: (m) => m.displayName },
    { header: 'Role', render: (m) => (m.commissioner ? 'Commissioner' : 'Member') },
    {
      header: 'Actions',
      render: (m) => isCommissioner && !m.commissioner
        ? <Button variant="danger" loading={busy} onClick={() => run(() => leaguesApi.removeMember(id, m.userId), refreshAll())}>Remove</Button>
        : null,
    },
  ];

  const tabs: { key: Tab; label: string }[] = [
    { key: 'standings', label: 'Standings' },
    ...(isMember ? [
      { key: 'teams' as Tab, label: 'Teams' },
      { key: 'matchups' as Tab, label: 'Matchups' },
      { key: 'members' as Tab, label: 'Members' },
    ] : []),
  ];

  return (
    <>
      <Async state={league} lines={2}>
        {(l) => (
          <header className="page-head">
            <h1>{l.name}</h1>
            <p className="muted">
              Season {l.season} - {l.memberCount} members - {l.rostersLocked ? 'Rosters locked' : 'Rosters open'}
            </p>
          </header>
        )}
      </Async>

      {actionError && <ErrorMessage message={actionError} />}

      {!user && (
        <Card><p><Link to="/login">Log in</Link> or <Link to="/register">sign up</Link> to join this league and see rosters and matchups.</p></Card>
      )}

      {user && !isMember && !myLeagues.loading && (
        <Card title="Join this league">
          <JoinLeagueForm onJoined={() => myLeagues.reload()} />
        </Card>
      )}

      {isMember && !myTeam && !myTeams.loading && (
        <Card title="Create your team">
          <form onSubmit={createTeam} noValidate>
            <Input label="Team name" value={teamName} onChange={(e) => setTeamName(e.target.value)} maxLength={60} />
            <Button type="submit" loading={busy}>Create team</Button>
          </form>
        </Card>
      )}
      {isMember && myTeam && (
        <p><Link to={`/team/${myTeam.id}/roster`} className="btn btn-primary">Manage my roster ({myTeam.name})</Link></p>
      )}

      {isCommissioner && league.data && (
        <Card title="Commissioner tools">
          <div className="row-actions">
            <Button variant="secondary" loading={busy}
                    onClick={() => run(() => leaguesApi.lock(id, !league.data!.rostersLocked), [league.reload])}>
              {league.data.rostersLocked ? 'Unlock rosters' : 'Lock rosters'}
            </Button>
            <Button variant="secondary" loading={busy}
                    onClick={() => run(() => leaguesApi.generate(id), [matchups.reload])}>
              Generate matchups
            </Button>
          </div>
          <p className="muted">Matchups need an even number of teams. Calculate each matchup's score from the Matchups tab.</p>
        </Card>
      )}

      <div role="tablist" aria-label="League sections" className="tabs">
        {tabs.map((t) => (
          <button key={t.key} type="button" role="tab" id={`tab-${t.key}`} aria-selected={tab === t.key}
                  aria-controls="tabpanel" className={tab === t.key ? 'tab active' : 'tab'} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" id="tabpanel" aria-labelledby={`tab-${tab}`} className="card">
        {tab === 'standings' && (
          <Async state={standings}>
            {(rows) => <Table caption="League standings" columns={standingColumns} rows={rows} rowKey={(s) => s.teamId}
                               emptyText="No teams yet." />}
          </Async>
        )}
        {tab === 'teams' && (
          <Async state={teams}>
            {(rows) => <Table caption="Teams" columns={teamColumns} rows={rows} rowKey={(t) => t.id} emptyText="No teams yet." />}
          </Async>
        )}
        {tab === 'matchups' && (
          <Async state={matchups}>
            {() => (
              <>
                {weeks.length > 0 && (
                  <Select label="Week" value={week} onChange={(e) => setWeek(e.target.value)}
                          options={[{ value: 'all', label: 'All weeks' }, ...weeks.map((w) => ({ value: String(w), label: `Week ${w}` }))]} />
                )}
                <Table caption="Matchups" columns={matchupColumns} rows={visibleMatchups} rowKey={(m) => m.id}
                       emptyText="No matchups yet. The commissioner can generate them once teams have joined." />
              </>
            )}
          </Async>
        )}
        {tab === 'members' && (
          <Async state={members}>
            {(rows) => <Table caption="League members" columns={memberColumns} rows={rows} rowKey={(m) => m.userId} />}
          </Async>
        )}
      </div>
    </>
  );
}
