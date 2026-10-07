import { useState } from 'react';
import Async from '../components/Async';
import Button from '../components/Button';
import Card from '../components/Card';
import Pagination from '../components/Pagination';
import SyncPanel from '../components/SyncPanel';
import Table, { type Column } from '../components/Table';
import { ErrorMessage } from '../components/Feedback';
import { useFetch } from '../hooks/useFetch';
import { adminApi } from '../services/api';
import { errMsg } from '../utils/format';
import type { UserSummary } from '../types';

export default function Admin() {
  const [page, setPage] = useState(0);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const users = useFetch(() => adminApi.users(page), [page]);

  async function toggle(u: UserSummary) {
    setBusyId(u.id);
    setError(null);
    try {
      await adminApi.setStatus(u.id, !u.enabled);
      users.reload();
    } catch (e) {
      setError(errMsg(e));
    } finally {
      setBusyId(null);
    }
  }

  const columns: Column<UserSummary>[] = [
    { header: 'Email', render: (u) => u.email },
    { header: 'Name', render: (u) => u.displayName },
    { header: 'Roles', render: (u) => u.roles.map((r) => r.replace('ROLE_', '')).join(', ') },
    { header: 'Status', render: (u) => <span className={u.enabled ? 'badge ok' : 'badge off'}>{u.enabled ? 'Active' : 'Disabled'}</span> },
    { header: 'Action', render: (u) => (
      <Button variant={u.enabled ? 'danger' : 'secondary'} loading={busyId === u.id} onClick={() => toggle(u)}>
        {u.enabled ? 'Disable' : 'Enable'}
      </Button>) },
  ];

  return (
    <>
      <SyncPanel />
    <Card title="User administration">
      {error && <ErrorMessage message={error} />}
      <Async state={users}>
        {(p) => (
          <>
            <Table caption="All users" columns={columns} rows={p.content} rowKey={(u) => u.id} />
            <Pagination page={p.number} totalPages={p.totalPages} onChange={setPage} />
          </>
        )}
      </Async>
    </Card>
    </>
  );
}
