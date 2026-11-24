import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errMsg, money } from '../api.js';
import Alert from '../components/Alert.jsx';

export default function Wishlist() {
  const [items, setItems] = useState([]);
  const [msg, setMsg] = useState(null);

  useEffect(() => { api.get('/wishlist').then((r) => setItems(r.data)).catch((e) => setMsg({ kind: 'error', text: errMsg(e) })); }, []);

  const remove = async (id) => {
    try { setItems((await api.delete(`/wishlist/${id}`)).data); } catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };
  const moveToCart = async (p) => {
    try {
      await api.post('/cart/items', { productId: p.id, quantity: 1 });
      setMsg({ kind: 'success', text: `Added "${p.name}" to cart` });
    } catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };

  return (
    <div className="card">
      <h2>My wishlist</h2>
      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      {items.length === 0 ? <p>Nothing saved yet. <Link to="/">Browse products</Link></p> : (
        <table className="table">
          <tbody>
            {items.map((p) => (
              <tr key={p.id}>
                <td><Link to={`/products/${p.id}`}>{p.name}</Link></td>
                <td>{money(p.price)}</td>
                <td>{p.stock > 0 ? `${p.stock} in stock` : <span className="danger">Out of stock</span>}</td>
                <td className="actions">
                  <button className="btn small" disabled={p.stock <= 0} onClick={() => moveToCart(p)}>Add to cart</button>
                  <button className="btn small danger-btn" onClick={() => remove(p.id)}>Remove</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
