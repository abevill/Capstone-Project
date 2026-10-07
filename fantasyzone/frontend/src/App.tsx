import { Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Leagues from './pages/Leagues';
import CreateLeague from './pages/CreateLeague';
import LeagueDetail from './pages/LeagueDetail';
import TeamPage from './pages/TeamPage';
import Players from './pages/Players';
import Admin from './pages/Admin';
import NotFound from './pages/NotFound';
import PlayerDetail from './pages/PlayerDetail';

export default function App() {
  return (
    <>
      <a className="skip-link" href="#main">Skip to content</a>
      <Navbar />
      <main id="main" className="container" tabIndex={-1}>
        <Routes>
          {/* Public: anyone can look around before signing up */}
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/leagues" element={<Leagues />} />
          <Route path="/leagues/:leagueId" element={<LeagueDetail />} />
          <Route path="/players" element={<Players />} />
          {/* Protected: requires login */}
          <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/leagues/create" element={<ProtectedRoute><CreateLeague /></ProtectedRoute>} />
          <Route path="/team/:teamId" element={<ProtectedRoute><TeamPage /></ProtectedRoute>} />
          <Route path="/team/:teamId/roster" element={<ProtectedRoute><TeamPage /></ProtectedRoute>} />
          {/* Admin only */}
          <Route path="/admin" element={<ProtectedRoute adminOnly><Admin /></ProtectedRoute>} />
          <Route path="*" element={<NotFound />} />
          <Route path="/players/:playerId" element={<PlayerDetail />} />
        </Routes>
      </main>
    </>
  );
}
