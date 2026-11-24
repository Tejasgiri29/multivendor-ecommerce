import { useCallback, useEffect, useState } from 'react';
import api, { errMsg, money } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import Alert from '../components/Alert.jsx';
import Pagination from '../components/Pagination.jsx';
import { OrderItems, statusClass } from './Orders.jsx';

function useNotice() {
  const [msg, setMsg] = useState(null);
  return [msg, (kind, text) => setMsg({ kind, text })];
}

function CategoriesTab() {
  const [list, setList] = useState([]);
  const [form, setForm] = useState({ name: '', description: '' });
  const [editingId, setEditingId] = useState(null);
  const [msg, notify] = useNotice();
  const load = useCallback(() => { api.get('/categories').then((r) => setList(r.data)); }, []);
  useEffect(load, [load]);

  const submit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) await api.put(`/admin/categories/${editingId}`, form);
      else await api.post('/admin/categories', form);
      setForm({ name: '', description: '' }); setEditingId(null); notify('success', 'Saved'); load();
    } catch (err) { notify('error', errMsg(err)); }
  };
  const remove = async (c) => {
    if (!window.confirm(`Delete category "${c.name}"?`)) return;
    try { await api.delete(`/admin/categories/${c.id}`); notify('success', 'Deleted'); load(); } catch (err) { notify('error', errMsg(err)); }
  };

  return (
    <>
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      <form className="row form" onSubmit={submit}>
        <input required placeholder="Name" maxLength={100} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <input placeholder="Description" maxLength={500} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <button className="btn">{editingId ? 'Update' : 'Add category'}</button>
        {editingId && <button type="button" className="btn secondary" onClick={() => { setEditingId(null); setForm({ name: '', description: '' }); }}>Cancel</button>}
      </form>
      <table className="table">
        <thead><tr><th>ID</th><th>Name</th><th>Description</th><th /></tr></thead>
        <tbody>
          {list.map((c) => (
            <tr key={c.id}>
              <td>{c.id}</td><td>{c.name}</td><td>{c.description}</td>
              <td className="actions">
                <button className="btn small secondary" onClick={() => { setEditingId(c.id); setForm({ name: c.name, description: c.description || '' }); }}>Edit</button>
                <button className="btn small danger-btn" onClick={() => remove(c)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

const BLANK_COUPON = { code: '', type: 'PERCENT', value: '', minOrderAmount: '', expiresAt: '', usageLimit: '', active: true };

function CouponsTab() {
  const [list, setList] = useState([]);
  const [form, setForm] = useState(BLANK_COUPON);
  const [editingId, setEditingId] = useState(null);
  const [msg, notify] = useNotice();
  const load = useCallback(() => { api.get('/admin/coupons').then((r) => setList(r.data)).catch((e) => notify('error', errMsg(e))); }, []);
  useEffect(load, [load]);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const toBody = (f) => ({
    code: f.code, type: f.type, value: Number(f.value),
    minOrderAmount: f.minOrderAmount === '' || f.minOrderAmount == null ? null : Number(f.minOrderAmount),
    expiresAt: f.expiresAt ? (f.expiresAt.length === 16 ? f.expiresAt + ':00' : f.expiresAt) : null,
    usageLimit: f.usageLimit === '' || f.usageLimit == null ? null : Number(f.usageLimit),
    active: f.active,
  });

  const submit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) await api.put(`/admin/coupons/${editingId}`, toBody(form));
      else await api.post('/admin/coupons', toBody(form));
      setForm(BLANK_COUPON); setEditingId(null); notify('success', 'Saved'); load();
    } catch (err) { notify('error', errMsg(err)); }
  };
  const edit = (c) => {
    setEditingId(c.id);
    setForm({ code: c.code, type: c.type, value: c.value, minOrderAmount: c.minOrderAmount ?? '', expiresAt: c.expiresAt ? c.expiresAt.slice(0, 16) : '', usageLimit: c.usageLimit ?? '', active: c.active });
  };
  const toggle = async (c) => {
    try { await api.put(`/admin/coupons/${c.id}`, toBody({ ...c, active: !c.active, expiresAt: c.expiresAt || '' })); load(); } catch (err) { notify('error', errMsg(err)); }
  };
  const remove = async (c) => {
    if (!window.confirm(`Delete coupon ${c.code}?`)) return;
    try { await api.delete(`/admin/coupons/${c.id}`); load(); } catch (err) { notify('error', errMsg(err)); }
  };

  return (
    <>
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      <form className="form grid-form" onSubmit={submit}>
        <h3>{editingId ? `Edit coupon #${editingId}` : 'New coupon'}</h3>
        <label>Code<input required maxLength={50} value={form.code} onChange={set('code')} /></label>
        <label>Type
          <select value={form.type} onChange={set('type')}><option value="PERCENT">Percent (%)</option><option value="FIXED">Fixed amount</option></select>
        </label>
        <label>Value<input type="number" required min="0.01" step="0.01" value={form.value} onChange={set('value')} /></label>
        <label>Min order amount<input type="number" min="0" step="0.01" value={form.minOrderAmount} onChange={set('minOrderAmount')} /></label>
        <label>Expires at<input type="datetime-local" value={form.expiresAt} onChange={set('expiresAt')} /></label>
        <label>Usage limit<input type="number" min="1" step="1" value={form.usageLimit} onChange={set('usageLimit')} /></label>
        <label className="check"><input type="checkbox" checked={form.active} onChange={set('active')} /> Active</label>
        <div className="row span2">
          <button className="btn">{editingId ? 'Save coupon' : 'Create coupon'}</button>
          {editingId && <button type="button" className="btn secondary" onClick={() => { setEditingId(null); setForm(BLANK_COUPON); }}>Cancel</button>}
        </div>
      </form>
      <table className="table">
        <thead><tr><th>Code</th><th>Discount</th><th>Min order</th><th>Expires</th><th>Used</th><th>Active</th><th /></tr></thead>
        <tbody>
          {list.map((c) => (
            <tr key={c.id}>
              <td><strong>{c.code}</strong></td>
              <td>{c.type === 'PERCENT' ? `${c.value}%` : money(c.value)}</td>
              <td>{c.minOrderAmount != null ? money(c.minOrderAmount) : '-'}</td>
              <td>{c.expiresAt ? new Date(c.expiresAt).toLocaleDateString() : 'never'}</td>
              <td>{c.usedCount}{c.usageLimit != null ? ` / ${c.usageLimit}` : ''}</td>
              <td>{c.active ? 'Yes' : 'No'}</td>
              <td className="actions">
                <button className="btn small secondary" onClick={() => edit(c)}>Edit</button>
                <button className="btn small secondary" onClick={() => toggle(c)}>{c.active ? 'Deactivate' : 'Activate'}</button>
                <button className="btn small danger-btn" onClick={() => remove(c)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}

function UsersTab() {
  const { user: me } = useAuth();
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [msg, notify] = useNotice();
  const load = useCallback(() => {
    api.get('/admin/users', { params: { page, size: 10 } }).then((r) => setData(r.data)).catch((e) => notify('error', errMsg(e)));
  }, [page]);
  useEffect(load, [load]);

  const call = async (fn) => { try { await fn(); load(); } catch (e) { notify('error', errMsg(e)); } };

  return (
    <>
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      <table className="table">
        <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th /></tr></thead>
        <tbody>
          {data.content.map((u) => (
            <tr key={u.id}>
              <td>{u.id}</td><td>{u.fullName}</td><td>{u.email}</td><td><span className="badge">{u.role}</span></td>
              <td>
                {u.enabled ? 'Enabled' : <span className="danger">Disabled</span>}
                {u.role === 'SELLER' && (u.sellerApproved ? ' / approved' : ' / pending approval')}
              </td>
              <td className="actions">
                {u.role === 'SELLER' && !u.sellerApproved && <button className="btn small" onClick={() => call(() => api.post(`/admin/users/${u.id}/approve-seller`))}>Approve seller</button>}
                {u.id !== me.id && <button className={`btn small ${u.enabled ? 'danger-btn' : 'secondary'}`}
                  onClick={() => call(() => api.put(`/admin/users/${u.id}/enabled`, null, { params: { value: !u.enabled } }))}>{u.enabled ? 'Disable' : 'Enable'}</button>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </>
  );
}

const NEXT = { PLACED: ['PAID', 'CANCELLED'], PAID: ['SHIPPED', 'CANCELLED'], SHIPPED: ['DELIVERED'], DELIVERED: [], CANCELLED: [] };

function OrdersTab() {
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [msg, notify] = useNotice();
  const load = useCallback(() => {
    api.get('/admin/orders', { params: { page, size: 8 } }).then((r) => setData(r.data)).catch((e) => notify('error', errMsg(e)));
  }, [page]);
  useEffect(load, [load]);

  const change = async (id, status) => {
    try { await api.put(`/orders/${id}/status`, { status }); load(); } catch (e) { notify('error', errMsg(e)); }
  };

  return (
    <>
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      <table className="table">
        <thead><tr><th>#</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th><th>Placed</th><th>Move to</th></tr></thead>
        <tbody>
          {data.content.map((o) => (
            <tr key={o.id}>
              <td>{o.id}</td><td>{o.customerName}</td><td><OrderItems items={o.items} /></td><td>{money(o.total)}</td>
              <td><span className={statusClass(o.status)}>{o.status}</span></td>
              <td>{new Date(o.createdAt).toLocaleDateString()}</td>
              <td className="actions">
                {NEXT[o.status].length === 0 ? <span className="muted">final</span> : NEXT[o.status].map((s) => (
                  <button key={s} className={`btn small ${s === 'CANCELLED' ? 'danger-btn' : 'secondary'}`} onClick={() => change(o.id, s)}>{s}</button>
                ))}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </>
  );
}

const TABS = { categories: ['Categories', CategoriesTab], coupons: ['Coupons', CouponsTab], users: ['Users', UsersTab], orders: ['Orders', OrdersTab] };

export default function AdminDashboard() {
  const [tab, setTab] = useState('users');
  const Active = TABS[tab][1];
  return (
    <div className="card">
      <h2>Admin dashboard</h2>
      <div className="tabs">
        {Object.entries(TABS).map(([key, [label]]) => (
          <button key={key} className={tab === key ? 'active' : ''} onClick={() => setTab(key)}>{label}</button>
        ))}
      </div>
      <Active />
    </div>
  );
}
