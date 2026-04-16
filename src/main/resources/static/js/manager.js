// Configuration & State
const API_BASE = '';
let currentUserId = null;
let deleteCallback = null;

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
    loadManagerInfo();
    loadUsers();
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
        if (!user.roles.includes('ROLE_MANAGER')) {
            alert('Access denied. Manager only.');
            window.location.href = '/login.html';
            return;
        }
        document.getElementById('managerUsername').textContent = user.username;
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
            if (section === 'users') loadUsers();
            else if (section === 'pending') loadPendingTransactions();
            else if (section === 'transactions') loadAllTransactions();
            else if (section === 'profile') populateProfileForm();
        });
    });

    // Add User button
    document.getElementById('addUserBtn').addEventListener('click', () => openUserModal());

    // User form submit
    document.getElementById('userForm').addEventListener('submit', saveUser);

    // Modal close buttons
    document.querySelectorAll('.close-modal').forEach(el => el.addEventListener('click', closeModal));
    document.getElementById('cancelModalBtn').addEventListener('click', closeModal);
    document.querySelectorAll('.close-modal-btn').forEach(el => el.addEventListener('click', closeModal));

    // Delete confirmation
    document.getElementById('cancelDeleteBtn').addEventListener('click', () => {
        document.getElementById('confirmModal').classList.remove('active');
    });
    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (deleteCallback) deleteCallback();
        document.getElementById('confirmModal').classList.remove('active');
    });

    // Refresh buttons
    document.getElementById('refreshPendingBtn').addEventListener('click', loadPendingTransactions);
    document.getElementById('refreshTransactionsBtn').addEventListener('click', loadAllTransactions);

    // Approval form
    document.getElementById('approvalForm').addEventListener('submit', processApproval);

    // Profile form
    document.getElementById('profileForm').addEventListener('submit', updateProfile);

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

// Users CRUD
async function loadUsers() {
    try {
        const users = await apiRequest('/api/manager/users');
        renderUsersTable(users);
    } catch (error) {
        showToast('Failed to load users: ' + error.message, 'error');
    }
}

function renderUsersTable(users) {
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = users.map(u => `
        <tr>
            <td>${u.id}</td>
            <td>${u.username}</td>
            <td>${u.fullName}</td>
            <td>${u.email}</td>
            <td>${u.accountNumber || 'N/A'}</td>
            <td>$${u.balance ? u.balance.toFixed(2) : '0.00'}</td>
            <td><span class="status-badge ${u.active ? 'status-active' : 'status-inactive'}">${u.active ? 'Active' : 'Inactive'}</span></td>
            <td>
                <button class="btn btn-outline btn-small" onclick="editUser(${u.id})">Edit</button>
                <button class="btn btn-outline btn-small" onclick="toggleUserStatus(${u.id})">${u.active ? 'Deactivate' : 'Activate'}</button>
                <button class="btn btn-danger btn-small" onclick="confirmDeleteUser(${u.id}, '${u.username}')">Delete</button>
            </td>
        </tr>
    `).join('');
}

function openUserModal(user = null) {
    const modal = document.getElementById('userModal');
    const title = document.getElementById('modalTitle');
    const form = document.getElementById('userForm');
    form.reset();
    document.getElementById('userId').value = '';
    document.getElementById('passwordOptional').textContent = '*';

    if (user) {
        title.textContent = 'Edit User';
        document.getElementById('userId').value = user.id;
        document.getElementById('userUsername').value = user.username;
        document.getElementById('userFullName').value = user.fullName;
        document.getElementById('userEmail').value = user.email;
        document.getElementById('userActive').checked = user.active;
        document.getElementById('userPassword').required = false;
        document.getElementById('passwordOptional').textContent = '(leave blank to keep unchanged)';
    } else {
        title.textContent = 'Add New User';
        document.getElementById('userPassword').required = true;
        document.getElementById('passwordOptional').textContent = '*';
    }
    modal.classList.add('active');
}

async function editUser(id) {
    try {
        const user = await apiRequest(`/api/manager/users/${id}`);
        openUserModal(user);
    } catch (error) {
        showToast('Failed to load user details: ' + error.message, 'error');
    }
}

async function saveUser(e) {
    e.preventDefault();
    const id = document.getElementById('userId').value;
    const data = {
        username: document.getElementById('userUsername').value,
        fullName: document.getElementById('userFullName').value,
        email: document.getElementById('userEmail').value,
        active: document.getElementById('userActive').checked
    };
    const password = document.getElementById('userPassword').value;
    if (password) data.password = password;

    const url = id ? `/api/manager/users/${id}` : '/api/manager/users';
    const method = id ? 'PUT' : 'POST';

    try {
        await apiRequest(url, method, data);
        showToast(`User ${id ? 'updated' : 'created'} successfully`);
        closeModal();
        loadUsers();
    } catch (error) {
        showToast(error.message || 'Operation failed', 'error');
    }
}

async function toggleUserStatus(id) {
    try {
        await apiRequest(`/api/manager/users/${id}/toggle-status`, 'PATCH');
        showToast('User status updated');
        loadUsers();
    } catch (error) {
        showToast('Failed to toggle status: ' + error.message, 'error');
    }
}

function confirmDeleteUser(id, username) {
    deleteCallback = () => deleteUser(id);
    document.getElementById('confirmMessage').textContent = `Are you sure you want to delete user "${username}"? This action cannot be undone.`;
    document.getElementById('confirmModal').classList.add('active');
}

