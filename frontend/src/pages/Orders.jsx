import { useCallback, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import api, { errMsg, money } from '../api.js';
import Alert from '../components/Alert.jsx';
import Pagination from '../components/Pagination.jsx';

export const statusClass = (s) => `status status-${s.toLowerCase()}`;

export function OrderItems({ items }) {
  return (
    <ul className="plain small-text">
      {items.map((i, idx) => <li key={idx}>{i.quantity} x {i.productName} @ {money(i.unitPrice)}</li>)}
    </ul>
  );
}

export default function Orders() {
  const location = useLocation();
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [msg, setMsg] = useState(location.state?.placed ? { kind: 'success', text: 'Order placed! You can pay for it below.' } : null);

  const load = useCallback(() => {
    api.get('/orders', { params: { page, size: 5 } }).then((r) => setData(r.data)).catch((e) => setMsg({ kind: 'error', text: errMsg(e) }));
  }, [page]);
  useEffect(load, [load]);

  const act = async (id, action) => {
    try { await api.post(`/orders/${id}/${action}`); setMsg({ kind: 'success', text: `Order #${id} updated` }); load(); }
    catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };

  return (
    <div className="card">
      <h2>My orders</h2>
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      {data.content.length === 0 ? <p>No orders yet.</p> : data.content.map((o) => (
        <div className="order" key={o.id}>
          <div className="order-head">
            <strong>Order #{o.id}</strong>
            <span className={statusClass(o.status)}>{o.status}</span>
            <span className="muted">{new Date(o.createdAt).toLocaleString()}</span>
            <span className="right"><strong>{money(o.total)}</strong>{Number(o.discount) > 0 && <span className="muted"> (saved {money(o.discount)} with {o.couponCode})</span>}</span>
          </div>
          <OrderItems items={o.items} />
          <div className="muted small-text">Ship to: {o.shippingAddress}</div>
          <div className="row">
            {o.status === 'PLACED' && <button className="btn small" onClick={() => act(o.id, 'pay')}>Pay now</button>}
            {(o.status === 'PLACED' || o.status === 'PAID') && <button className="btn small danger-btn" onClick={() => window.confirm('Cancel this order?') && act(o.id, 'cancel')}>Cancel</button>}
          </div>
        </div>
      ))}
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}
