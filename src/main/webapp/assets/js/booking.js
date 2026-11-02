// =====================================================================
// RailBook – Tatkal-Style Booking Engine with Senior Citizen Logic
// =====================================================================

let passengerIndex = 0;
let currentTrain = null;

// Initialize booking page
async function initBookingPage() {
    const urlParams = new URLSearchParams(window.location.search);
    const trainId = urlParams.get('trainId') || '1';
    const journeyDate = urlParams.get('date') || new Date().toISOString().split('T')[0];
    const travelClass = urlParams.get('class') || 'Sleeper';
    const quota = urlParams.get('quota') || 'General';

    document.getElementById('inputJourneyDate').value = journeyDate;
    document.getElementById('selectTravelClass').value = travelClass;
    document.getElementById('selectQuota').value = quota;

    try {
        const res = await fetch(`${API_BASE}/trains/${trainId}`);
        if (!res.ok) throw new Error('Train details not found');
        currentTrain = await res.json();
        renderTrainHeader(currentTrain);
    } catch (e) {
        console.error(e);
        showToast('Error loading train details', 'error');
    }

    // Add first passenger row by default
    addPassengerRow();

    // Attach form listeners
    document.getElementById('selectTravelClass').addEventListener('change', updateFareSummary);
    document.getElementById('selectQuota').addEventListener('change', updateFareSummary);
    document.getElementById('bookingForm').addEventListener('submit', handleBookingSubmit);
}

function renderTrainHeader(t) {
    document.getElementById('displayTrainNumber').textContent = t.train_number;
    document.getElementById('displayTrainName').textContent = t.train_name;
    document.getElementById('displaySource').textContent = t.source;
    document.getElementById('displayDestination').textContent = t.destination;
    document.getElementById('displayDeparture').textContent = t.departure_time;
    document.getElementById('displayArrival').textContent = t.arrival_time;
    document.getElementById('hiddenTrainId').value = t.train_id;

    updateFareSummary();
}

// Add a passenger row dynamically
function addPassengerRow() {
    passengerIndex++;
    const idx = passengerIndex;
    const container = document.getElementById('passengersContainer');

    const card = document.createElement('div');
    card.className = 'card border-0 shadow-sm rounded-4 mb-3 passenger-row-card';
    card.id = `passengerCard_${idx}`;

    card.innerHTML = `
        <div class="card-header bg-light border-0 py-3 d-flex justify-content-between align-items-center">
            <h6 class="mb-0 fw-bold text-primary">
                <i class="fas fa-user-tag me-2"></i> Passenger ${container.children.length + 1}
            </h6>
            ${container.children.length > 0 ? `
                <button type="button" class="btn btn-outline-danger btn-sm rounded-pill px-3 py-0" onclick="removePassengerRow(${idx})">
                    <i class="fas fa-trash-alt me-1"></i> Remove
                </button>
            ` : ''}
        </div>
        <div class="card-body p-3 p-md-4">
            <div class="row g-3">
                <div class="col-md-5">
                    <label class="form-label-custom">Full Name (as per ID) *</label>
                    <input type="text" class="form-control form-control-rb passenger-name" placeholder="e.g. Ramanathan Iyer" required>
                </div>
                <div class="col-md-2">
                    <label class="form-label-custom">Age *</label>
                    <input type="number" class="form-control form-control-rb passenger-age" min="1" max="110" placeholder="e.g. 65" required oninput="checkSeniorCitizenAge(${idx})">
                </div>
                <div class="col-md-2">
                    <label class="form-label-custom">Gender *</label>
                    <select class="form-select form-control-rb passenger-gender" required>
                        <option value="Male">Male</option>
                        <option value="Female">Female</option>
                        <option value="Transgender">Transgender</option>
                    </select>
                </div>
                <div class="col-md-3">
                    <label class="form-label-custom">Berth Preference *</label>
                    <select class="form-select form-control-rb passenger-berth" id="berthPref_${idx}">
                        <option value="No Preference">No Preference</option>
                        <option value="Lower">Lower Berth</option>
                        <option value="Middle">Middle Berth</option>
                        <option value="Upper">Upper Berth</option>
                        <option value="Side Lower">Side Lower Berth</option>
                        <option value="Side Upper">Side Upper Berth</option>
                    </select>
                </div>

                <div class="col-md-4">
                    <label class="form-label-custom">Nationality</label>
                    <input type="text" class="form-control form-control-rb passenger-nationality" value="Indian">
                </div>
                <div class="col-md-4">
                    <label class="form-label-custom">Mobile Number</label>
                    <input type="tel" class="form-control form-control-rb passenger-phone" placeholder="10-digit mobile" maxlength="10">
                </div>
                <div class="col-md-4">
                    <label class="form-label-custom">Demo Identity (Aadhaar / ID No.)</label>
                    <input type="text" class="form-control form-control-rb passenger-aadhaar" placeholder="e.g. 9876 5432 1234" maxlength="14" value="9876 5432 1234">
                </div>
            </div>

            <!-- Senior Citizen Auto-Assignment Notice -->
            <div id="seniorNotice_${idx}" class="alert-senior-citizen d-none">
                <i class="fas fa-wheelchair fa-lg text-primary"></i>
                <div>
                    <strong>Senior Citizen Privilege Applied:</strong>
                    <span>Passenger is above 60 years. Lower berth has been automatically preferred.</span>
                </div>
            </div>
        </div>
    `;

    container.appendChild(card);
    updateFareSummary();
}

