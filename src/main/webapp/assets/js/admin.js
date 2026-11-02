// =====================================================================
// RailBook – Admin Operations & Ticket Decisions
// =====================================================================

let allBookings = [];

async function initAdminDashboard() {
    checkAdminAuth();
    await loadAdminStats();
    await loadBookingsTable();
    await loadTrainsList();

    // Attach search and filter listeners
    const searchInput = document.getElementById('searchBookingsInput');
    if (searchInput) {
        searchInput.addEventListener('input', filterBookingsTable);
    }
}

function checkAdminAuth() {
    const adminSession = sessionStorage.getItem('railbook_admin');
    if (!adminSession) {
        window.location.href = 'login.html?role=admin';
    } else {
        const admin = JSON.parse(adminSession);
        const nameEl = document.getElementById('adminNameDisplay');
        if (nameEl) nameEl.textContent = admin.name || 'System Administrator';
    }
}

function adminLogout() {
    sessionStorage.removeItem('railbook_admin');
    window.location.href = 'login.html?role=admin';
}

// 1. Load Stats
async function loadAdminStats() {
    try {
        const res = await fetch(`${API_BASE}/admin/stats`);
        const stats = await res.json();

        document.getElementById('statTotalBookings').textContent = stats.totalBookings || 0;
        document.getElementById('statConfirmed').textContent = stats.confirmedTickets || 0;
        document.getElementById('statWaitingList').textContent = stats.waitingList || 0;
        document.getElementById('statNotSelected').textContent = stats.notSelected || 0;
        document.getElementById('statTotalPassengers').textContent = stats.totalPassengers || 0;
        document.getElementById('statTodayBookings').textContent = stats.todayBookings || 0;
    } catch (e) {
        console.error('Stats error:', e);
    }
}

// 2. Load Bookings Table
async function loadBookingsTable() {
    try {
        const res = await fetch(`${API_BASE}/admin/bookings`);
        allBookings = await res.json();
        renderBookingsTable(allBookings);
    } catch (e) {
        console.error('Bookings table error:', e);
        showToast('Error loading passenger bookings', 'error');
    }
}

function renderBookingsTable(bookings) {
    const tbody = document.getElementById('adminBookingsTableBody');
    if (!tbody) return;

    if (bookings.length === 0) {
        tbody.innerHTML = `<tr><td colspan="11" class="text-center py-4 text-muted">No reservations found in database.</td></tr>`;
        return;
    }

    tbody.innerHTML = bookings.map(b => {
        const p = (b.passengers && b.passengers.length > 0) ? b.passengers[0] : {};
        const maskedAadhaar = p.masked_identifier || 'XXXX XXXX 1234';

        let badgeClass = 'badge-confirmed';
        if (b.status === 'WAITING LIST') badgeClass = 'badge-waiting';
        else if (b.status === 'NOT SELECTED') badgeClass = 'badge-not-selected';
        else if (b.status === 'CANCELLED') badgeClass = 'badge-cancelled';

        return `
            <tr>
                <td class="fw-bold font-monospace text-primary">
                    <a href="#" onclick="openBookingDetails('${b.booking_id}'); return false;">${b.pnr}</a>
                </td>
                <td>
                    <div class="fw-bold text-dark">${p.name || 'Passenger'}</div>
                    <small class="text-muted"><i class="fas fa-id-card me-1"></i>Aadhaar: <span class="badge bg-light text-dark border font-monospace">${maskedAadhaar}</span></small>
                </td>
                <td>${p.age || '-'}</td>
                <td>${p.gender || '-'}</td>
                <td>
                    <div class="fw-semibold text-truncate" style="max-width: 140px;">${b.train_name}</div>
                    <small class="text-muted font-monospace">${b.train_number}</small>
                </td>
                <td>${b.source}</td>
                <td>${b.destination}</td>
                <td class="text-nowrap">${new Date(b.journey_date).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                <td><span class="badge bg-secondary-subtle text-secondary border">${b.travel_class}</span></td>
                <td><span class="${badgeClass}">${b.status}</span></td>
                <td>
                    <div class="btn-group btn-group-sm" role="group">
                        <button type="button" class="btn btn-outline-success btn-sm" title="Confirm Ticket" onclick="makeDecision('${b.booking_id}', 'CONFIRMED')">
                            <i class="fas fa-check"></i>
                        </button>
                        <button type="button" class="btn btn-outline-warning btn-sm" title="Waiting List" onclick="makeDecision('${b.booking_id}', 'WAITING LIST')">
                            <i class="fas fa-clock"></i>
                        </button>
                        <button type="button" class="btn btn-outline-danger btn-sm" title="Not Selected" onclick="makeDecision('${b.booking_id}', 'NOT SELECTED')">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// 3. Make Ticket Decision: CONFIRM TICKET / WAITING LIST / NOT SELECTED
async function makeDecision(bookingId, decision) {
    if (!confirm(`Are you sure you want to mark booking ${bookingId} as ${decision}?`)) return;

    try {
        const res = await fetch(`${API_BASE}/admin/decide`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bookingId, decision })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Ticket status updated to ${decision}`, 'success');
            await loadAdminStats();
            await loadBookingsTable();
        } else {
            showToast(data.error || 'Failed to update ticket status', 'error');
        }
    } catch (e) {
        showToast('Error updating status', 'error');
    }
}

