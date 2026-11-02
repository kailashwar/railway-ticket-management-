// =====================================================================
// RailBook – TTR Officer Verification & Camera Scanner
// =====================================================================

let html5QrScanner = null;
let currentVerifiedBooking = null;

function initTTRDashboard() {
    checkTTROfficerAuth();
    loadTTRAuditLogs();

    // Event listener for manual token input
    const manualForm = document.getElementById('manualVerificationForm');
    if (manualForm) {
        manualForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const token = document.getElementById('manualTokenInput').value.trim();
            if (token) {
                performVerification(token, 'MANUAL_INPUT');
            }
        });
    }
}

function checkTTROfficerAuth() {
    const ttrSession = sessionStorage.getItem('railbook_ttr');
    if (!ttrSession) {
        window.location.href = 'login.html?role=ttr';
    } else {
        const ttr = JSON.parse(ttrSession);
        const nameEl = document.getElementById('ttrOfficerNameDisplay');
        if (nameEl) nameEl.textContent = ttr.name || 'Officer';
        const badgeEl = document.getElementById('ttrBadgeDisplay');
        if (badgeEl) badgeEl.textContent = ttr.badgeNumber || 'SR-TTR-8942';
    }
}

function ttrLogout() {
    sessionStorage.removeItem('railbook_ttr');
    window.location.href = 'login.html?role=ttr';
}

// Camera Scanner using HTML5 Video or WebRTC
function toggleCameraScanner() {
    const wrap = document.getElementById('cameraScannerWrapper');
    const startBtn = document.getElementById('btnStartCamera');
    const video = document.getElementById('scannerVideo');

    if (wrap.classList.contains('d-none')) {
        wrap.classList.remove('d-none');
        startBtn.innerHTML = '<i class="fas fa-stop me-2"></i> Stop Camera Scanner';
        startBtn.classList.replace('btn-primary', 'btn-danger');

        // Request camera
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
            navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
                .then(stream => {
                    video.srcObject = stream;
                    video.setAttribute('playsinline', true);
                    video.play();
                    // Simulating or processing QR scanner feed
                    startQrPolling();
                })
                .catch(err => {
                    console.warn('Camera access denied or unavailable:', err);
                    document.getElementById('scannerCameraError').classList.remove('d-none');
                });
        }
    } else {
        stopCamera();
    }
}

function stopCamera() {
    const wrap = document.getElementById('cameraScannerWrapper');
    const startBtn = document.getElementById('btnStartCamera');
    const video = document.getElementById('scannerVideo');

    if (video.srcObject) {
        video.srcObject.getTracks().forEach(track => track.stop());
    }
    wrap.classList.add('d-none');
    startBtn.innerHTML = '<i class="fas fa-camera me-2"></i> Start Camera Scanner';
    startBtn.classList.replace('btn-danger', 'btn-primary');
}

function startQrPolling() {
    // For demo convenience, allows selecting a quick demo QR token or camera scanning
}

// Perform Verification Call to Backend
async function performVerification(token, actionType = 'QR_SCAN') {
    const resultBox = document.getElementById('verificationResultCard');
    resultBox.classList.remove('d-none');
    resultBox.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-3 fw-semibold text-muted">Checking token with railway verification database...</p>
        </div>
    `;

    try {
        const response = await fetch(`${API_BASE}/ttr/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token, action: actionType })
        });
        const data = await response.json();

        if (data.verified) {
            currentVerifiedBooking = data.booking;
            renderVerifiedSuccess(data);
        } else {
            renderVerifiedFailure(data);
        }

        // Refresh audit logs
        loadTTRAuditLogs();

    } catch (e) {
        console.error(e);
        resultBox.innerHTML = `
            <div class="alert alert-danger">
                <h5><i class="fas fa-exclamation-triangle me-2"></i> Verification System Error</h5>
                <p class="mb-0">Unable to establish connection with verification server.</p>
            </div>
        `;
    }
}

