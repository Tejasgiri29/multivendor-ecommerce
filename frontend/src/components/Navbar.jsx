import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="navbar">
      <Link to="/" className="brand">MultiMart</Link>
      <nav>
        <NavLink to="/" end>Shop</NavLink>
        {user?.role === 'CUSTOMER' && (
          <>
            <NavLink to="/cart">Cart</NavLink>
            <NavLink to="/wishlist">Wishlist</NavLink>
            <NavLink to="/orders">My Orders</NavLink>
          </>
        )}
        {user?.role === 'SELLER' && <NavLink to="/seller">Seller Dashboard</NavLink>}
        {user?.role === 'ADMIN' && <NavLink to="/admin">Admin Dashboard</NavLink>}
      </nav>
      <div className="nav-right">
        {user ? (
          <>
            <span className="muted">{user.fullName} <span className="badge">{user.role}</span></span>
            <button className="btn small secondary" onClick={() => { logout(); navigate('/'); }}>Logout</button>
          </>
        ) : (
          <>
            <Link className="btn small secondary" to="/login">Login</Link>
            <Link className="btn small" to="/register">Register</Link>
          </>
        )}
      </div>
    </header>
  );
}
