import { useCallback, useEffect, useState } from 'react';
import api, { errMsg, money } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import Alert from '../components/Alert.jsx';
import Pagination from '../components/Pagination.jsx';
import { OrderItems, statusClass } from './Orders.jsx';

const BLANK = { name: '', description: '', price: '', stock: '', categoryId: '', imageUrl: '' };

function ProductsTab({ approved }) {
  const [categories, setCategories] = useState([]);
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [form, setForm] = useState(BLANK);
  const [editingId, setEditingId] = useState(null);
  const [msg, setMsg] = useState(null);

  const load = useCallback(() => {
    api.get('/seller/products', { params: { page, size: 8 } }).then((r) => setData(r.data)).catch((e) => setMsg({ kind: 'error', text: errMsg(e) }));
  }, [page]);
  useEffect(load, [load]);
  useEffect(() => { api.get('/categories').then((r) => setCategories(r.data)); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const body = { ...form, price: Number(form.price), stock: Number(form.stock), categoryId: Number(form.categoryId), imageUrl: form.imageUrl || null };
    try {
      if (editingId) await api.put(`/seller/products/${editingId}`, body);
      else await api.post('/seller/products', body);
      setMsg({ kind: 'success', text: editingId ? 'Product updated' : 'Product created' });
      setForm(BLANK); setEditingId(null); load();
    } catch (err) { setMsg({ kind: 'error', text: errMsg(err) }); }
  };

  const edit = (p) => {
    setEditingId(p.id);
    setForm({ name: p.name, description: p.description || '', price: p.price, stock: p.stock, categoryId: p.categoryId, imageUrl: p.imageUrl || '' });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const remove = async (p) => {
    if (!window.confirm(`Delete "${p.name}"?`)) return;
    try { await api.delete(`/seller/products/${p.id}`); setMsg({ kind: 'success', text: 'Product deleted' }); load(); }
    catch (err) { setMsg({ kind: 'error', text: errMsg(err) }); }
  };

  return (
    <>
      {!approved && <Alert kind="warning">Your seller account is awaiting admin approval. You can prepare listings once it is approved.</Alert>}
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      <form className="form grid-form" onSubmit={submit}>
        <h3>{editingId ? `Edit product #${editingId}` : 'Add product'}</h3>
        <label>Name<input required maxLength={200} value={form.name} onChange={set('name')} /></label>
        <label>Category
          <select required value={form.categoryId} onChange={set('categoryId')}>
            <option value="">Select...</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label>Price<input type="number" required min="0.01" step="0.01" value={form.price} onChange={set('price')} /></label>
        <label>Stock<input type="number" required min="0" step="1" value={form.stock} onChange={set('stock')} /></label>
        <label className="span2">Image URL<input maxLength={500} value={form.imageUrl} onChange={set('imageUrl')} placeholder="https://..." /></label>
        <label className="span2">Description<textarea rows={3} maxLength={2000} value={form.description} onChange={set('description')} /></label>
        <div className="row span2">
          <button className="btn">{editingId ? 'Save changes' : 'Create product'}</button>
          {editingId && <button type="button" className="btn secondary" onClick={() => { setEditingId(null); setForm(BLANK); }}>Cancel edit</button>}
        </div>
      </form>

      <table className="table">
        <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th /></tr></thead>
        <tbody>
          {data.content.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td><td>{p.name}</td><td>{p.categoryName}</td><td>{money(p.price)}</td>
              <td className={p.stock === 0 ? 'danger' : ''}>{p.stock}</td>
              <td className="actions">
                <button className="btn small secondary" onClick={() => edit(p)}>Edit</button>
                <button className="btn small danger-btn" onClick={() => remove(p)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </>
  );
}

function OrdersTab() {
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [msg, setMsg] = useState(null);

  const load = useCallback(() => {
    api.get('/seller/orders', { params: { page, size: 8 } }).then((r) => setData(r.data)).catch((e) => setMsg({ kind: 'error', text: errMsg(e) }));
  }, [page]);
  useEffect(load, [load]);

  const advance = async (id, status) => {
    try { await api.put(`/orders/${id}/status`, { status }); load(); }
    catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };

  return (
    <>
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      {data.content.length === 0 && <p>No orders containing your products yet.</p>}
      {data.content.map((o) => (
        <div className="order" key={o.id}>
          <div className="order-head">
            <strong>Order #{o.id}</strong>
            <span className={statusClass(o.status)}>{o.status}</span>
            <span className="muted">{o.customerName}</span>
            <span className="muted">{new Date(o.createdAt).toLocaleString()}</span>
          </div>
          <OrderItems items={o.items} />
          <div className="muted small-text">Ship to: {o.shippingAddress}</div>
          <div className="row">
            {o.status === 'PAID' && <button className="btn small" onClick={() => advance(o.id, 'SHIPPED')}>Mark shipped</button>}
            {o.status === 'SHIPPED' && <button className="btn small" onClick={() => advance(o.id, 'DELIVERED')}>Mark delivered</button>}
            {o.status === 'PLACED' && <span className="muted small-text">Waiting for customer payment</span>}
          </div>
        </div>
      ))}
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </>
  );
}

export default function SellerDashboard() {
  const { user } = useAuth();
  const [tab, setTab] = useState('products');
  return (
    <div className="card">
      <h2>Seller dashboard</h2>
      <div className="tabs">
        <button className={tab === 'products' ? 'active' : ''} onClick={() => setTab('products')}>My products</button>
        <button className={tab === 'orders' ? 'active' : ''} onClick={() => setTab('orders')}>Orders</button>
      </div>
      {tab === 'products' ? <ProductsTab approved={user.sellerApproved} /> : <OrdersTab />}
    </div>
  );
}
