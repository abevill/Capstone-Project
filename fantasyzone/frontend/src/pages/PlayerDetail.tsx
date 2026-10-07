import { Link, useParams } from 'react-router-dom';
import Async from '../components/Async';
import Card from '../components/Card';
import Table, { type Column } from '../components/Table';
import { useFetch } from '../hooks/useFetch';
import { playersApi } from '../services/api';
import type { WeeklyPlayerStats } from '../types';

export default function PlayerDetail() {
    const { playerId } = useParams();

    const id = Number(playerId);

    const player = useFetch(
        () => playersApi.get(id),
        [id]
    );
    const stats = useFetch(
        () => playersApi.getStats(id),
        [id]
    );

    const columns: Column<WeeklyPlayerStats>[] = [
        {
            header: 'Week',
            render: (stat) => `Week ${stat.week}`,
        },
        {
            header: 'Pass Yds',
            render: (stat) => stat.passingYards,
        },
        {
            header: 'Pass TD',
            render: (stat) => stat.passingTouchdowns,
        },
        {
            header: 'INT',
            render: (stat) => stat.interceptions,
        },
        {
            header: 'Rush Yds',
            render: (stat) => stat.rushingYards,
        },
        {
            header: 'Rush TD',
            render: (stat) => stat.rushingTouchdowns,
        },
        {
            header: 'Receptions',
            render: (stat) => stat.receptions,
        },
        {
            header: 'Rec Yds',
            render: (stat) => stat.receivingYards,
        },
        {
            header: 'Rec TD',
            render: (stat) => stat.receivingTouchdowns,
        },
        {
            header: 'Fantasy Points',
            render: (stat) => stat.fantasyPoints.toFixed(1),
        },
    ];

    if (!Number.isInteger(id) || id <= 0) {
        return (
            <Card title="Invalid player">
                <p>The player ID is invalid.</p>
                <Link to="/players">Back to players</Link>
            </Card>
        );
    }

    return (
        <Async state={player}>
            {(data: any) => (
                <Card title={data.name}>
                    <p className="muted">
                        {data.position === 'PK' ? 'K' : data.position}
                        {' · '}
                        {data.nflTeam ?? 'No NFL team'}
                    </p>

                    <h2>
                        Player Statistics
                    </h2>

                    <p className="muted">
                        NFL statistics are synchronized from the sports data provider.
                        Fantasy points are calculated by FantasyZone.
                    </p>

                    <Table
                        caption={`${data.name} weekly statistics`}
                        columns={columns}
                        rows={((stats.data as any[]) ?? []).map((s: any) => ({
                            season: s.season,
                            week: s.week,
                            fantasyPoints: s.fantasyPoints,

                            passingYards: s.stats.passingYards,
                            passingTouchdowns: s.stats.passingTouchdowns,
                            interceptions: s.stats.interceptions,

                            rushingYards: s.stats.rushingYards,
                            rushingTouchdowns: s.stats.rushingTouchdowns,

                            receptions: s.stats.receptions,
                            receivingYards: s.stats.receivingYards,
                            receivingTouchdowns: s.stats.receivingTouchdowns,

                            fumblesLost: s.stats.fumblesLost
                        }))}
                        rowKey={(stat) => `${stat.season}-${stat.week}`}
                        emptyText="No weekly statistics are available for this player."
                    />

                    <div className="row-actions">
                        <Link to="/players">Back to NFL players</Link>
                    </div>
                </Card>
            )}
        </Async>
    );
}