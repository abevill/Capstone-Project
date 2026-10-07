import type { ReactNode } from 'react';

export interface Column<T> { header: string; render: (row: T) => ReactNode }

interface Props<T> {
  caption: string;
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  emptyText?: string;
}

export default function Table<T>({ caption, columns, rows, rowKey, emptyText = 'Nothing to show yet.' }: Props<T>) {
  if (rows.length === 0) return <p className="muted">{emptyText}</p>;
  return (
    <div className="table-wrap" role="region" aria-label={caption} tabIndex={0}>
      <table>
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr>{columns.map((c) => <th key={c.header} scope="col">{c.header}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={rowKey(r)}>{columns.map((c) => <td key={c.header}>{c.render(r)}</td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