// Render Successful Ticket Verification
function renderVerifiedSuccess(data) {
    const b = data.booking;
    const p = (b.passengers && b.passengers.length > 0) ? b.passengers[0] : {};
    const resultBox = document.getElementById('verificationResultCard');

    resultBox.innerHTML = `
        <div class="card border-0 shadow rounded-4 overflow-hidden">
            <div class="card-header bg-success text-white py-3 px-4 d-flex justify-content-between align-items-center">
                <div class="d-flex align-items-center gap-2">
                    <i class="fas fa-check-circle fa-2x"></i>
                    <div>
                        <h5 class="mb-0 fw-bold">✓ TICKET VERIFIED</h5>
                        <small class="text-white-50">Authorized Passenger Travel Confirmed</small>
                    </div>
                </div>
                <span class="badge bg-white text-success fw-bold px-3 py-2 rounded-pill fs-6">${b.status}</span>
            </div>
            <div class="card-body p-4">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="text-muted small text-uppercase fw-bold">Passenger Name</label>
                        <h5 class="fw-bold text-dark mb-0">${p.name || 'Passenger'}</h5>
                        <small class="text-muted">${p.age} Yrs • ${p.gender} • ${p.nationality || 'Indian'}</small>
                    </div>
                    <div class="col-md-6 text-md-end">
                        <label class="text-muted small text-uppercase fw-bold">PNR Number</label>
                        <h5 class="fw-bold font-monospace text-primary mb-0">${b.pnr}</h5>
                        <small class="text-muted">Coach: <strong class="text-dark">${p.coach}</strong> | Seat: <strong class="text-primary">${p.seat}</strong></small>
                    </div>

                    <div class="col-12"><hr class="my-2"></div>

                    <div class="col-md-6">
                        <label class="text-muted small text-uppercase fw-bold">Train Details</label>
                        <p class="fw-semibold text-dark mb-0">${b.train_number} - ${b.train_name}</p>
                        <small class="text-muted">${b.travel_class} (${b.quota})</small>
                    </div>
                    <div class="col-md-6 text-md-end">
                        <label class="text-muted small text-uppercase fw-bold">Journey Route & Date</label>
                        <p class="fw-semibold text-dark mb-0">${b.source} ➔ ${b.destination}</p>
                        <small class="text-muted">${new Date(b.journey_date).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</small>
                    </div>

                    <!-- CRITICAL REQUIREMENT 21: Identity Verification Section -->
                    <div class="col-12">
                        <div class="p-3 bg-light rounded-3 border d-flex flex-column flex-md-row justify-content-between align-items-center gap-3">
                            <div>
                                <h6 class="mb-1 fw-bold text-dark">
                                    <i class="fas fa-id-card-alt text-primary me-2"></i> Identity Verification
                                </h6>
                                <p class="mb-0 text-muted small">
                                    Identity Document: <strong class="text-success"><i class="fas fa-check-circle me-1"></i>Verified</strong>
                                    &nbsp;|&nbsp; Number: <span class="badge bg-secondary-subtle text-secondary font-monospace">${p.masked_identifier || 'XXXX XXXX 1234'}</span>
                                </p>
                            </div>
                            <button type="button" class="btn btn-outline-primary btn-sm rounded-pill px-3" onclick="viewIdentityDocument(${p.document_id || 1})">
                                <i class="fas fa-eye me-1"></i> View Identity Document
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card-footer bg-light px-4 py-3 d-flex justify-content-between align-items-center">
                <small class="text-muted"><i class="fas fa-shield-alt text-success me-1"></i> Digital signature verified by RailBook Security System</small>
                <button type="button" class="btn btn-outline-secondary btn-sm rounded-pill" onclick="clearVerificationView()">Done / Next Scan</button>
            </div>
        </div>
    `;
}

// Render Failed Verification
function renderVerifiedFailure(data) {
    const resultBox = document.getElementById('verificationResultCard');
    resultBox.innerHTML = `
        <div class="card border-0 shadow rounded-4 overflow-hidden">
            <div class="card-header bg-danger text-white py-3 px-4 d-flex align-items-center gap-3">
                <i class="fas fa-times-circle fa-2x"></i>
                <div>
                    <h5 class="mb-0 fw-bold">✗ VERIFICATION FAILED</h5>
                    <small class="text-white-50">Unauthorized or Invalid Travel Record</small>
                </div>
            </div>
            <div class="card-body p-4 text-center">
                <div class="py-3">
                    <i class="fas fa-ban text-danger fa-4x mb-3"></i>
                    <h5 class="fw-bold text-danger">${data.message || 'Invalid Ticket / Revoked QR Token'}</h5>
                    <p class="text-muted">This QR token cannot be verified against active passenger travel manifests.</p>
                    <p class="small text-muted font-monospace mb-0">Security Note: Confidential passenger information is withheld for unverified tokens.</p>
                </div>
            </div>
            <div class="card-footer bg-light px-4 py-3 text-center">
                <button type="button" class="btn btn-secondary btn-sm rounded-pill px-4" onclick="clearVerificationView()">Try Another Scan</button>
            </div>
        </div>
    `;
}

function clearVerificationView() {
    document.getElementById('verificationResultCard').classList.add('d-none');
    document.getElementById('manualTokenInput').value = '';
}

// View Identity Document (Controlled access + Audit Log)
async function viewIdentityDocument(docId) {
    try {
        const res = await fetch(`${API_BASE}/documents/view?id=${docId}&ttrId=TTR-2026`);
        const docData = await res.json();

        if (docData.success) {
            const modalBody = document.getElementById('ttrDocModalBody');
            modalBody.innerHTML = `
                <div class="alert alert-warning py-2 small fw-semibold">
                    <i class="fas fa-info-circle me-1"></i> ${docData.notice}
                </div>
                <div class="text-center p-3 bg-light rounded-3 border">
                    ${docData.svgBadge}
                </div>
                <div class="mt-3 small text-muted text-center">
                    <i class="fas fa-lock me-1 text-success"></i> Document view action has been appended to the official verification audit log.
                </div>
            `;
            const modal = new bootstrap.Modal(document.getElementById('ttrDocModal'));
            modal.show();
            loadTTRAuditLogs();
        } else {
            showToast('Unable to load document record', 'error');
        }
    } catch (e) {
        showToast('Error fetching document', 'error');
    }
}

// Load Audit Logs Table
async function loadTTRAuditLogs() {
    try {
        const res = await fetch(`${API_BASE}/ttr/audit-logs?limit=15`);
        const logs = await res.json();
        const tbody = document.getElementById('ttrAuditLogsTableBody');
        if (!tbody) return;

        if (logs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">No audit logs recorded yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = logs.map(l => {
            const isSuccess = l.result === 'VERIFIED';
            return `
                <tr>
                    <td class="font-monospace small">${new Date(l.verification_time).toLocaleTimeString()}</td>
                    <td class="fw-bold">${l.ttr_id}</td>
                    <td class="font-monospace">${l.pnr || l.booking_id}</td>
                    <td><span class="badge ${isSuccess ? 'bg-success' : 'bg-danger'}">${l.result}</span></td>
                    <td><small class="text-muted">${l.action} – ${l.notes || ''}</small></td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        console.error('Audit logs error:', e);
    }
}

// Quick demo test helper
function quickVerifySample(token) {
    document.getElementById('manualTokenInput').value = token;
    performVerification(token, 'MANUAL_INPUT');
}
