import Button from './Button';

interface Props { page: number; totalPages: number; onChange: (page: number) => void }

export default function Pagination({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <Button variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</Button>
      <span aria-live="polite">Page {page + 1} of {totalPages}</span>
      <Button variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</Button>
    </nav>
  );
}
