// Seller Dashboard Component
function SellerDashboard({ matches, loading, editingMatch, setEditingMatch, websocketStatus, websocket, initWebSocket, onSuccess, onError, onRefresh }) {

    const deleteMatch = async (id) => {
        if (!confirm('🗑️ Are you sure you want to delete this match?\n\nThis action cannot be undone and will notify all connected clients.')) {
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/api/matches/${id}`, {
                method: 'DELETE',
                headers: AuthUtils.getAuthHeaders()
            });

            if (!response.ok) {
                if (response.status === 403) {
                    throw new Error('Access denied: Seller privileges required');
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            onSuccess('🗑️ Match deleted successfully! All clients notified.');
        } catch (err) {
            onError(`Failed to delete match: ${err.message}`);
        }
    };

    return (
        <>
            <div className="card seller-only">
                <div className="role-badge">Seller</div>
                <h2>🎟️ Match Management Center</h2>
                <p>As a <strong>seller</strong>, you have full control to create, edit, and delete basketball matches.
                    Your changes will be broadcast in real-time to all connected clients!</p>

                <div style={{marginTop: '15px', display: 'flex', gap: '10px', alignItems: 'center'}}>
                    <button
                        className="btn btn-secondary"
                        onClick={() => {
                            if (websocket.current && websocket.current.readyState === WebSocket.OPEN) {
                                console.log('WebSocket is connected and ready');
                                onSuccess('WebSocket connection verified! ✅');
                            } else {
                                console.log('WebSocket not connected, attempting reconnection...');
                                initWebSocket();
                                onError('WebSocket reconnection attempted. Check status in a few seconds.');
                            }
                        }}
                    >
                        🔗 Test Connection
                    </button>
                    <span style={{fontSize: '0.9rem', color: '#666'}}>
                        Status: <strong style={{color: websocketStatus === 'connected' ? '#4CAF50' : '#f44336'}}>
                            {websocketStatus.toUpperCase()}
                        </strong>
                    </span>
                </div>
            </div>

            <MatchForm
                editingMatch={editingMatch}
                setEditingMatch={setEditingMatch}
                onSuccess={onSuccess}
                onError={onError}
            />

            <MatchList
                matches={matches}
                loading={loading}
                onEdit={setEditingMatch}
                onDelete={deleteMatch}
                onRefresh={onRefresh}
                userRole="seller"
            />
        </>
    );
}