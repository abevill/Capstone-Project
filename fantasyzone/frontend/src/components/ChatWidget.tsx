import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import Button from './Button';
import Select from './Select';
import { useAuth } from '../context/AuthContext';
import { useFetch } from '../hooks/useFetch';
import { assistantApi, meApi } from '../services/api';
import { errMsg } from '../utils/format';

interface ChatMessage {
    id: number;
    role: 'user' | 'assistant' | 'error';
    text: string;
    dataUsed?: string[];
}

const SUGGESTIONS = [
    'Who should I start this week?',
    "Summarize my team's strengths and weaknesses",
    'Which of my players has been scoring best lately?',
];

/** Floating AI assistant. Only shown to logged-in users. The API key never reaches the browser; this only calls our backend. */
export default function ChatWidget() {
    const { user } = useAuth();
    // key=user id: logging in as someone else starts with a clean conversation
    return user ? <ChatBox key={user.userId} /> : null;
}

function ChatBox() {
    const [open, setOpen] = useState(false);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [input, setInput] = useState('');
    const [busy, setBusy] = useState(false);
    const [teamChoice, setTeamChoice] = useState<string | null>(null); // null = not chosen, so use the first team
    const teams = useFetch(() => meApi.teams(), []);
    const nextId = useRef(1);
    const endRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLTextAreaElement>(null);

    const myTeams = teams.data ?? [];
    const selected = teamChoice ?? (myTeams.length > 0 ? String(myTeams[0].id) : 'none');
    const teamOptions = [
        { value: 'none', label: 'No team (general question)' },
        ...myTeams.map((t) => ({ value: String(t.id), label: t.name })),
    ];

    useEffect(() => {
        if (open) inputRef.current?.focus();
    }, [open]);

    useEffect(() => {
        endRef.current?.scrollIntoView({ block: 'end' });
    }, [messages, busy, open]);

    async function send(text: string) {
        const question = text.trim();
        if (!question || busy) return;
        setMessages((m) => [...m, { id: nextId.current++, role: 'user', text: question }]);
        setInput('');
        setBusy(true);
        try {
            const res = await assistantApi.ask({
                question,
                teamId: selected === 'none' ? undefined : Number(selected),
            });
            setMessages((m) => [...m, { id: nextId.current++, role: 'assistant', text: res.answer, dataUsed: res.dataUsed }]);
        } catch (e) {
            setMessages((m) => [...m, { id: nextId.current++, role: 'error', text: errMsg(e, 'The assistant could not answer.') }]);
        } finally {
            setBusy(false);
        }
    }

    function onSubmit(e: FormEvent) {
        e.preventDefault();
        send(input);
    }

    function onInputKey(e: KeyboardEvent<HTMLTextAreaElement>) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault(); // Enter sends, Shift+Enter adds a new line
            send(input);
        }
    }

    function onPanelKey(e: KeyboardEvent<HTMLDivElement>) {
        if (e.key === 'Escape') setOpen(false);
    }

    return (
        <>
            {open && (
                <div className="chat-panel" role="dialog" aria-label="AI fantasy assistant" onKeyDown={onPanelKey}>
                    <div className="chat-header">
                        <div className="chat-title-row">
                            <h2>AI fantasy assistant</h2>
                            {messages.length > 0 && (
                                <button type="button" className="chat-clear" onClick={() => setMessages([])}>Clear</button>
                            )}
                        </div>
                        <Select label="Ask about" value={selected} onChange={(e) => setTeamChoice(e.target.value)} options={teamOptions} />
                    </div>

                    <div className="chat-messages" role="log" aria-live="polite" aria-label="Conversation">
                        {messages.length === 0 && (
                            <>
                                <p className="muted">
                                    Ask about your lineup, compare players, or get a quick read on your team. I only use FantasyZone data.
                                </p>
                                {myTeams.length === 0 && !teams.loading && (
                                    <p className="muted">Join a league and create a team to ask about your roster.</p>
                                )}
                                <div className="chat-chips">
                                    {SUGGESTIONS.map((s) => (
                                        <button key={s} type="button" className="chat-chip" disabled={busy} onClick={() => send(s)}>{s}</button>
                                    ))}
                                </div>
                            </>
                        )}
                        {messages.map((m) => (
                            <div key={m.id} className={`chat-bubble ${m.role}`} role={m.role === 'error' ? 'alert' : undefined}>
                                {m.text}
                                {m.dataUsed && m.dataUsed.length > 0 && (
                                    <details className="chat-used">
                                        <summary>Data used</summary>
                                        <ul>{m.dataUsed.map((d, i) => <li key={i}>{d}</li>)}</ul>
                                    </details>
                                )}
                            </div>
                        ))}
                        {busy && <div className="chat-typing" role="status">Thinking...</div>}
                        <div ref={endRef} />
                    </div>

                    <form className="chat-form" onSubmit={onSubmit}>
            <textarea
                ref={inputRef}
                rows={2}
                maxLength={500}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={onInputKey}
                placeholder="Ask a question..."
                aria-label="Your question"
            />
                        <Button type="submit" loading={busy} disabled={!input.trim()}>Send</Button>
                    </form>
                    <p className="chat-note">AI answers can be wrong. They use only FantasyZone data.</p>
                </div>
            )}

            <button
                type="button"
                className="chat-fab"
                onClick={() => setOpen((o) => !o)}
                aria-label={open ? 'Close AI assistant' : 'Open AI assistant'}
                aria-expanded={open}
            >
                {open ? (
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                ) : (
                    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    </svg>
                )}
            </button>
        </>
    );
}