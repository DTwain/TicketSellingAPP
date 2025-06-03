function App() {
    const [isAuthenticated, setIsAuthenticated] = React.useState(false);
    const [currentUser, setCurrentUser] = React.useState(null);
    const [matches, setMatches] = React.useState([]);
    const [loading, setLoading] = React.useState(true);
    const [error, setError] = React.useState(null);
    const [success, setSuccess] = React.useState(null);
    const [editingMatch, setEditingMatch] = React.useState(null);
    const [websocketStatus, setWebsocketStatus] = React.useState('disconnected');
    const [statusMessage, setStatusMessage] = React.useState('');
    const websocket = React.useRef(null);

    const API_BASE = 'http://localhost:8080/api';
    const WS_URL = 'ws://localhost:8080/ws/matches';

    // Check authentication on app load
    React.useEffect(() => {
        checkAuthentication();
    }, []);

    // DEBUG: State change logging
    React.useEffect(() => {
        console.log('🔍 STATE-CHANGE: isAuthenticated changed to:', isAuthenticated);
    }, [isAuthenticated]);

    React.useEffect(() => {
        console.log('🔍 STATE-CHANGE: currentUser changed to:', currentUser?.username);
    }, [currentUser]);

    React.useEffect(() => {
        console.log('🔍 STATE-CHANGE: matches count changed to:', matches.length);
    }, [matches.length]);

    React.useEffect(() => {
        console.log('🔍 STATE-CHANGE: websocketStatus changed to:', websocketStatus);
    }, [websocketStatus]);

    // Clear messages after timeout
    React.useEffect(() => {
        if (success) {
            const timer = setTimeout(() => setSuccess(null), 4000);
            return () => clearTimeout(timer);
        }
    }, [success]);

    React.useEffect(() => {
        if (statusMessage) {
            const timer = setTimeout(() => setStatusMessage(''), 3000);
            return () => clearTimeout(timer);
        }
    }, [statusMessage]);

    const checkAuthentication = async () => {
        // Always clear localStorage on page load to force fresh login
        localStorage.clear();

        // Check for force logout parameter
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('logout') === 'true') {
            AuthUtils.removeToken();
            AuthUtils.removeUser();
            setLoading(false);
            return;
        }

        // Always force fresh login - don't check for existing tokens
        setLoading(false);
        return;
    };

    const testWebSocketConnection = async () => {
        try {
            // Test if WebSocket endpoint is reachable
            const testWs = new WebSocket(WS_URL);

            testWs.onopen = () => {
                console.log('✅ TEST: WebSocket endpoint is reachable');
                setStatusMessage('WebSocket endpoint test: SUCCESS ✅');
                testWs.close();
            };

            testWs.onerror = (error) => {
                console.error('❌ TEST: WebSocket endpoint test failed:', error);
                setStatusMessage('WebSocket endpoint test: FAILED ❌');
            };

            testWs.onclose = (event) => {
                console.log('🔌 TEST: Test connection closed:', event.code, event.reason);
            };

        } catch (err) {
            console.error('❌ TEST: WebSocket test error:', err);
            setStatusMessage(`WebSocket test error: ${err.message}`);
        }
    };

    const handleLogin = (userData) => {
        console.log('🔑 LOGIN: Starting handleLogin process');
        console.log('🔑 LOGIN: User data received:', userData);

        setIsAuthenticated(true);
        setCurrentUser(userData.user);
        AuthUtils.setToken(userData.token);
        AuthUtils.setUser(userData.user);
        setSuccess('Login successful! Welcome to Basketball Ticket Shop!');

        console.log('🔑 LOGIN: About to load matches');
        loadMatches();

        // Add delay before WebSocket initialization to ensure state is updated
        setTimeout(() => {
            console.log('🔑 LOGIN: About to init WebSocket after delay');
            console.log('🔑 LOGIN: isAuthenticated state:', isAuthenticated);
            console.log('🔑 LOGIN: currentUser state:', currentUser);
            initWebSocket();
        }, 500); // 500ms delay

        console.log('🔑 LOGIN: handleLogin completed');
    };

    const handleLogout = () => {
        setIsAuthenticated(false);
        setCurrentUser(null);
        setMatches([]);
        AuthUtils.removeToken();
        AuthUtils.removeUser();
        if (websocket.current) {
            websocket.current.close();
        }
        setWebsocketStatus('disconnected');
        setSuccess('Logged out successfully!');
    };

    const loadMatches = async () => {
        console.log('📊 LOAD-MATCHES: Starting loadMatches');
        console.log('📊 LOAD-MATCHES: isAuthenticated =', isAuthenticated);
        console.log('📊 LOAD-MATCHES: currentUser =', currentUser?.username);

        if (!isAuthenticated) {
            console.log('❌ LOAD-MATCHES: Not authenticated, skipping');
            return;
        }

        try {
            setLoading(true);
            setError(null);

            console.log('📊 LOAD-MATCHES: Making API request to', `${API_BASE}/matches`);

            const response = await fetch(`${API_BASE}/matches`, {
                headers: AuthUtils.getAuthHeaders()
            });

            console.log('📊 LOAD-MATCHES: Response status:', response.status);

            if (!response.ok) {
                if (response.status === 401) {
                    console.log('❌ LOAD-MATCHES: 401 Unauthorized, logging out');
                    handleLogout();
                    return;
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('📊 LOAD-MATCHES: Received', data.length, 'matches');

            setMatches(data);
            if (data.length > 0) {
                setSuccess(`Loaded ${data.length} basketball matches!`);
            }

            console.log('✅ LOAD-MATCHES: Successfully loaded matches');

        } catch (err) {
            console.error('❌ LOAD-MATCHES: Error:', err);
            setError(`Failed to load matches: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    const initWebSocket = () => {
        console.log('🚀 WEBSOCKET: initWebSocket called');
        console.log('🚀 WEBSOCKET: isAuthenticated =', isAuthenticated);
        console.log('🚀 WEBSOCKET: currentUser =', currentUser);
        console.log('🚀 WEBSOCKET: WS_URL =', WS_URL);

        // Check if we have authentication token
        const token = AuthUtils.getToken();
        if (!token) {
            console.log('❌ WEBSOCKET: Skipping - no authentication token');
            return;
        }

        // Close existing WebSocket if any
        if (websocket.current && websocket.current.readyState !== WebSocket.CLOSED) {
            console.log('🔌 WEBSOCKET: Closing existing connection');
            websocket.current.close();
        }

        try {
            console.log('🔗 WEBSOCKET: Setting status to connecting');
            setWebsocketStatus('connecting');
            setStatusMessage('Connecting to real-time updates...');

            console.log('🔗 WEBSOCKET: Creating WebSocket object');
            websocket.current = new WebSocket(WS_URL);
            console.log('🔗 WEBSOCKET: WebSocket object created:', websocket.current);
            console.log('🔗 WEBSOCKET: Initial ready state:', websocket.current.readyState);

            websocket.current.onopen = () => {
                console.log('✅ WEBSOCKET: onopen event fired');
                console.log('✅ WEBSOCKET: Connection ready state:', websocket.current.readyState);
                setWebsocketStatus('connected');
                setStatusMessage('Real-time updates enabled! 🚀');
            };

            websocket.current.onmessage = (event) => {
                try {
                    console.log('📨 WEBSOCKET: Message received:', event.data);
                    const data = JSON.parse(event.data);

                    console.log('📨 WEBSOCKET: Parsed message type:', data.type);
                    console.log('📨 WEBSOCKET: Full message data:', data);

                    if (data.type === 'MATCH_UPDATED') {
                        console.log('🔄 WEBSOCKET: Match updated detected, operation:', data.operation);
                        console.log('🔄 WEBSOCKET: Current isAuthenticated:', isAuthenticated);
                        console.log('🔄 WEBSOCKET: Current token exists:', !!AuthUtils.getToken());

                        // Force refresh matches with better error handling
                        refreshMatchesFromWebSocket(data.operation);

                    } else if (data.type === 'CONNECTION_ESTABLISHED') {
                        console.log('🔗 WEBSOCKET: Connection confirmed');
                        setStatusMessage('Real-time connection established! 🎉');
                    } else {
                        console.log('📨 WEBSOCKET: Unknown message type:', data.type);
                    }
                } catch (err) {
                    console.error('❌ WEBSOCKET: Error parsing message:', err);
                    console.error('❌ WEBSOCKET: Raw message was:', event.data);
                }
            };

            websocket.current.onerror = (error) => {
                console.error('❌ WEBSOCKET: Error event:', error);
                console.log('❌ WEBSOCKET: Ready state during error:', websocket.current?.readyState);
                setWebsocketStatus('error');
                setStatusMessage('Real-time updates connection failed');
            };

            websocket.current.onclose = (event) => {
                console.log('🔌 WEBSOCKET: Close event. Code:', event.code, 'Reason:', event.reason);
                console.log('🔌 WEBSOCKET: Was clean close:', event.wasClean);
                setWebsocketStatus('disconnected');

                if (event.code !== 1000 && AuthUtils.getToken()) {
                    setStatusMessage('Connection lost. Retrying in 3 seconds...');
                    setTimeout(() => {
                        const currentToken = AuthUtils.getToken();
                        if (currentToken && (!websocket.current || websocket.current.readyState === WebSocket.CLOSED)) {
                            console.log('🔄 WEBSOCKET: Attempting reconnection...');
                            initWebSocket();
                        }
                    }, 3000);
                }
            };

            console.log('🔗 WEBSOCKET: Event handlers attached');

            // Add connection timeout
            setTimeout(() => {
                if (websocket.current && websocket.current.readyState === WebSocket.CONNECTING) {
                    console.log('⏰ WEBSOCKET: Connection timeout, ready state still CONNECTING');
                    websocket.current.close();
                    setWebsocketStatus('error');
                    setStatusMessage('Connection timeout - retrying...');
                    setTimeout(initWebSocket, 2000);
                }
            }, 10000); // 10 second timeout

        } catch (err) {
            console.error('❌ WEBSOCKET: Initialization error:', err);
            setWebsocketStatus('error');
            setStatusMessage(`Connection failed: ${err.message}`);
        }
    };

    const refreshMatchesFromWebSocket = async (operation) => {
        try {
            console.log('🔄 WS-REFRESH: Starting match refresh for operation:', operation);

            // Check if we have authentication
            const token = AuthUtils.getToken();
            if (!token) {
                console.error('❌ WS-REFRESH: No authentication token available');
                setStatusMessage('Update received but not authenticated');
                return;
            }

            console.log('🔄 WS-REFRESH: Token available, making API call');

            const response = await fetch(`${API_BASE}/matches`, {
                headers: AuthUtils.getAuthHeaders()
            });

            console.log('🔄 WS-REFRESH: API response status:', response.status);

            if (!response.ok) {
                if (response.status === 401) {
                    console.error('❌ WS-REFRESH: Authentication failed, logging out');
                    handleLogout();
                    return;
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('🔄 WS-REFRESH: Received', data.length, 'matches from API');

            // Update matches state
            setMatches(data);

            // Show success message
            const action = operation ? operation.toLowerCase() : 'updated';
            setStatusMessage(`🔄 Match ${action} - List refreshed automatically!`);

            console.log('✅ WS-REFRESH: Match refresh completed successfully');

        } catch (err) {
            console.error('❌ WS-REFRESH: Error refreshing matches:', err);
            setStatusMessage(`❌ Auto-refresh failed: ${err.message}`);
            setError(`Failed to refresh matches: ${err.message}`);
        }
    };

    const getWebSocketReadyState = () => {
        if (!websocket.current) return 'None';
        switch (websocket.current.readyState) {
            case WebSocket.CONNECTING: return 'CONNECTING (0)';
            case WebSocket.OPEN: return 'OPEN (1)';
            case WebSocket.CLOSING: return 'CLOSING (2)';
            case WebSocket.CLOSED: return 'CLOSED (3)';
            default: return 'UNKNOWN';
        }
    };

    if (loading) {
        return (
            <div className="container">
                <div className="auth-container">
                    <div className="card">
                        <div className="loading">
                            🔄 Checking authentication...
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    if (!isAuthenticated) {
        return (
            <div className="container">
                <div className="header">
                    <h1>🏀 Basketball Ticket Shop</h1>
                    <p>Your gateway to the best basketball matches - Please sign in to continue</p>
                </div>
                <AuthForm onLogin={handleLogin} onError={setError} onSuccess={setSuccess} />
                {error && <div className="error">{error}</div>}
                {success && <div className="success">{success}</div>}
            </div>
        );
    }

    return (
        <div className="container">
            <div className="header">
                <div className="auth-header">
                    <div>
                        <h1>🏀 Basketball Ticket Shop</h1>
                        <p>
                            {currentUser?.isSeller
                                ? 'Seller Dashboard - Create and manage basketball matches'
                                : 'Client Portal - Browse exciting basketball matches'
                            }
                        </p>
                    </div>
                    <div className="user-info">
                        <div className="user-avatar">
                            {currentUser?.username?.charAt(0).toUpperCase()}
                        </div>
                        <div className="user-details">
                            <div style={{fontWeight: 'bold', fontSize: '1.1rem'}}>{currentUser?.username}</div>
                            <div className={`user-role ${currentUser?.isSeller ? 'seller' : 'client'}`}>
                                {currentUser?.isSeller ? '🎫 Seller' : '👤 Client'}
                            </div>
                        </div>
                        <button className="btn btn-secondary" onClick={handleLogout}>
                            🚪 Logout
                        </button>
                    </div>
                </div>
            </div>

            {statusMessage && (
                <div className={`status websocket-${websocketStatus}`}>
                    {statusMessage}
                </div>
            )}

            {error && <div className="error">{error}</div>}
            {success && <div className="success">{success}</div>}

            {/* Enhanced debug info with test functionality */}
            <div style={{position: 'fixed', bottom: '10px', left: '10px', background: 'rgba(0,0,0,0.8)', color: 'white', padding: '10px', borderRadius: '5px', fontSize: '12px', zIndex: 1000}}>
                <div>WebSocket Status: {websocketStatus}</div>
                <div>WebSocket URL: {WS_URL}</div>
                <div>Authenticated: {isAuthenticated ? 'Yes' : 'No'}</div>
                <div>Ready State: {getWebSocketReadyState()}</div>
                <div>Has Token: {AuthUtils.getToken() ? 'Yes' : 'No'}</div>
                <div>Current User: {currentUser?.username || 'None'}</div>
                <div>Matches Count: {matches.length}</div>
                <div style={{marginTop: '5px', display: 'flex', gap: '5px', flexWrap: 'wrap'}}>
                    <button
                        style={{padding: '2px 8px', fontSize: '10px'}}
                        onClick={() => {
                            console.log('🔄 Manual WebSocket reconnect attempt');
                            initWebSocket();
                        }}
                    >
                        Reconnect
                    </button>
                    <button
                        style={{padding: '2px 8px', fontSize: '10px'}}
                        onClick={testWebSocketConnection}
                    >
                        Test WS
                    </button>
                    <button
                        style={{padding: '2px 8px', fontSize: '10px'}}
                        onClick={() => {
                            console.log('🔄 Manual match refresh');
                            loadMatches();
                        }}
                    >
                        Refresh
                    </button>
                    <button
                        style={{padding: '2px 8px', fontSize: '10px'}}
                        onClick={() => {
                            console.log('🧪 Simulating WebSocket message');
                            refreshMatchesFromWebSocket('TEST');
                        }}
                    >
                        Test Refresh
                    </button>
                </div>
            </div>

            {currentUser?.isSeller ? (
                <SellerDashboard
                    matches={matches}
                    loading={loading}
                    editingMatch={editingMatch}
                    setEditingMatch={setEditingMatch}
                    websocketStatus={websocketStatus}
                    websocket={websocket}
                    initWebSocket={initWebSocket}
                    onSuccess={(message) => {
                        loadMatches();
                        setSuccess(message);
                        setEditingMatch(null);
                    }}
                    onError={setError}
                    onRefresh={loadMatches}
                />
            ) : (
                <ClientDashboard
                    matches={matches}
                    loading={loading}
                    onRefresh={loadMatches}
                />
            )}
        </div>
    );
}