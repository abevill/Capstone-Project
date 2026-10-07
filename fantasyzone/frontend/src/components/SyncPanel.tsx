import { useState } from 'react';
import Button from './Button';
import Card from './Card';
import Input from './Input';
import Select from './Select';
import { ErrorMessage, Notice } from './Feedback';
import { adminApi } from '../services/api';
import { errMsg } from '../utils/format';

const WEEK_OPTIONS = [
  { value: 'current', label: 'Current week' },
  ...Array.from({ length: 18 }, (_, i) => ({ value: String(i + 1), label: `Week ${i + 1}` })),
];

export default function SyncPanel() {
  const [season, setSeason] = useState('2026');
  const [week, setWeek] = useState('current');
  const [busy, setBusy] = useState<'players' | 'stats' | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function syncPlayers() {
    setBusy('players');
    setError(null);
    setMessage(null);
    try {
      const r = await adminApi.syncPlayers();
      setMessage(`Players synced: ${r.playersCreated} added, ${r.playersUpdated} updated.`);
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(null);
    }
  }

  async function syncStats() {
    const useCurrent = week === 'current';
    if (!useCurrent && !/^\d{4}$/.test(season)) {
      setError('Enter a four-digit season year, for example 2026.');
      return;
    }
    setBusy('stats');
    setError(null);
    setMessage(null);
    try {
      const r = await adminApi.syncStats(useCurrent ? undefined : Number(season), useCurrent ? undefined : Number(week));
      setMessage(
        `Stats synced: ${r.statLinesCreated} added, ${r.statLinesUpdated} updated, ` +
        `${r.skippedUnknownPlayers} skipped (not in the player list; sync players first).`,
      );
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusy(null);
    }
  }

  return (
    <Card title="Real NFL data (ESPN)">
      <p className="muted">
        Pulls players and weekly box-score stats from ESPN into FantasyZone. Run &quot;Sync players&quot; first
        (about half a minute), then sync stats for each week you want to score.
      </p>
      {message && <Notice message={message} />}
      {error && <ErrorMessage message={error} />}
      <div className="filters">
        <Button loading={busy === 'players'} disabled={busy !== null} onClick={syncPlayers}>Sync players</Button>
      </div>
      <div className="filters">
        <Input label="Season" inputMode="numeric" value={season} onChange={(e) => setSeason(e.target.value)}
               disabled={week === 'current'} />
        <Select label="Week" value={week} onChange={(e) => setWeek(e.target.value)} options={WEEK_OPTIONS} />
        <Button loading={busy === 'stats'} disabled={busy !== null} onClick={syncStats}>Sync stats</Button>
      </div>
    </Card>
  );
}
