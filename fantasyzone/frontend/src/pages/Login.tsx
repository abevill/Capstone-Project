import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';
import Input from '../components/Input';
import { ErrorMessage } from '../components/Feedback';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/api';
import { errMsg } from '../utils/format';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const v: Record<string, string> = {};
    if (!/^\S+@\S+\.\S+$/.test(email)) v.email = 'Enter a valid email address';
    if (!password) v.password = 'Password is required';
    setErrors(v);
    setFormError(null);
    if (Object.keys(v).length > 0) return;

    setBusy(true);
    try {
      login(await authApi.login({ email, password }));
      navigate(from, { replace: true });
    } catch (err) {
      setFormError(errMsg(err, 'Login failed'));
      setBusy(false);
    }
  }

  return (
    <Card title="Log in" className="narrow">
      {formError && <ErrorMessage message={formError} />}
      <form onSubmit={onSubmit} noValidate>
        <Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
               error={errors.email} autoComplete="email" />
        <Input label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
               error={errors.password} autoComplete="current-password" />
        <Button type="submit" loading={busy}>Log in</Button>
      </form>
      <p className="muted">New here? <Link to="/register">Create an account</Link></p>
    </Card>
  );
}
