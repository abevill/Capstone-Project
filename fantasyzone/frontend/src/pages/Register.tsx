import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';
import Input from '../components/Input';
import { ErrorMessage } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import { ApiError, authApi } from '../services/api';
import { errMsg } from '../utils/format';

export default function Register() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', displayName: '', password: '', confirm: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const v: Record<string, string> = {};
    if (!/^\S+@\S+\.\S+$/.test(form.email)) v.email = 'Enter a valid email address';
    if (form.displayName.trim().length < 3) v.displayName = 'Display name must be at least 3 characters';
    if (form.password.length < 8) v.password = 'Password must be at least 8 characters';
    if (form.confirm !== form.password) v.confirm = 'Passwords do not match';
    setErrors(v);
    setFormError(null);
    if (Object.keys(v).length > 0) return;

    setBusy(true);
    try {
      login(await authApi.register({ email: form.email, displayName: form.displayName, password: form.password }));
      navigate('/dashboard', { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) setErrors(err.fieldErrors);
      setFormError(errMsg(err, 'Registration failed'));
      setBusy(false);
    }
  }

  return (
    <Card title="Create your account" className="narrow">
      {formError && <ErrorMessage message={formError} />}
      <form onSubmit={onSubmit} noValidate>
        <Input label="Email" type="email" value={form.email} onChange={set('email')} error={errors.email} autoComplete="email" />
        <Input label="Display name" value={form.displayName} onChange={set('displayName')} error={errors.displayName} autoComplete="nickname" />
        <Input label="Password" type="password" value={form.password} onChange={set('password')} error={errors.password} autoComplete="new-password" />
        <Input label="Confirm password" type="password" value={form.confirm} onChange={set('confirm')} error={errors.confirm} autoComplete="new-password" />
        <Button type="submit" loading={busy}>Sign up</Button>
      </form>
      <p className="muted">Already have an account? <Link to="/login">Log in</Link></p>
    </Card>
  );
}
