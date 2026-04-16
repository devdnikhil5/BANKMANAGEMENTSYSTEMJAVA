// Configuration & State 
const API_BASE = '';
let currentManagerId = null;
let deleteCallback = null;

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
    loadManagers();
    loadAdminInfo();
});

function checkAuth() {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    if (!token || !userStr) {
        window.location.href = '/login.html';
        return;
    }
    try {
        const user = JSON.parse(userStr);
        if (!user.roles.includes('ROLE_ADMIN')) {
            alert('Access denied. Admin only.');
            window.location.href = '/login.html';
            return;
        }
        document.getElementById('adminUsername').textContent = user.username;
    } catch (e) {
        window.location.href = '/login.html';
    }
}

function setupEventListeners() {
    // Navigation
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const section = link.dataset.section;
            showSection(section);
            if (section === 'managers') loadManagers();
            else if (section === 'users') loadAllUsers();
            else if (section === 'statistics') loadStatistics();
        });
    });

    // Add Manager button
    document.getElementById('addManagerBtn').addEventListener('click', () => openManagerModal());

    // Manager form submit
    document.getElementById('managerForm').addEventListener('submit', saveManager);

    // Modal close buttons
    document.querySelectorAll('.close-modal').forEach(el => el.addEventListener('click', closeModal));
    document.getElementById('cancelModalBtn').addEventListener('click', closeModal);

    // Delete confirmation
    document.getElementById('cancelDeleteBtn').addEventListener('click', () => {
        document.getElementById('confirmModal').classList.remove('active');
    });
    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) deleteCallback();
        document.getElementById('confirmModal').classList.remove('active');
    });

    // Refresh stats
    document.getElementById('refreshStatsBtn').addEventListener('click', loadStatistics);

    // Change password form
    document.getElementById('changePasswordForm').addEventListener('submit', changePassword);

    // Logout
    document.getElementById('logoutBtn').addEventListener('click', logout);
}

function showSection(sectionId) {
    document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
    document.getElementById(`${sectionId}-section`).classList.add('active');
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    document.querySelector(`[data-section="${sectionId}"]`).classList.add('active');
}

// API Helpers
async function apiRequest(url, method = 'GET', body = null) {
    const token = localStorage.getItem('token');
    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);
    const response = await fetch(API_BASE + url, options);
    if (!response.ok) {
        // Try to parse error as JSON, otherwise use text
        let errorMessage;
        try {
            const errorData = await response.json();
            errorMessage = errorData.message || errorData.error || response.statusText;
        } catch (e) {
            errorMessage = await response.text() || response.statusText;
        }
        throw new Error(errorMessage || 'Request failed');
    }
    if (response.status === 204) return null;
    return response.json();
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toastMessage');
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    setTimeout(() => toast.classList.remove('show'), 3000);
}

// Managers CRUD 
async function loadManagers() {
    try {
        const managers = await apiRequest('/api/admin/managers');
        renderManagersTable(managers);
    } catch (error) {
        showToast('Failed to load managers: ' + error.message, 'error');
    }
}

