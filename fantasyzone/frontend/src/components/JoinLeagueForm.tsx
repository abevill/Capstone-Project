import { useState, type FormEvent } from 'react';
import Button from './Button';
import Input from './Input';
import { leaguesApi } from '../services/api';
import { errMsg } from '../utils/format';
import type { League } from '../types';

export default function JoinLeagueForm({ onJoined }: { onJoined: (league: League) => void }) {
  const [code, setCode] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!code.trim()) { setError('Enter the join code'); return; }
    setBusy(true);
    setError(undefined);
    try {
      onJoined(await leaguesApi.join(code.trim()));
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      <Input label="Join code" value={code} onChange={(e) => setCode(e.target.value)} error={error}
             autoComplete="off" placeholder="e.g. 7F3A9C21" />
      <Button type="submit" loading={busy}>Join league</Button>
    </form>
  );
}
