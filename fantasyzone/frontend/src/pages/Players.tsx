import { useMemo, useReducer, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import Async from '../components/Async';
import Button from '../components/Button';
import Card from '../components/Card';
import Input from '../components/Input';
import Modal from '../components/Modal';
import Pagination from '../components/Pagination';
import Select from '../components/Select';
import Table, { type Column } from '../components/Table';
import { ErrorMessage, Notice } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../hooks/useFetch';
import { meApi, playersApi, teamsApi } from '../services/api';
import { errMsg } from '../utils/format';
import type { League, Player, PlayerQuery, Team } from '../types';

type Mode = 'all' | 'name' | 'position' | 'team';
interface Filters { mode: Mode; value: string; page: number }
type FilterAction =
  | { type: 'mode'; mode: Mode }
  | { type: 'value'; value: string }
  | { type: 'page'; page: number };

function filterReducer(state: Filters, action: FilterAction): Filters {
  switch (action.type) {
    case 'mode': return { mode: action.mode, value: action.mode === 'position' ? 'QB' : '', page: 0 };
    case 'value': return { ...state, value: action.value, page: 0 };
    case 'page': return { ...state, page: action.page };
  }
}

const POSITION_OPTIONS = [
  { value: 'QB', label: 'QB' },
  { value: 'RB', label: 'RB' },
  { value: 'WR', label: 'WR' },
  { value: 'TE', label: 'TE' },
  { value: 'PK', label: 'PK' }
];


export default function Players() {
  const { user } = useAuth();
  const [filters, dispatch] = useReducer(filterReducer, { mode: 'all', value: '', page: 0 });
  const [draft, setDraft] = useState('');

  const params = useMemo<PlayerQuery>(() => {
    const q: PlayerQuery = { page: filters.page };
    if (filters.value) {
      if (filters.mode === 'name') q.name = filters.value;
      if (filters.mode === 'position') q.position = filters.value;
      if (filters.mode === 'team') q.team = filters.value;
    }
    return q;
  }, [filters]);

  const players = useFetch(() => playersApi.list(params), [params]);
  const myTeams = useFetch(() => (user ? meApi.teams() : Promise.resolve([] as Team[])), [user?.userId]);
  const myLeagues = useFetch(() => (user ? meApi.leagues() : Promise.resolve([] as League[])), [user?.userId]);
  const leagueNames = useMemo(() => new Map((myLeagues.data ?? []).map((l) => [l.id, l.name] as const)), [myLeagues.data]);

  const [target, setTarget] = useState<Player | null>(null);
  const [teamId, setTeamId] = useState('');

  const [busy, setBusy] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const teamOptions = (myTeams.data ?? []).map((t) => ({
    value: String(t.id), label: `${t.name} (${leagueNames.get(t.leagueId) ?? `League ${t.leagueId}`})`,
  }));
  const chosenTeam = teamId || teamOptions[0]?.value || '';

  function applyFilter(e: FormEvent) {
    e.preventDefault();
    dispatch({ type: 'value', value: draft.trim() });
  }

  async function addPlayer() {
    if (!target || !chosenTeam) return;
    setBusy(true);
    setModalError(null);
    try {
      const entry = await teamsApi.addPlayer(Number(chosenTeam), target.id);
      setNotice(`${target.name} was added to your roster (${entry.slot}).`);
    } catch (e) {
      setModalError(errMsg(e));
    } finally {
      setBusy(false);
    }
  }

  const columns: Column<Player>[] = [
    {
      header: 'Player',
      render: (p) => (
          <Link to={`/players/${p.id}`}>
            {p.name}
          </Link>
      ),
    },
    {
      header: 'Position',
      render: (p) => p.position === 'PK' ? 'K' : p.position,
    },
    { header: 'NFL team', render: (p) => p.nflTeam ?? '-' },
    ...(user ? [{ header: 'Roster', render: (p: Player) =>
      <Button variant="secondary" onClick={() => { setModalError(null); setNotice(null); setTarget(p); }}>Add</Button> }] : []),
  ];

  return (
    <Card title="NFL players">
      <p className="muted">Real-world player data. Anyone can browse; log in to add players to your team.</p>
      {notice && <Notice message={notice} />}

      <form className="filters" onSubmit={applyFilter}>
        <Select label="Filter by" value={filters.mode}
                onChange={(e) => { setDraft(''); dispatch({ type: 'mode', mode: e.target.value as Mode }); }}
                options={[{ value: 'all', label: 'Everyone' }, { value: 'name', label: 'Name' },
                          { value: 'position', label: 'Position' }, { value: 'team', label: 'NFL team' }]} />
        {filters.mode === 'position' && (
          <Select label="Position" value={filters.value}
                  onChange={(e) => dispatch({ type: 'value', value: e.target.value })}
                  options={POSITION_OPTIONS} />
        )}
        {(filters.mode === 'name' || filters.mode === 'team') && (
          <>
            <Input label={filters.mode === 'name' ? 'Player name' : 'Team abbreviation (e.g. KC)'}
                   value={draft} onChange={(e) => setDraft(e.target.value)} />
            <Button type="submit">Search</Button>
          </>
        )}
      </form>

      <Async state={players}>
        {(p) => (
          <>
            <Table caption="NFL players" columns={columns} rows={p.content} rowKey={(x) => x.id} emptyText="No players match that filter." />
            <Pagination page={p.number} totalPages={p.totalPages} onChange={(page) => dispatch({ type: 'page', page })} />
          </>
        )}
      </Async>

      <Modal open={target !== null} title={`Add ${target?.name ?? 'player'}`} onClose={() => setTarget(null)}>
        {modalError && <ErrorMessage message={modalError} />}
        {teamOptions.length === 0 ? (
          <p>You need a team first. <Link to="/leagues">Join or create a league</Link>, then create your team.</p>
        ) : (
          <>
            <Select label="Team" value={chosenTeam} onChange={(e) => setTeamId(e.target.value)} options={teamOptions} />
            <p className="muted">
              FantasyZone puts the player in the right starting spot automatically, or on the bench if that spot is full.
            </p>
            <div className="row-actions">
              <Button loading={busy} onClick={addPlayer}>Add to roster</Button>
              <Button variant="secondary" onClick={() => setTarget(null)}>Cancel</Button>
            </div>
          </>
        )}
      </Modal>
    </Card>
  );
}
