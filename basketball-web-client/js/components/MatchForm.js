// Match Form Component (Seller only)
function MatchForm({ editingMatch, setEditingMatch, onSuccess, onError }) {
    const [formData, setFormData] = React.useState({
        teamA: '',
        teamB: '',
        dateTime: ''
    });
    const [loading, setLoading] = React.useState(false);

    const API_BASE = 'http://localhost:8080/api/matches';

    React.useEffect(() => {
        if (editingMatch) {
            setFormData({
                teamA: editingMatch.teamA || '',
                teamB: editingMatch.teamB || '',
                dateTime: editingMatch.dateTime ? editingMatch.dateTime.slice(0, 16) : ''
            });
        }
    }, [editingMatch]);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.teamA.trim() || !formData.teamB.trim() || !formData.dateTime) {
            onError('Please fill in all fields');
            return;
        }

        if (formData.teamA.trim().toLowerCase() === formData.teamB.trim().toLowerCase()) {
            onError('Teams must be different!');
            return;
        }

        setLoading(true);
        try {
            const payload = {
                teamA: formData.teamA.trim(),
                teamB: formData.teamB.trim(),
                dateTime: formData.dateTime
            };

            let response;
            if (editingMatch) {
                response = await fetch(`${API_BASE}/${editingMatch.id}`, {
                    method: 'PUT',
                    headers: AuthUtils.getAuthHeaders(),
                    body: JSON.stringify(payload),
                });
            } else {
                response = await fetch(API_BASE, {
                    method: 'POST',
                    headers: AuthUtils.getAuthHeaders(),
                    body: JSON.stringify(payload),
                });
            }

            if (!response.ok) {
                if (response.status === 403) {
                    throw new Error('Access denied: Seller privileges required');
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            setFormData({ teamA: '', teamB: '', dateTime: '' });
            onSuccess(editingMatch
                ? '✅ Match updated successfully! All clients notified.'
                : '🎉 Match created successfully! All clients notified.'
            );
        } catch (err) {
            onError(`Failed to ${editingMatch ? 'update' : 'create'} match: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        setEditingMatch(null);
        setFormData({ teamA: '', teamB: '', dateTime: '' });
    };

    return (
        <div className="card">
            <h2>{editingMatch ? '✏️ Edit Match' : '➕ Add New Match'}</h2>
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="teamA">🏀 Team A:</label>
                    <input
                        type="text"
                        id="teamA"
                        value={formData.teamA}
                        onChange={(e) => setFormData({...formData, teamA: e.target.value})}
                        placeholder="e.g., Los Angeles Lakers"
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="teamB">🏀 Team B:</label>
                    <input
                        type="text"
                        id="teamB"
                        value={formData.teamB}
                        onChange={(e) => setFormData({...formData, teamB: e.target.value})}
                        placeholder="e.g., Golden State Warriors"
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="dateTime">📅 Match Date & Time:</label>
                    <input
                        type="datetime-local"
                        id="dateTime"
                        value={formData.dateTime}
                        onChange={(e) => setFormData({...formData, dateTime: e.target.value})}
                        required
                    />
                </div>
                <div className="button-group">
                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? '⏳ Processing...' : (editingMatch ? '💾 Update Match' : '🚀 Create Match')}
                    </button>
                    {editingMatch && (
                        <button type="button" className="btn btn-warning" onClick={handleCancel}>
                            ❌ Cancel Edit
                        </button>
                    )}
                </div>
            </form>
        </div>
    );
}