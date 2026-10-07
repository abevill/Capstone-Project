import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Async from '../components/Async';
import Card from '../components/Card';
import JoinLeagueForm from '../components/JoinLeagueForm';
import Modal from '../components/Modal';
import Pagination from '../components/Pagination';
import Table, { type Column } from '../components/Table';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../hooks/useFetch';
import { leaguesApi } from '../services/api';
import type { League } from '../types';

const columns: Column<League>[] = [
  { header: 'League', render: (l) => <Link to={`/leagues/${l.id}`}>{l.name}</Link> },
  { header: 'Season', render: (l) => l.season },
  { header: 'Members', render: (l) => l.memberCount },
];

export default function Leagues() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [joinOpen, setJoinOpen] = useState(false);
  const leagues = useFetch(() => leaguesApi.list(page), [page]);

  return (
    <Card title="Leagues">
      <div className="row-actions">
        {user ? (
          <>
            <Link to="/leagues/create" className="btn btn-primary">Create a league</Link>
            <button type="button" className="btn btn-secondary" onClick={() => setJoinOpen(true)}>Join with a code</button>
          </>
        ) : (
          <p className="muted"><Link to="/login">Log in</Link> to create or join a league. Browsing is open to everyone.</p>
        )}
      </div>
      <Async state={leagues}>
        {(p) => (
          <>
            <Table caption="All leagues" columns={columns} rows={p.content} rowKey={(l) => l.id}
                   emptyText="No leagues yet. Create the first one!" />
            <Pagination page={p.number} totalPages={p.totalPages} onChange={setPage} />
          </>
        )}
      </Async>
      <Modal open={joinOpen} title="Join a league" onClose={() => setJoinOpen(false)}>
        <JoinLeagueForm onJoined={(l) => { setJoinOpen(false); navigate(`/leagues/${l.id}`); }} />
      </Modal>
    </Card>
  );
}
