import { Link } from 'react-router-dom';
import Card from '../components/Card';
import Async from '../components/Async';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../hooks/useFetch';
import { leaguesApi } from '../services/api';

export default function Home() {
  const { user } = useAuth();
  const leagues = useFetch(() => leaguesApi.list(0, 5), []);

  return (
    <>
      <section className="hero">
        <h1>Run your fantasy football league in one place</h1>
        <p>Create or join a league, build your roster, follow weekly matchups, and watch the standings update.
           Real NFL player data powers the stats, and FantasyZone runs the league itself.</p>
        <div className="row-actions">
          {user
            ? <Link to="/dashboard" className="btn btn-primary">Go to my dashboard</Link>
            : <><Link to="/register" className="btn btn-primary">Create a free account</Link>
                <Link to="/login" className="btn btn-secondary">Log in</Link></>}
          <Link to="/players" className="btn btn-secondary">Browse NFL players</Link>
        </div>
      </section>

      <div className="grid">
        <Card title="What you can do">
          <ul>
            <li>Create a league and invite friends with a join code</li>
            <li>Add and drop players from your roster</li>
            <li>Follow weekly head-to-head matchups</li>
            <li>See live league standings, no account needed</li>
          </ul>
        </Card>
        <Card title="Leagues you can peek at">
          <Async state={leagues} lines={3}>
            {(page) => page.content.length === 0
              ? <p className="muted">No leagues yet. Be the first to create one.</p>
              : <ul>{page.content.map((l) => (
                  <li key={l.id}><Link to={`/leagues/${l.id}`}>{l.name}</Link>{' '}
                    <span className="muted">({l.season}, {l.memberCount} members)</span></li>))}</ul>}
          </Async>
          <p><Link to="/leagues">See all leagues</Link></p>
        </Card>
      </div>
    </>
  );
}