// Filter table
function filterBookingsTable() {
    const q = document.getElementById('searchBookingsInput').value.toLowerCase().trim();
    if (!q) {
        renderBookingsTable(allBookings);
        return;
    }
    const filtered = allBookings.filter(b => {
        const p = (b.passengers && b.passengers.length > 0) ? b.passengers[0] : {};
        return (
            b.pnr.toLowerCase().includes(q) ||
            (p.name && p.name.toLowerCase().includes(q)) ||
            b.train_number.toLowerCase().includes(q) ||
            b.train_name.toLowerCase().includes(q) ||
            b.status.toLowerCase().includes(q)
        );
    });
    renderBookingsTable(filtered);
}

// Booking Details Modal
function openBookingDetails(bookingId) {
    const b = allBookings.find(x => x.booking_id === bookingId);
    if (!b) return;

    const modalBody = document.getElementById('bookingDetailsModalBody');
    const p = (b.passengers && b.passengers.length > 0) ? b.passengers[0] : {};

    modalBody.innerHTML = `
        <div class="row g-3">
            <div class="col-md-6">
                <p class="mb-1 text-muted small text-uppercase fw-bold">PNR Number</p>
                <p class="h5 font-monospace text-primary">${b.pnr}</p>
            </div>
            <div class="col-md-6">
                <p class="mb-1 text-muted small text-uppercase fw-bold">Booking ID</p>
                <p class="h5 font-monospace">${b.booking_id}</p>
            </div>
            <div class="col-12"><hr class="my-1"></div>
            <div class="col-md-6">
                <p class="mb-1 text-muted small">Passenger Name</p>
                <p class="fw-bold">${p.name || 'N/A'}</p>
            </div>
            <div class="col-md-3">
                <p class="mb-1 text-muted small">Age / Gender</p>
                <p class="fw-semibold">${p.age} / ${p.gender}</p>
            </div>
            <div class="col-md-3">
                <p class="mb-1 text-muted small">Berth / Seat</p>
                <p class="fw-semibold">${p.seat || 'Assigned on chart'}</p>
            </div>
            <div class="col-md-6">
                <p class="mb-1 text-muted small">Train Details</p>
                <p class="fw-semibold">${b.train_number} – ${b.train_name}</p>
            </div>
            <div class="col-md-6">
                <p class="mb-1 text-muted small">Journey Date & Route</p>
                <p class="fw-semibold">${b.source} ➔ ${b.destination}</p>
            </div>
            <div class="col-md-6">
                <p class="mb-1 text-muted small">Masked Identity (Privacy Protected)</p>
                <p class="font-monospace text-primary fw-bold">${p.masked_identifier || 'XXXX XXXX 1234'}</p>
            </div>
            <div class="col-md-6">
                <p class="mb-1 text-muted small">Ticket Decision Status</p>
                <p><span class="badge ${b.status === 'CONFIRMED' ? 'badge-confirmed' : (b.status === 'WAITING LIST' ? 'badge-waiting' : 'badge-not-selected')}">${b.status}</span></p>
            </div>
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('bookingDetailsModal'));
    modal.show();
}

// 4. Train Management CRUD
async function loadTrainsList() {
    try {
        const res = await fetch(`${API_BASE}/admin/trains`);
        const trains = await res.json();
        const tbody = document.getElementById('adminTrainsTableBody');
        if (!tbody) return;

        tbody.innerHTML = trains.map(t => `
            <tr>
                <td class="font-monospace fw-bold text-primary">${t.train_number}</td>
                <td class="fw-bold">${t.train_name}</td>
                <td>${t.source} ➔ ${t.destination}</td>
                <td>${t.departure_time} - ${t.arrival_time}</td>
                <td><span class="badge bg-success-subtle text-success">${t.available_seats} / ${t.total_seats}</span></td>
                <td>₹${t.fare_sleeper} / ₹${t.fare_ac}</td>
                <td>
                    <button class="btn btn-outline-danger btn-sm" onclick="deleteTrain(${t.train_id})">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Trains list error:', e);
    }
}

async function handleAddTrainSubmit(e) {
    e.preventDefault();
    const data = {
        trainNumber: document.getElementById('newTrainNumber').value.trim(),
        trainName: document.getElementById('newTrainName').value.trim(),
        source: document.getElementById('newTrainSource').value.trim(),
        destination: document.getElementById('newTrainDestination').value.trim(),
        departureTime: document.getElementById('newTrainDeparture').value.trim(),
        arrivalTime: document.getElementById('newTrainArrival').value.trim(),
        fareAc: document.getElementById('newTrainFareAc').value,
        fareSleeper: document.getElementById('newTrainFareSleeper').value,
        totalSeats: 120,
        availableSeats: 85,
        acSeats: 40,
        sleeperSeats: 60,
        tatkalSeats: 20
    };

    try {
        const res = await fetch(`${API_BASE}/admin/trains/add`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const result = await res.json();
        if (result.success) {
            showToast('Train added successfully!', 'success');
            document.getElementById('addTrainForm').reset();
            bootstrap.Modal.getInstance(document.getElementById('addTrainModal')).hide();
            await loadTrainsList();
        } else {
            showToast(result.error || 'Failed to add train', 'error');
        }
    } catch (err) {
        showToast('Error adding train', 'error');
    }
}

async function deleteTrain(trainId) {
    if (!confirm('Are you sure you want to delete this train?')) return;
    try {
        const res = await fetch(`${API_BASE}/admin/trains/delete`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ trainId })
        });
        const result = await res.json();
        if (result.success) {
            showToast('Train deleted', 'success');
            await loadTrainsList();
        }
    } catch (e) {
        showToast('Error deleting train', 'error');
    }
}
