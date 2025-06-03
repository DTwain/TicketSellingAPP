// Match List Component
function MatchList({ matches, loading, onEdit, onDelete, onRefresh, userRole }) {
    if (loading) {
        return (
            <div className="card">
                <div className="loading">
                    🔄 Loading basketball matches...
                </div>
            </div>
        );
    }

    return (
        <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2>
                    {userRole === 'seller' ? '🎯 Match Management' : '🏀 Available Matches'} ({matches.length})
                </h2>
                <button className="btn btn-success" onClick={onRefresh}>
                    🔄 Refresh
                </button>
            </div>

            {matches.length === 0 ? (
                <div style={{ textAlign: 'center', color: '#718096', fontSize: '1.1rem', padding: '40px' }}>
                    {userRole === 'seller'
                        ? '🏀 No matches found. Create your first exciting basketball match above!'
                        : '🏀 No matches available at the moment. Check back soon for exciting games!'
                    }
                </div>
            ) : (
                <div className="matches-grid">
                    {matches.map(match => (
                        <MatchCard
                            key={match.id}
                            match={match}
                            onEdit={onEdit}
                            onDelete={onDelete}
                            userRole={userRole}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}