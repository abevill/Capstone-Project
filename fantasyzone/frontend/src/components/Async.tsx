import type { ReactNode } from 'react';
import type { FetchState } from '../hooks/useFetch';
import { ErrorMessage, Skeleton } from './Feedback';

interface Props<T> { state: FetchState<T>; lines?: number; children: (data: T) => ReactNode }

/** Renders a skeleton while loading, an error with retry on failure, and the content otherwise. */
export default function Async<T>({ state, lines = 4, children }: Props<T>) {
  if (state.error) return <ErrorMessage message={state.error} onRetry={state.reload} />;
  if (state.data === null) return state.loading ? <Skeleton lines={lines} /> : null;
  return <>{children(state.data)}</>;
}