function renderManagersTable(managers) {
    const tbody = document.getElementById('managersTableBody');
    tbody.innerHTML = managers.map(m => `
        <tr>
            <td>${m.id}</td>
            <td>${m.username}</td>
            <td>${m.fullName}</td>
            <td>${m.email}</td>
            <td><span class="status-badge ${m.active ? 'status-active' : 'status-inactive'}">${m.active ? 'Active' : 'Inactive'}</span></td>
            <td>${m.createdAt ? new Date(m.createdAt).toLocaleDateString() : ''}</td>
            <td>
                <button class="btn btn-outline btn-small" onclick="editManager(${m.id})">Edit</button>
                <button class="btn btn-outline btn-small" onclick="toggleManagerStatus(${m.id})">${m.active ? 'Deactivate' : 'Activate'}</button>
                <button class="btn btn-danger btn-small" onclick="confirmDeleteManager(${m.id}, '${m.username}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openManagerModal(manager = null) {
    const modal = document.getElementById('managerModal');
    const title = document.getElementById('modalTitle');
    const form = document.getElementById('managerForm');
    form.reset();
    document.getElementById('managerId').value = '';
    document.getElementById('passwordOptional').textContent = '*';

    if (manager) {
        title.textContent = 'Edit Manager';
        document.getElementById('managerId').value = manager.id;
        document.getElementById('managerUsername').value = manager.username;
        document.getElementById('managerFullName').value = manager.fullName;
        document.getElementById('managerEmail').value = manager.email;
        document.getElementById('managerActive').checked = manager.active;
        document.getElementById('managerPassword').required = false;
        document.getElementById('passwordOptional').textContent = '(leave blank to keep unchanged)';
    } else {
        title.textContent = 'Add New Manager';
        document.getElementById('managerPassword').required = true;
        document.getElementById('passwordOptional').textContent = '*';
    }
    modal.classList.add('active');
}

async function editManager(id) {
    try {
        const manager = await apiRequest(`/api/admin/managers/${id}`);
        openManagerModal(manager);
    } catch (error) {
        showToast('Failed to load manager details: ' + error.message, 'error');
    }
}

async function saveManager(e) {
    e.preventDefault();
    const id = document.getElementById('managerId').value;
    const data = {
        username: document.getElementById('managerUsername').value,
        fullName: document.getElementById('managerFullName').value,
        email: document.getElementById('managerEmail').value,
        active: document.getElementById('managerActive').checked
    };
    const password = document.getElementById('managerPassword').value;
    if (password) data.password = password;

    const url = id ? `/api/admin/managers/${id}` : '/api/admin/managers';
    const method = id ? 'PUT' : 'POST';

    try {
        await apiRequest(url, method, data);
        showToast(`Manager ${id ? 'updated' : 'created'} successfully`);
        closeModal();
        loadManagers();
    } catch (error) {
        showToast(error.message || 'Operation failed', 'error');
    }
}

async function toggleManagerStatus(id) {
    try {
        await apiRequest(`/api/admin/managers/${id}/toggle-status`, 'PATCH');
        showToast('Manager status updated');
        loadManagers();
    } catch (error) {
        showToast('Failed to toggle status: ' + error.message, 'error');
    }
}

function confirmDeleteManager(id, username) {
    deleteCallback = () => deleteManager(id);
    document.getElementById('confirmModal').classList.add('active');
}

async function deleteManager(id) {
    try {
        await apiRequest(`/api/admin/managers/${id}`, 'DELETE');
        showToast('Manager deleted successfully');
        loadManagers();
    } catch (error) {
        console.error('Delete error:', error);
        showToast('Delete failed: ' + error.message, 'error');
    }
}

// All Users
async function loadAllUsers() {
    try {
        const users = await apiRequest('/api/admin/users');
        renderAllUsersTable(users);
    } catch (error) {
        showToast('Failed to load users: ' + error.message, 'error');
    }
}

function renderAllUsersTable(users) {
    const tbody = document.getElementById('allUsersTableBody');
    tbody.innerHTML = users.map(u => `
        <tr>
            <td>${u.id}</td>
            <td>${u.username}</td>
            <td>${u.fullName}</td>
            <td>${u.accountNumber || 'N/A'}</td>
            <td>$${u.balance ? u.balance.toFixed(2) : '0.00'}</td>
            <td><span class="status-badge ${u.active ? 'status-active' : 'status-inactive'}">${u.active ? 'Active' : 'Inactive'}</span></td>
            <td>${u.createdAt ? new Date(u.createdAt).toLocaleDateString() : ''}</td>
            <td>
                <button class="btn btn-danger btn-small" onclick="confirmDeleteUser(${u.id}, '${u.username}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function confirmDeleteUser(id, username) {
    deleteCallback = () => deleteUser(id);
    document.getElementById('confirmModal').classList.add('active');
}

async function deleteUser(id) {
    try {
        await apiRequest(`/api/admin/users/${id}`, 'DELETE');
        showToast('User deleted successfully');
        loadAllUsers();
    } catch (error) {
        console.error('Delete error:', error);
        showToast('Delete failed: ' + error.message, 'error');
    }
}

// Statistics
async function loadStatistics() {
    try {
        const stats = await apiRequest('/api/admin/statistics');
        renderStats(stats);
    } catch (error) {
        console.error('Stats error:', error);
        showToast('Failed to load statistics', 'error');
        renderStats({ totalUsers: 0, totalManagers: 0, activeUsers: 0, totalDeposits: 0 });
    }
}

function renderStats(stats) {
    const container = document.getElementById('statsContainer');
    container.innerHTML = `
        <div class="stat-card">
            <div class="stat-value">${stats.totalUsers || 0}</div>
            <div class="stat-label">Total Users</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">${stats.totalManagers || 0}</div>
            <div class="stat-label">Managers</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">${stats.activeUsers || 0}</div>
            <div class="stat-label">Active Users</div>
        </div>
        <div class="stat-card">
            <div class="stat-value">$${stats.totalDeposits ? stats.totalDeposits.toFixed(2) : '0.00'}</div>
            <div class="stat-label">Total Deposits</div>
        </div>
    `;
}

// Profile & Password
async function loadAdminInfo() {
    try {
        const userStr = localStorage.getItem('user');
        if (!userStr) return;
        const user = JSON.parse(userStr);
    } catch (e) {}
}

async function changePassword(e) {
    e.preventDefault();
    const current = document.getElementById('currentPassword').value;
    const newPass = document.getElementById('newPassword').value;
    const confirm = document.getElementById('confirmPassword').value;
    if (newPass !== confirm) {
        showToast('New passwords do not match', 'error');
        return;
    }
    try {
        await apiRequest('/api/user/change-password', 'POST', {
            currentPassword: current,
            newPassword: newPass
        });
        showToast('Password updated successfully');
        e.target.reset();
    } catch (error) {
        showToast(error.message || 'Failed to update password', 'error');
    }
}

function closeModal() {
    document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login.html';
}

// Make functions global for onclick handlers
window.editManager = editManager;
window.toggleManagerStatus = toggleManagerStatus;
window.confirmDeleteManager = confirmDeleteManager;
window.confirmDeleteUser = confirmDeleteUser;