async function deleteUser(id) {
    try {
        await apiRequest(`/api/manager/users/${id}`, 'DELETE');
        showToast('User deleted successfully');
        loadUsers();
        closeModal();
    } catch (error) {
        console.error('Delete error:', error);
        showToast('Delete failed: ' + error.message, 'error');
    }
}

// Transactions (Pending & All)
async function loadPendingTransactions() {
    try {
        const transactions = await apiRequest('/api/manager/transactions/pending');
        renderPendingTable(transactions);
    } catch (error) {
        showToast('Failed to load pending transactions: ' + error.message, 'error');
    }
}

function renderPendingTable(transactions) {
    const tbody = document.getElementById('pendingTableBody');
    if (transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">No pending transactions</td></tr>';
        return;
    }
    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td>${t.id}</td>
            <td>${t.transactionType}</td>
            <td>$${t.amount.toFixed(2)}</td>
            <td>${t.fromAccountNumber || '-'}</td>
            <td>${t.toAccountNumber || '-'}</td>
            <td>${t.description || ''}</td>
            <td>${new Date(t.createdAt).toLocaleString()}</td>
            <td>
                <button class="btn btn-success btn-small" onclick="openApprovalModal(${t.id}, 'approve')">Approve</button>
                <button class="btn btn-danger btn-small" onclick="openApprovalModal(${t.id}, 'reject')">Reject</button>
            </td>
        </tr>
    `).join('');
}

async function loadAllTransactions() {
    try {
        const transactions = await apiRequest('/api/manager/transactions');
        renderAllTransactionsTable(transactions);
    } catch (error) {
        showToast('Failed to load transactions: ' + error.message, 'error');
    }
}

function renderAllTransactionsTable(transactions) {
    const tbody = document.getElementById('transactionsTableBody');
    if (transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">No transactions found</td></tr>';
        return;
    }
    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td>${t.id}</td>
            <td>${t.transactionType}</td>
            <td>$${t.amount.toFixed(2)}</td>
            <td>${t.fromAccountNumber || '-'}</td>
            <td>${t.toAccountNumber || '-'}</td>
            <td><span class="status-badge status-${t.status.toLowerCase()}">${t.status}</span></td>
            <td>${t.approvedBy || '-'}</td>
            <td>${t.approvedAt ? new Date(t.approvedAt).toLocaleString() : new Date(t.createdAt).toLocaleString()}</td>
        </tr>
    `).join('');
}

function openApprovalModal(transactionId, action) {
    const modal = document.getElementById('approvalModal');
    document.getElementById('approvalTransactionId').value = transactionId;
    document.getElementById('approvalAction').value = action;
    document.getElementById('approvalTitle').textContent = action === 'approve' ? 'Approve Transaction' : 'Reject Transaction';
    document.getElementById('approvalDetails').textContent = `Transaction ID: ${transactionId}`;
    document.getElementById('managerComment').value = '';
    modal.classList.add('active');
}

async function processApproval(e) {
    e.preventDefault();
    const transactionId = document.getElementById('approvalTransactionId').value;
    const action = document.getElementById('approvalAction').value;
    const comment = document.getElementById('managerComment').value;

    const url = `/api/manager/transactions/${transactionId}/${action}`;
    const body = { managerComment: comment };

    try {
        await apiRequest(url, 'PATCH', body);
        showToast(`Transaction ${action}d successfully`);
        closeModal();
        loadPendingTransactions();
        loadAllTransactions();
    } catch (error) {
        showToast(error.message || `Failed to ${action} transaction`, 'error');
    }
}

// Profile
async function loadManagerInfo() {
    try {
        const profile = await apiRequest('/api/user/profile');
        populateProfileFields(profile);
    } catch (error) {
        console.error('Failed to load profile:', error);
        const userStr = localStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            document.getElementById('profileUsername').value = user.username || '';
            document.getElementById('profileFullName').value = user.fullName || '';
            document.getElementById('profileEmail').value = user.email || '';
        }
    }
}

function populateProfileForm() {
    loadManagerInfo();
}

function populateProfileFields(profile) {
    document.getElementById('profileUsername').value = profile.username || '';
    document.getElementById('profileFullName').value = profile.fullName || '';
    document.getElementById('profileEmail').value = profile.email || '';
}

async function updateProfile(e) {
    e.preventDefault();
    
    const fullNameInput = document.getElementById('profileFullName');
    const emailInput = document.getElementById('profileEmail');
    
    const fullName = fullNameInput.value.trim();
    const email = emailInput.value.trim();
    
    if (!fullName) {
        showToast('Full name cannot be empty', 'error');
        return;
    }
    if (!email) {
        showToast('Email cannot be empty', 'error');
        return;
    }
    if (!email.includes('@')) {
        showToast('Please enter a valid email address', 'error');
        return;
    }
    
    const data = { fullName, email };
    
    try {
        const updatedUser = await apiRequest('/api/user/profile', 'PUT', data);
        showToast('Profile updated successfully');
        
        const userStr = localStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            user.fullName = updatedUser.fullName;
            user.email = updatedUser.email;
            localStorage.setItem('user', JSON.stringify(user));
        }
    } catch (error) {
        console.error('Profile update error:', error);
        let errorMsg = 'Failed to update profile';
        if (error.message) errorMsg = error.message;
        showToast(errorMsg, 'error');
    }
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

// Helpers
function closeModal() {
    document.querySelectorAll('.modal').forEach(m => m.classList.remove('active'));
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login.html';
}

// Make functions global for onclick handlers
window.editUser = editUser;
window.toggleUserStatus = toggleUserStatus;
window.confirmDeleteUser = confirmDeleteUser;
window.openApprovalModal = openApprovalModal;