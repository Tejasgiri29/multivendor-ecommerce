export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button className="btn small secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="btn small secondary" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}
