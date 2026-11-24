import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext.jsx';
import { errMsg } from '../api.js';
import Alert from '../components/Alert.jsx';
import { homeFor } from './Login.jsx';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', role: 'CUSTOMER' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      const user = await register(form);
      navigate(homeFor(user.role), { replace: true });
    } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  };

  return (
    <form className="card form narrow" onSubmit={submit}>
      <h2>Create account</h2>
      <Alert>{error}</Alert>
      <label>Full name<input required maxLength={100} value={form.fullName} onChange={set('fullName')} /></label>
      <label>Email<input type="email" required value={form.email} onChange={set('email')} /></label>
      <label>Password (min 8 characters)<input type="password" required minLength={8} value={form.password} onChange={set('password')} /></label>
      <label>I want to
        <select value={form.role} onChange={set('role')}>
          <option value="CUSTOMER">Buy products (Customer)</option>
          <option value="SELLER">Sell products (Seller - needs admin approval)</option>
        </select>
      </label>
      <button className="btn" disabled={busy}>{busy ? 'Creating...' : 'Register'}</button>
      <p className="muted">Already registered? <Link to="/login">Login</Link></p>
    </form>
  );
}
