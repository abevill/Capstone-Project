import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();

  return (
    <header className="navbar">
      <nav aria-label="Main" className="navbar-inner">
        <Link to="/" className="brand">FantasyZone</Link>
        <div className="nav-links">
          <NavLink to="/leagues">Leagues</NavLink>
          <NavLink to="/players">Players</NavLink>
          {user && <NavLink to="/dashboard">Dashboard</NavLink>}
          {isAdmin && <NavLink to="/admin">Admin</NavLink>}
        </div>
        <div className="nav-actions">
          <button type="button" className="btn btn-secondary" onClick={toggle}
                  aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}>
            {theme === 'dark' ? 'Light' : 'Dark'}
          </button>
          {user ? (
            <>
              <span className="muted">{user.displayName}</span>
              <button type="button" className="btn btn-secondary" onClick={() => { logout(); navigate('/'); }}>Log out</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-secondary">Log in</Link>
              <Link to="/register" className="btn btn-primary">Sign up</Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
