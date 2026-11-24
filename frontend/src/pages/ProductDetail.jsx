import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api, { errMsg, money } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import Alert from '../components/Alert.jsx';

export default function ProductDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [qty, setQty] = useState(1);
  const [msg, setMsg] = useState(null);

  useEffect(() => {
    api.get(`/products/${id}`).then((r) => setProduct(r.data)).catch((e) => setMsg({ kind: 'error', text: errMsg(e) }));
  }, [id]);

  const guard = () => {
    if (!user) { navigate('/login', { state: { from: `/products/${id}` } }); return false; }
    if (user.role !== 'CUSTOMER') { setMsg({ kind: 'error', text: 'Only customers can shop' }); return false; }
    return true;
  };

  const act = async (fn, okText) => {
    if (!guard()) return;
    try { await fn(); setMsg({ kind: 'success', text: okText }); } catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };

  if (!product) return <Alert kind={msg?.kind}>{msg?.text || 'Loading...'}</Alert>;

  return (
    <div className="card detail">
      <img src={product.imageUrl} alt={product.name} onError={(e) => { e.currentTarget.style.visibility = 'hidden'; }} />
      <div>
        <span className="badge">{product.categoryName}</span>
        <h1>{product.name}</h1>
        <p className="muted">Sold by {product.sellerName}</p>
        <div className="price big">{money(product.price)}</div>
        <p>{product.description}</p>
        <p className={product.stock > 0 ? 'muted' : 'danger'}>{product.stock > 0 ? `${product.stock} in stock` : 'Out of stock'}</p>
        {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
        <div className="row">
          <input type="number" className="qty" min="1" max={Math.max(product.stock, 1)} value={qty}
                 onChange={(e) => setQty(Math.max(1, Number(e.target.value) || 1))} />
          <button className="btn" disabled={product.stock <= 0}
                  onClick={() => act(() => api.post('/cart/items', { productId: product.id, quantity: qty }), 'Added to cart')}>Add to cart</button>
          <button className="btn secondary" onClick={() => act(() => api.post(`/wishlist/${product.id}`), 'Saved to wishlist')}>Add to wishlist</button>
        </div>
      </div>
    </div>
  );
}