function removePassengerRow(idx) {
    const card = document.getElementById(`passengerCard_${idx}`);
    if (card) {
        card.remove();
        // Re-index remaining headers
        const container = document.getElementById('passengersContainer');
        Array.from(container.children).forEach((c, i) => {
            const h = c.querySelector('h6');
            if (h) h.innerHTML = `<i class="fas fa-user-tag me-2"></i> Passenger ${i + 1}`;
        });
        updateFareSummary();
    }
}

// CRITICAL REQUIREMENT 11:
// Senior Citizen Lower Berth Logic (Age > 60)
function checkSeniorCitizenAge(idx) {
    const card = document.getElementById(`passengerCard_${idx}`);
    if (!card) return;

    const ageInput = card.querySelector('.passenger-age');
    const berthSelect = document.getElementById(`berthPref_${idx}`);
    const noticeEl = document.getElementById(`seniorNotice_${idx}`);

    const age = parseInt(ageInput.value || '0');

    if (age > 60) {
        // Automatically prefer and lock to Lower Berth
        berthSelect.value = 'Lower';
        noticeEl.classList.remove('d-none');
    } else {
        noticeEl.classList.add('d-none');
    }
}

// Update live fare calculation
function updateFareSummary() {
    if (!currentTrain) return;

    const passengerCards = document.querySelectorAll('.passenger-row-card');
    const passengerCount = Math.max(1, passengerCards.length);

    const travelClass = document.getElementById('selectTravelClass').value;
    const quota = document.getElementById('selectQuota').value;

    let basePerTicket = travelClass.includes('AC') ? parseFloat(currentTrain.fare_ac) : parseFloat(currentTrain.fare_sleeper);
    let surcharge = 0;
    if (quota === 'Tatkal') {
        surcharge = parseFloat(currentTrain.tatkal_surcharge || 150);
    }

    const perTicketTotal = basePerTicket + surcharge;
    const grandTotal = perTicketTotal * passengerCount;

    document.getElementById('summaryCount').textContent = `${passengerCount} Passenger(s)`;
    document.getElementById('summaryClass').textContent = `${travelClass} (${quota})`;
    document.getElementById('summaryFarePer').textContent = formatINR(perTicketTotal);
    document.getElementById('summaryGrandTotal').textContent = formatINR(grandTotal);
}

// Handle Form Submission
async function handleBookingSubmit(e) {
    e.preventDefault();

    const submitBtn = document.getElementById('btnSubmitBooking');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Processing Reservation...';

    try {
        const trainId = document.getElementById('hiddenTrainId').value;
        const journeyDate = document.getElementById('inputJourneyDate').value;
        const travelClass = document.getElementById('selectTravelClass').value;
        const quota = document.getElementById('selectQuota').value;

        // Collect passenger details
        const cards = document.querySelectorAll('.passenger-row-card');
        const passengers = [];

        cards.forEach((card, i) => {
            passengers.push({
                name: card.querySelector('.passenger-name').value.trim(),
                age: card.querySelector('.passenger-age').value.trim(),
                gender: card.querySelector('.passenger-gender').value,
                berthPreference: card.querySelector('.passenger-berth').value,
                nationality: card.querySelector('.passenger-nationality').value.trim() || 'Indian',
                mobileNumber: card.querySelector('.passenger-phone').value.trim(),
                aadhaarNumber: card.querySelector('.passenger-aadhaar').value.trim()
            });
        });

        // Use FormData for file upload support
        const formData = new FormData();
        formData.append('trainId', trainId);
        formData.append('journeyDate', journeyDate);
        formData.append('travelClass', travelClass);
        formData.append('quota', quota);
        formData.append('passengers', JSON.stringify(passengers));

        const user = getCurrentUser();
        if (user && user.userId) {
            formData.append('userId', user.userId);
        }

        const fileInput = document.getElementById('identityProofUpload');
        if (fileInput && fileInput.files.length > 0) {
            formData.append('identityDocument', fileInput.files[0]);
        }

        const response = await fetch(`${API_BASE}/bookings/create`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (response.ok && result.success) {
            // Store result in sessionStorage for instant confirmation screen
            sessionStorage.setItem('latest_booking', JSON.stringify(result));
            window.location.href = `booking-success.html?pnr=${result.pnr}`;
        } else {
            showToast(result.error || 'Booking reservation failed', 'error');
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="fas fa-lock me-2"></i> Confirm Booking & Pay';
        }

    } catch (err) {
        console.error(err);
        showToast('Error connecting to reservation server', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="fas fa-lock me-2"></i> Confirm Booking & Pay';
    }
}
