import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errMsg, money } from '../api.js';
import Alert from '../components/Alert.jsx';

export default function Cart() {
  const [cart, setCart] = useState(null);
  const [error, setError] = useState('');

  const run = async (fn) => {
    setError('');
    try { const { data } = await fn(); setCart(data); } catch (e) { setError(errMsg(e)); }
  };

  useEffect(() => { run(() => api.get('/cart')); }, []);

  if (!cart) return error ? <Alert>{error}</Alert> : <p>Loading...</p>;

  return (
    <div className="card">
      <h2>Shopping cart</h2>
      <Alert>{error}</Alert>
      {cart.items.length === 0 ? (
        <p>Your cart is empty. <Link to="/">Continue shopping</Link></p>
      ) : (
        <>
          <table className="table">
            <thead><tr><th>Product</th><th>Price</th><th>Qty</th><th>Total</th><th /></tr></thead>
            <tbody>
              {cart.items.map((i) => (
                <tr key={i.productId}>
                  <td><Link to={`/products/${i.productId}`}>{i.name}</Link></td>
                  <td>{money(i.unitPrice)}</td>
                  <td>
                    <input type="number" className="qty" min="1" max={i.stock} defaultValue={i.quantity} key={i.quantity}
                           onBlur={(e) => { const q = Number(e.target.value); if (q >= 1 && q !== i.quantity) run(() => api.put(`/cart/items/${i.productId}`, { quantity: q })); }} />
                  </td>
                  <td>{money(i.lineTotal)}</td>
                  <td><button className="btn small danger-btn" onClick={() => run(() => api.delete(`/cart/items/${i.productId}`))}>Remove</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="summary">
            <strong>Subtotal: {money(cart.subtotal)}</strong>
            <Link className="btn" to="/checkout">Proceed to checkout</Link>
          </div>
        </>
      )}
    </div>
  );
}
