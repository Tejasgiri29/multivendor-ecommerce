import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { errMsg } from '../api.js';
import Alert from '../components/Alert.jsx';

export const homeFor = (role) => (role === 'ADMIN' ? '/admin' : role === 'SELLER' ? '/seller' : '/');

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const user = await login(form.email, form.password);
      navigate(location.state?.from || homeFor(user.role), { replace: true });
    } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  };

  return (
    <form className="card form narrow" onSubmit={submit}>
      <h2>Login</h2>
      <Alert>{error}</Alert>
      <label>Email<input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
      <label>Password<input type="password" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
      <button className="btn" disabled={busy}>{busy ? 'Signing in...' : 'Login'}</button>
      <p className="muted">No account? <Link to="/register">Register</Link></p>
      <p className="muted small-text">Demo: customer@shop.com / Customer@123 &middot; seller@shop.com / Seller@123 &middot; admin@shop.com / Admin@123</p>
    </form>
  );
}
