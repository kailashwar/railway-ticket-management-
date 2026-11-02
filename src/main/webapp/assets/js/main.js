// =====================================================================
// RailBook – Main Application Script & Shared Utilities
// =====================================================================

const API_BASE = '/api';

// Format currency
function formatINR(val) {
    return '₹' + parseFloat(val || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
}

// Get user session from localStorage
function getCurrentUser() {
    try {
        const u = localStorage.getItem('railbook_user');
        return u ? JSON.parse(u) : null;
    } catch (e) {
        return null;
    }
}

// Set user session
function setCurrentUser(user) {
    localStorage.setItem('railbook_user', JSON.stringify(user));
}

// Clear user session
function clearUser() {
    localStorage.removeItem('railbook_user');
}

// Render dynamic navbar state
function renderNavbarAuth() {
    const authContainer = document.getElementById('navbarAuthButtons');
    if (!authContainer) return;

    const user = getCurrentUser();
    if (user && user.role === 'PASSENGER') {
        authContainer.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-outline-primary btn-sm dropdown-toggle rounded-pill px-3 py-1 font-weight-bold" type="button" data-bs-toggle="dropdown">
                    <i class="fas fa-user-circle me-1 text-primary"></i> ${user.name}
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0">
                    <li><a class="dropdown-item" href="my-bookings.html"><i class="fas fa-ticket-alt me-2 text-primary"></i>My Bookings</a></li>
                    <li><a class="dropdown-item" href="pnr-status.html"><i class="fas fa-search me-2 text-info"></i>PNR Status</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item text-danger" href="#" onclick="logoutPassenger(event)"><i class="fas fa-sign-out-alt me-2"></i>Logout</a></li>
                </ul>
            </div>
        `;
    } else {
        authContainer.innerHTML = `
            <a href="login.html" class="btn btn-outline-primary btn-sm px-3 py-1 rounded-pill fw-semibold me-2">
                <i class="fas fa-sign-in-alt me-1"></i> Login
            </a>
            <a href="register.html" class="btn btn-primary btn-sm px-3 py-1 rounded-pill fw-semibold">
                <i class="fas fa-user-plus me-1"></i> Register
            </a>
        `;
    }
}

function logoutPassenger(e) {
    if (e) e.preventDefault();
    clearUser();
    window.location.href = 'login.html';
}

// Toast Alert
function showToast(message, type = 'success') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'position-fixed bottom-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : (type === 'warning' ? 'warning' : 'success')} border-0 show`;
    toastEl.setAttribute('role', 'alert');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body fw-semibold">
                <i class="fas ${type === 'error' ? 'fa-exclamation-circle' : 'fa-check-circle'} me-2"></i>
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    container.appendChild(toastEl);
    setTimeout(() => {
        toastEl.remove();
    }, 4500);
}

document.addEventListener('DOMContentLoaded', () => {
    renderNavbarAuth();
});
