import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';

/** Route guard: requires login, and optionally one of the given roles. */
export default function ProtectedRoute({ roles, children }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="card center">
        <h2>Access denied</h2>
        <p>Your role ({user.role}) cannot open this page.</p>
      </div>
    );
  }
  return children;
}
