import type { ReactNode } from 'react';

interface Props { title?: string; children: ReactNode; className?: string }

export default function Card({ title, children, className = '' }: Props) {
  return (
    <section className={`card ${className}`.trim()}>
      {title && <h2 className="card-title">{title}</h2>}
      {children}
    </section>
  );
}
