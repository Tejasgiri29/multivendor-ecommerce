import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errMsg, money } from '../api.js';
import Alert from '../components/Alert.jsx';

export default function Checkout() {
  const navigate = useNavigate();
  const [cart, setCart] = useState(null);
  const [address, setAddress] = useState('');
  const [code, setCode] = useState('');
  const [applied, setApplied] = useState(null);   // validation result from the server
  const [couponMsg, setCouponMsg] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { api.get('/cart').then((r) => setCart(r.data)).catch((e) => setError(errMsg(e))); }, []);

  const applyCoupon = async () => {
    setCouponMsg(null);
    try {
      const { data } = await api.post('/coupons/validate', { code });
      setApplied(data);
      setCouponMsg({ kind: 'success', text: `Coupon ${data.code} applied: -${money(data.discount)}` });
    } catch (e) {
      setApplied(null);
      setCouponMsg({ kind: 'error', text: errMsg(e) });
    }
  };

  const place = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try {
      await api.post('/orders/checkout', { shippingAddress: address, couponCode: applied ? applied.code : null });
      navigate('/orders', { state: { placed: true } });
    } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  };

  if (!cart) return <Alert>{error}</Alert>;
  if (cart.items.length === 0) return <div className="card"><p>Your cart is empty. <Link to="/">Shop now</Link></p></div>;

  const discount = applied ? Number(applied.discount) : 0;
  return (
    <form className="card form" onSubmit={place}>
      <h2>Checkout</h2>
      <Alert>{error}</Alert>
      <ul className="plain">
        {cart.items.map((i) => <li key={i.productId}>{i.quantity} x {i.name} <span className="right">{money(i.lineTotal)}</span></li>)}
      </ul>
      <label>Shipping address
        <textarea required maxLength={500} rows={3} value={address} onChange={(e) => setAddress(e.target.value)} />
      </label>
      <label>Coupon code
        <div className="row">
          <input value={code} onChange={(e) => { setCode(e.target.value); setApplied(null); }} placeholder="e.g. WELCOME10" />
          <button type="button" className="btn secondary" disabled={!code.trim()} onClick={applyCoupon}>Apply</button>
        </div>
      </label>
      {couponMsg && <Alert kind={couponMsg.kind}>{couponMsg.text}</Alert>}
      <div className="totals">
        <div>Subtotal <span className="right">{money(cart.subtotal)}</span></div>
        <div>Discount <span className="right">-{money(discount)}</span></div>
        <div className="grand">Total <span className="right">{money(Number(cart.subtotal) - discount)}</span></div>
      </div>
      <button className="btn" disabled={busy}>{busy ? 'Placing order...' : 'Place order'}</button>
    </form>
  );
}
