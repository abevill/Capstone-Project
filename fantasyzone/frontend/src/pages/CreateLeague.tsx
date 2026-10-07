import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';
import Input from '../components/Input';
import { ErrorMessage, Notice } from '../components/Feedback';
import { ApiError, leaguesApi } from '../services/api';
import { errMsg } from '../utils/format';
import type { League } from '../types';

export default function CreateLeague() {
  const [name, setName] = useState('');
  const [season, setSeason] = useState('2026');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState<League | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const v: Record<string, string> = {};
    if (!name.trim()) v.name = 'League name is required';
    const year = Number(season);
    if (!Number.isInteger(year) || year < 2000 || year > 2100) v.season = 'Enter a season year between 2000 and 2100';
    setErrors(v);
    setFormError(null);
    if (Object.keys(v).length > 0) return;

    setBusy(true);
    try {
      setCreated(await leaguesApi.create({ name: name.trim(), season: year }));
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) setErrors(err.fieldErrors);
      setFormError(errMsg(err));
    } finally {
      setBusy(false);
    }
  }

  if (created) {
    return (
      <Card title="League created" className="narrow">
        <Notice message={`${created.name} is ready.`} />
        <p>Share this join code with your friends: <strong className="code">{created.joinCode}</strong></p>
        <p>Next, open the league and create your team.</p>
        <Link to={`/leagues/${created.id}`} className="btn btn-primary">Open league</Link>
      </Card>
    );
  }

  return (
    <Card title="Create a league" className="narrow">
      {formError && <ErrorMessage message={formError} />}
      <form onSubmit={onSubmit} noValidate>
        <Input label="League name" value={name} onChange={(e) => setName(e.target.value)} error={errors.name} maxLength={80} />
        <Input label="Season (year)" inputMode="numeric" value={season} onChange={(e) => setSeason(e.target.value)} error={errors.season} />
        <Button type="submit" loading={busy}>Create league</Button>
      </form>
      <p className="muted">You become the commissioner of this league.</p>
    </Card>
  );
}
