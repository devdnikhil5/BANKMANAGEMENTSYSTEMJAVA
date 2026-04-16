// Configuration & State
const API_BASE = '';

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
    loadUserInfo();
    loadOverviewData();
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
        if (!user.roles.includes('ROLE_USER')) {
            alert('Access denied. User only.');
            window.location.href = '/login.html';
            return;
        }
        document.getElementById('userUsername').textContent = user.username;
        document.getElementById('displayFullName').textContent = user.username;
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
            if (section === 'overview') loadOverviewData();
            else if (section === 'history') loadTransactionHistory();
            else if (section === 'pending') loadPendingTransactions();
            else if (section === 'profile') populateProfileForm();
        });
    });

    // Quick action buttons on overview
    document.querySelectorAll('.quick-action').forEach(btn => {
        btn.addEventListener('click', () => {
            const action = btn.dataset.action;
            showSection('transactions');
            document.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            document.querySelector(`[data-tab="${action}"]`).classList.add('active');
            document.getElementById(`${action}-tab`).classList.add('active');
        });
    });

    // Transaction tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.dataset.tab;
            document.querySelectorAll('.tab-btn').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById(`${tab}-tab`).classList.add('active');
        });
    });

    // Transaction forms
    document.getElementById('depositForm').addEventListener('submit', (e) => makeTransaction(e, 'deposit'));
    document.getElementById('withdrawForm').addEventListener('submit', (e) => makeTransaction(e, 'withdraw'));
    document.getElementById('transferForm').addEventListener('submit', (e) => makeTransaction(e, 'transfer'));

    // Refresh buttons
    document.getElementById('refreshHistoryBtn').addEventListener('click', loadTransactionHistory);
    document.getElementById('refreshPendingBtn').addEventListener('click', loadPendingTransactions);

    // Profile forms
    document.getElementById('profileForm').addEventListener('submit', updateProfile);
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
        const error = await response.text();
        throw new Error(error || 'Request failed');
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

// User Info & Overview 
async function loadUserInfo() {
    try {
        const profile = await apiRequest('/api/user/profile');
        document.getElementById('accountNumber').textContent = profile.accountNumber || 'N/A';
        document.getElementById('accountBalance').textContent = `$${profile.balance ? profile.balance.toFixed(2) : '0.00'}`;
        const userStr = localStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            user.fullName = profile.fullName;
            user.email = profile.email;
            localStorage.setItem('user', JSON.stringify(user));
        }
        populateProfileFields(profile);
    } catch (error) {
        showToast('Failed to load account info', 'error');
    }
}

async function loadOverviewData() {
    await loadUserInfo();
    await loadRecentTransactions();
}

async function loadRecentTransactions() {
    try {
        const transactions = await apiRequest('/api/user/transactions');
        const recent = transactions.slice(0, 5);
        renderRecentTransactions(recent);
    } catch (error) {
        console.error('Failed to load recent transactions', error);
    }
}

