import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Products from './pages/Products.jsx';
import ProductDetail from './pages/ProductDetail.jsx';
import Cart from './pages/Cart.jsx';
import Wishlist from './pages/Wishlist.jsx';
import Checkout from './pages/Checkout.jsx';
import Orders from './pages/Orders.jsx';
import SellerDashboard from './pages/SellerDashboard.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';

export default function App() {
  const customer = ['CUSTOMER'];
  return (
    <>
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/" element={<Products />} />
          <Route path="/products/:id" element={<ProductDetail />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/cart" element={<ProtectedRoute roles={customer}><Cart /></ProtectedRoute>} />
          <Route path="/wishlist" element={<ProtectedRoute roles={customer}><Wishlist /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute roles={customer}><Checkout /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute roles={customer}><Orders /></ProtectedRoute>} />
          <Route path="/seller" element={<ProtectedRoute roles={['SELLER']}><SellerDashboard /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboard /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="footer">MultiMart demo - Spring Boot + React</footer>
    </>
  );
}
