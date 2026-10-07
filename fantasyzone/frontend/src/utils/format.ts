export const errMsg = (e: unknown, fallback = 'Something went wrong') => (e instanceof Error ? e.message : fallback);
export const score = (n: number | null) => (n === null ? '-' : n.toFixed(2));
export const record = (w: number, l: number, t: number) => (t > 0 ? `${w}-${l}-${t}` : `${w}-${l}`);
