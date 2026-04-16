document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('loginForm');
    const errorDiv = document.getElementById('errorMessage');

    // Clear any existing token
    localStorage.removeItem('token');
    localStorage.removeItem('user');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorDiv.textContent = '';

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        if (!username || !password) {
            errorDiv.textContent = 'Please enter both username and password.';
            return;
        }

        try {
            const response = await fetch('/api/auth/signin', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                const error = await response.text();
                throw new Error(error || 'Invalid credentials');
            }

            const data = await response.json();
            
            // Store token and user info
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify({
                id: data.id,
                username: data.username,
                email: data.email,
                roles: data.roles
            }));

            // Redirect based on role
            if (data.roles.includes('ROLE_ADMIN')) {
                window.location.href = '/admin-dashboard.html';
            } else if (data.roles.includes('ROLE_MANAGER')) {
                window.location.href = '/manager-dashboard.html';
            } else if (data.roles.includes('ROLE_USER')) {
                window.location.href = '/user-dashboard.html';
            } else {
                errorDiv.textContent = 'Unknown role. Contact administrator.';
            }
        } catch (error) {
            console.error('Login error:', error);
            errorDiv.textContent = error.message || 'Login failed. Please try again.';
        }
    });
});