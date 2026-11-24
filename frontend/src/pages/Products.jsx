import { useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';
import Alert from '../components/Alert.jsx';
import Pagination from '../components/Pagination.jsx';
import ProductCard from '../components/ProductCard.jsx';

const EMPTY = { keyword: '', categoryId: '', minPrice: '', maxPrice: '', sort: 'newest' };

export default function Products() {
  const [categories, setCategories] = useState([]);
  const [draft, setDraft] = useState(EMPTY);        // values in the form
  const [filters, setFilters] = useState(EMPTY);    // applied values
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState(null);

  useEffect(() => { api.get('/categories').then((r) => setCategories(r.data)).catch(() => {}); }, []);

  useEffect(() => {
    const params = { page, size: 8, sort: filters.sort };
    ['keyword', 'categoryId', 'minPrice', 'maxPrice'].forEach((k) => { if (filters[k] !== '') params[k] = filters[k]; });
    setLoading(true);
    api.get('/products', { params })
      .then((r) => setData(r.data))
      .catch((e) => setMsg({ kind: 'error', text: errMsg(e) }))
      .finally(() => setLoading(false));
  }, [filters, page]);

  const apply = (e) => { e.preventDefault(); setPage(0); setFilters(draft); };
  const reset = () => { setDraft(EMPTY); setFilters(EMPTY); setPage(0); };
  const set = (k) => (e) => setDraft({ ...draft, [k]: e.target.value });

  return (
    <>
      <form className="filters card" onSubmit={apply}>
        <input placeholder="Search products..." value={draft.keyword} onChange={set('keyword')} />
        <select value={draft.categoryId} onChange={set('categoryId')}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input type="number" min="0" step="0.01" placeholder="Min price" value={draft.minPrice} onChange={set('minPrice')} />
        <input type="number" min="0" step="0.01" placeholder="Max price" value={draft.maxPrice} onChange={set('maxPrice')} />
        <select value={draft.sort} onChange={set('sort')}>
          <option value="newest">Newest</option>
          <option value="price_asc">Price: low to high</option>
          <option value="price_desc">Price: high to low</option>
          <option value="name">Name A-Z</option>
        </select>
        <button className="btn">Apply</button>
        <button type="button" className="btn secondary" onClick={reset}>Reset</button>
      </form>

      {msg && <Alert kind={msg.kind}>{msg.text}</Alert>}
      <p className="muted">{loading ? 'Loading...' : `${data.totalElements} product(s) found`}</p>
      <div className="grid">
        {data.content.map((p) => <ProductCard key={p.id} product={p} onMessage={(kind, text) => setMsg({ kind, text })} />)}
      </div>
      {!loading && data.content.length === 0 && <div className="card center">No products match your filters.</div>}
      <Pagination page={data.page ?? page} totalPages={data.totalPages} onChange={setPage} />
    </>
  );
}
