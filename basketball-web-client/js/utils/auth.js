// Authentication Utilities
const AuthUtils = {
    // Use sessionStorage instead of localStorage, or memory-only storage
    getToken: () => sessionStorage.getItem('basketball_token'),

    setToken: (token) => sessionStorage.setItem('basketball_token', token),

    removeToken: () => {
        sessionStorage.removeItem('basketball_token');
        localStorage.removeItem('basketball_token'); // Clean up any existing localStorage
    },

    getUser: () => {
        const userStr = sessionStorage.getItem('basketball_user');
        return userStr ? JSON.parse(userStr) : null;
    },

    setUser: (user) => sessionStorage.setItem('basketball_user', JSON.stringify(user)),

    removeUser: () => {
        sessionStorage.removeItem('basketball_user');
        localStorage.removeItem('basketball_user'); // Clean up any existing localStorage
    },

    isAuthenticated: () => !!AuthUtils.getToken(),

    getAuthHeaders: () => ({
        'Authorization': `Bearer ${AuthUtils.getToken()}`,
        'Content-Type': 'application/json'
    }),

    // Add method to clear all storage
    clearAllStorage: () => {
        sessionStorage.clear();
        localStorage.clear();
    }
};