function renderRecentTransactions(transactions) {
    const tbody = document.getElementById('recentTransactionsBody');
    if (transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center">No transactions yet</td></tr>';
        return;
    }
    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td>${new Date(t.createdAt).toLocaleDateString()}</td>
            <td>${t.transactionType}</td>
            <td>$${t.amount.toFixed(2)}</td>
            <td><span class="status-badge status-${t.status.toLowerCase()}">${t.status}</span></td>
        </tr>
    `).join('');
}

// Transactions
async function makeTransaction(e, type) {
    e.preventDefault();
    let data = {};
    const form = e.target;

    if (type === 'deposit') {
        data.amount = parseFloat(document.getElementById('depositAmount').value);
        data.description = document.getElementById('depositDescription').value;
    } else if (type === 'withdraw') {
        data.amount = parseFloat(document.getElementById('withdrawAmount').value);
        data.description = document.getElementById('withdrawDescription').value;
    } else if (type === 'transfer') {
        data.amount = parseFloat(document.getElementById('transferAmount').value);
        data.recipientAccountNumber = document.getElementById('recipientAccount').value;
        data.description = document.getElementById('transferDescription').value;
    }

    if (data.amount <= 0) {
        showToast('Amount must be positive', 'error');
        return;
    }

    const endpointMap = {
        deposit: '/api/user/transactions/deposit',
        withdraw: '/api/user/transactions/withdraw',
        transfer: '/api/user/transactions/transfer'
    };

    try {
        const result = await apiRequest(endpointMap[type], 'POST', data);
        showToast(`${type.charAt(0).toUpperCase() + type.slice(1)} request submitted for approval`, 'success');
        form.reset();
        setTimeout(() => {
            loadOverviewData();
            loadPendingTransactions();
        }, 500);
    } catch (error) {
        showToast(error.message || 'Transaction failed', 'error');
    }
}

async function loadTransactionHistory() {
    try {
        const transactions = await apiRequest('/api/user/transactions');
        renderHistoryTable(transactions);
    } catch (error) {
        showToast('Failed to load transaction history', 'error');
    }
}

function renderHistoryTable(transactions) {
    const tbody = document.getElementById('historyTableBody');
    if (transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">No transactions found</td></tr>';
        return;
    }
    tbody.innerHTML = transactions.map(t => {
        let details = '';
        if (t.fromAccountNumber) details += `From: ${t.fromAccountNumber}`;
        if (t.toAccountNumber) details += (details ? ' → ' : '') + `To: ${t.toAccountNumber}`;
        return `
        <tr>
            <td>${t.id}</td>
            <td>${t.transactionType}</td>
            <td>$${t.amount.toFixed(2)}</td>
            <td>${details || '-'}</td>
            <td><span class="status-badge status-${t.status.toLowerCase()}">${t.status}</span></td>
            <td>${t.approvedAt ? new Date(t.approvedAt).toLocaleString() : new Date(t.createdAt).toLocaleString()}</td>
            <td>${t.managerComment || ''}</td>
        </tr>
    `}).join('');
}

async function loadPendingTransactions() {
    try {
        const pending = await apiRequest('/api/user/transactions/pending');
        renderPendingTable(pending);
    } catch (error) {
        showToast('Failed to load pending transactions', 'error');
    }
}

function renderPendingTable(transactions) {
    const tbody = document.getElementById('pendingUserTableBody');
    if (transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">No pending transactions</td></tr>';
        return;
    }
    tbody.innerHTML = transactions.map(t => {
        let details = '';
        if (t.fromAccountNumber) details += `From: ${t.fromAccountNumber}`;
        if (t.toAccountNumber) details += (details ? ' → ' : '') + `To: ${t.toAccountNumber}`;
        return `
        <tr>
            <td>${t.id}</td>
            <td>${t.transactionType}</td>
            <td>$${t.amount.toFixed(2)}</td>
            <td>${details || t.description || '-'}</td>
            <td>${new Date(t.createdAt).toLocaleString()}</td>
        </tr>
    `}).join('');
}

// Profile
function populateProfileForm() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
        const user = JSON.parse(userStr);
        document.getElementById('profileUsername').value = user.username || '';
        document.getElementById('profileFullName').value = user.fullName || '';
        document.getElementById('profileEmail').value = user.email || '';
    }
    apiRequest('/api/user/profile').then(profile => {
        populateProfileFields(profile);
    }).catch(() => {});
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
        
        // Update localStorage
        const userStr = localStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            user.fullName = updatedUser.fullName;
            user.email = updatedUser.email;
            localStorage.setItem('user', JSON.stringify(user));
            document.getElementById('displayFullName').textContent = fullName;
        }
    } catch (error) {
        console.error('Profile update error:', error);
        let errorMsg = 'Failed to update profile';
        try {
            const err = JSON.parse(error.message);
            errorMsg = err.message || errorMsg;
        } catch (e) {}
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
function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login.html';
}