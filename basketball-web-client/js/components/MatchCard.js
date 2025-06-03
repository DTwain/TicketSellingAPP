// Individual Match Card Component
function MatchCard({ match, onEdit, onDelete, userRole }) {
    const formatDate = (dateString) => {
        try {
            const date = new Date(dateString);
            return date.toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                weekday: 'short'
            });
        } catch {
            return 'Date not available';
        }
    };

    return (
        <div className="match-card">
            <div className="match-teams">
                🏀 {match.teamA} vs {match.teamB}
            </div>
            <div className="match-date">
                📅 {formatDate(match.dateTime)}
            </div>
            <div className="match-price">
                💰 {match.priceRange || 'Price not available'}
            </div>
            <div className="match-availability">
                🎫 {match.availableTickets !== undefined ? `${match.availableTickets} seats available` : 'Availability unknown'}
            </div>

            {userRole === 'seller' ? (
                <div className="match-actions">
                    <button
                        className="btn btn-warning"
                        onClick={() => onEdit(match)}
                        title="Edit this match"
                    >
                        ✏️ Edit
                    </button>
                    <button
                        className="btn btn-danger"
                        onClick={() => onDelete(match.id)}
                        title="Delete this match"
                    >
                        🗑️ Delete
                    </button>
                </div>
            ) : (
                <div className="match-actions">
                    <button
                        className="btn btn-primary"
                        title="View match details"
                        onClick={() => alert(`🏀 Match Details:\n\n${match.teamA} vs ${match.teamB}\n📅 ${formatDate(match.dateTime)}\n💰 Price: ${match.priceRange || 'Not available'}\n🎫 Available seats: ${match.availableTickets || 'Unknown'}\n\n🎫 Contact a seller to purchase tickets!`)}
                    >
                        👁️ View Details
                    </button>
                </div>
            )}
        </div>
    );
}