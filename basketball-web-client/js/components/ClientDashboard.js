// Client Dashboard Component
function ClientDashboard({ matches, loading, onRefresh }) {
    return (
        <>
            <div className="card client-info">
                <div className="role-badge client">Client</div>
                <h2>🏀 Available Basketball Matches</h2>
                <p>Welcome to your <strong>client portal</strong>! Browse exciting basketball matches
                    and enjoy real-time updates when new matches are added or schedules change.</p>
            </div>

            <MatchList
                matches={matches}
                loading={loading}
                onRefresh={onRefresh}
                userRole="client"
            />
        </>
    );
}