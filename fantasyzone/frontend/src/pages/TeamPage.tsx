import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Async from '../components/Async';
import Button from '../components/Button';
import Card from '../components/Card';
import Modal from '../components/Modal';
import Table, { type Column } from '../components/Table';
import { ErrorMessage } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../hooks/useFetch';
import { meApi, teamsApi } from '../services/api';
import { errMsg, record, score } from '../utils/format';
import type { RosterEntry, RosterTransaction } from '../types';

const SLOT_ORDER = ['QB', 'RB', 'WR', 'TE', 'FLEX', 'BENCH'];
const slotRank = (slot: string) => {
  const i = SLOT_ORDER.indexOf(slot.replace(/\d+$/, ''));
  return i === -1 ? 99 : i;
};

export default function TeamPage() {
  const id = Number(useParams().teamId);
  const { user } = useAuth();
  const team = useFetch(() => teamsApi.get(id), [id]);
  const roster = useFetch(() => teamsApi.roster(id), [id]);
  const history = useFetch(() => teamsApi.transactions(id), [id]);
  const myTeams = useFetch(() => (user ? meApi.teams() : Promise.resolve([])), [user?.userId]);
  const isOwner = (myTeams.data ?? []).some((t) => t.id === id);

  const [toDrop, setToDrop] = useState<RosterEntry | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sorted = useMemo(
    () => [...(roster.data ?? [])].sort((a, b) => slotRank(a.slot) - slotRank(b.slot) || a.name.localeCompare(b.name)),
    [roster.data],
  );

  async function confirmDrop() {
    if (!toDrop) return;
    setBusy(true);
    setError(null);
    try {
      await teamsApi.dropPlayer(id, toDrop.playerId);
      setToDrop(null);
      roster.reload();
      history.reload();
    } catch (e) {
      setError(errMsg(e));
      setToDrop(null);
    } finally {
      setBusy(false);
    }
  }

  const columns: Column<RosterEntry>[] = [
    { header: 'Slot', render: (r) => r.slot },
    { header: 'Player', render: (r) => r.name },
    { header: 'Pos', render: (r) => r.position },
    { header: 'NFL team', render: (r) => r.nflTeam ?? '-' },
    ...(isOwner ? [{ header: 'Actions', render: (r: RosterEntry) =>
      <Button variant="danger" onClick={() => setToDrop(r)}>Drop</Button> }] : []),
  ];

  const historyColumns: Column<RosterTransaction>[] = [
    { header: 'When', render: (t) => new Date(t.at).toLocaleString() },
    { header: 'Move', render: (t) => (t.type === 'ADD' ? 'Added' : 'Dropped') },
    { header: 'Player', render: (t) => `${t.playerName} (${t.position})` },
    { header: 'Slot', render: (t) => t.slot ?? '-' },
    { header: 'By', render: (t) => t.madeBy },
  ];

  return (
    <>
      <Async state={team} lines={2}>
        {(t) => (
          <header className="page-head">
            <h1>{t.name}</h1>
            <p className="muted">
              Managed by {t.owner} - Record {record(t.wins, t.losses, t.ties)} - {score(t.pointsFor)} points
            </p>
            <p><Link to={`/leagues/${t.leagueId}`}>Back to league</Link></p>
          </header>
        )}
      </Async>

      {error && <ErrorMessage message={error} />}

      <Card title="Roster">
        {isOwner && <p><Link to="/players" className="btn btn-primary">Add players</Link></p>}
        <Async state={roster}>
          {() => <Table caption="Roster" columns={columns} rows={sorted} rowKey={(r) => r.playerId}
                        emptyText="No players yet. Browse the player list to add some." />}
        </Async>
      </Card>

      <Card title="Roster history">
        <Async state={history}>
          {(rows) => <Table caption="Roster history" columns={historyColumns} rows={rows} rowKey={(t) => t.id}
                            emptyText="No roster moves yet." />}
        </Async>
      </Card>

      <Modal open={toDrop !== null} title="Drop player?" onClose={() => setToDrop(null)}>
        <p>Drop <strong>{toDrop?.name}</strong> from your roster? They become available to everyone in your league.</p>
        <div className="row-actions">
          <Button variant="danger" loading={busy} onClick={confirmDrop}>Drop player</Button>
          <Button variant="secondary" onClick={() => setToDrop(null)}>Cancel</Button>
        </div>
      </Modal>
    </>
  );
}
