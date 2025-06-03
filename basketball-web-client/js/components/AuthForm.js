// Authentication Form Component
function AuthForm({ onLogin, onError, onSuccess }) {
    const [activeTab, setActiveTab] = React.useState('login');
    const [formData, setFormData] = React.useState({
        username: '',
        password: ''
    });
    const [loading, setLoading] = React.useState(false);

    const API_BASE = 'http://localhost:8080/api';

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.username.trim() || !formData.password.trim()) {
            onError('Please fill in all fields');
            return;
        }

        setLoading(true);
        try {
            const endpoint = activeTab === 'login' ? '/auth/login' : '/auth/signup';
            const response = await fetch(`${API_BASE}${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            const data = await response.json();

            if (data.success) {
                onLogin(data);
                setFormData({ username: '', password: '' });
            } else {
                onError(data.error || `${activeTab} failed`);
            }
        } catch (err) {
            onError(`${activeTab} error: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="card">
                <div className="auth-tabs">
                    <button
                        className={`auth-tab ${activeTab === 'login' ? 'active' : ''}`}
                        onClick={() => setActiveTab('login')}
                    >
                        🔑 Login
                    </button>
                    <button
                        className={`auth-tab ${activeTab === 'signup' ? 'active' : ''}`}
                        onClick={() => setActiveTab('signup')}
                    >
                        ✨ Sign Up
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="username">Username:</label>
                        <input
                            type="text"
                            id="username"
                            value={formData.username}
                            onChange={(e) => setFormData({...formData, username: e.target.value})}
                            placeholder="Enter your username"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="password">Password:</label>
                        <input
                            type="password"
                            id="password"
                            value={formData.password}
                            onChange={(e) => setFormData({...formData, password: e.target.value})}
                            placeholder="Enter your password"
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={loading}
                        style={{width: '100%'}}
                    >
                        {loading ? '⏳ Processing...' : (activeTab === 'login' ? '🚀 Login' : '🎉 Sign Up')}
                    </button>
                </form>

                {activeTab === 'signup' && (
                    <div style={{marginTop: '15px', padding: '15px', backgroundColor: '#f8f9fa', borderRadius: '8px', fontSize: '0.9rem'}}>
                        <strong>📋 Note:</strong> New accounts are created as <strong>Clients</strong> by default.
                        To become a <strong>Seller</strong>, contact an administrator.
                    </div>
                )}
            </div>
        </div>
    );
}