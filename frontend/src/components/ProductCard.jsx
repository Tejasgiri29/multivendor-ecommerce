import { Link, useNavigate } from 'react-router-dom';
import api, { errMsg, money } from '../api.js';
import { useAuth } from '../AuthContext.jsx';

const FALLBACK = 'data:image/svg+xml;utf8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="100%" height="100%" fill="#e5e7eb"/><text x="50%" y="50%" text-anchor="middle" fill="#9ca3af" font-size="20">No image</text></svg>');

export default function ProductCard({ product, onMessage }) {
  const { user } = useAuth();
  const navigate = useNavigate();

  const requireCustomer = () => {
    if (!user) { navigate('/login'); return false; }
    if (user.role !== 'CUSTOMER') { onMessage?.('error', 'Only customers can shop'); return false; }
    return true;
  };

  const addToCart = async () => {
    if (!requireCustomer()) return;
    try {
      await api.post('/cart/items', { productId: product.id, quantity: 1 });
      onMessage?.('success', `Added "${product.name}" to cart`);
    } catch (e) { onMessage?.('error', errMsg(e)); }
  };

  const addToWishlist = async () => {
    if (!requireCustomer()) return;
    try {
      await api.post(`/wishlist/${product.id}`);
      onMessage?.('success', `Saved "${product.name}" to wishlist`);
    } catch (e) { onMessage?.('error', errMsg(e)); }
  };

  return (
    <div className="product-card">
      <Link to={`/products/${product.id}`}>
        <img src={product.imageUrl || FALLBACK} alt={product.name} onError={(e) => { e.currentTarget.src = FALLBACK; }} />
      </Link>
      <div className="product-body">
        <span className="badge">{product.categoryName}</span>
        <h3><Link to={`/products/${product.id}`}>{product.name}</Link></h3>
        <div className="price">{money(product.price)}</div>
        <div className={product.stock > 0 ? 'muted' : 'danger'}>
          {product.stock > 0 ? `${product.stock} in stock` : 'Out of stock'}
        </div>
        <div className="row">
          <button className="btn small" disabled={product.stock <= 0} onClick={addToCart}>Add to cart</button>
          <button className="btn small secondary" onClick={addToWishlist} title="Add to wishlist">♡</button>
        </div>
      </div>
    </div>
  );
}
