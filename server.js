const express = require('express');
const cors = require('cors');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const qrcode = require('qrcode');
const crypto = require('crypto');
const path = require('path');
const fs = require('fs');
const multer = require('multer');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static frontend files
app.use(express.static(path.join(__dirname, 'src/main/webapp')));
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// Ensure uploads folder exists
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}

// Multer storage for demo ID uploads
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadsDir),
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, 'id-demo-' + uniqueSuffix + path.extname(file.originalname));
    }
});
const upload = multer({
    storage: storage,
    limits: { fileSize: 5 * 1024 * 1024 }, // 5 MB
    fileFilter: (req, file, cb) => {
        const allowed = /jpeg|jpg|png|pdf/;
        const ext = allowed.test(path.extname(file.originalname).toLowerCase());
        const mime = allowed.test(file.mimetype);
        if (ext && mime) {
            cb(null, true);
        } else {
            cb(new Error('Only JPG, JPEG, PNG, and PDF files are allowed.'));
        }
    }
});

// MySQL Database Connection Pool
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || 'kailash',
    database: process.env.DB_NAME || 'railbook_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Helper: Mask Identifier
function maskAadhaar(raw) {
    if (!raw || raw.length < 4) return 'XXXX XXXX 1234';
    const clean = raw.toString().replace(/\s+/g, '');
    const last4 = clean.slice(-4);
    return `XXXX XXXX ${last4}`;
}

// Helper: Generate PNR
function generatePNR() {
    const p1 = Math.floor(1000 + Math.random() * 9000);
    const p2 = Math.floor(100000 + Math.random() * 900000);
    return `${p1}${p2}`;
}

// Helper: Generate Booking ID
function generateBookingID() {
    const timestamp = Date.now().toString().slice(-6);
    const random = Math.floor(100 + Math.random() * 900);
    return `RB${timestamp}${random}`;
}

// ==========================================
// 1. AUTHENTICATION ENDPOINTS
// ==========================================

// Passenger Registration
app.post('/api/auth/register', async (req, res) => {
    try {
        const { name, email, phone, password } = req.body;
        if (!name || !email || !phone || !password) {
            return res.status(400).json({ error: 'All registration fields are required.' });
        }

        const [existing] = await pool.query('SELECT user_id FROM users WHERE email = ?', [email.trim().toLowerCase()]);
        if (existing.length > 0) {
            return res.status(409).json({ error: 'An account with this email already exists.' });
        }

        const hash = await bcrypt.hash(password, 10);
        const [result] = await pool.query(
            'INSERT INTO users (name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, ?)',
            [name.trim(), email.trim().toLowerCase(), phone.trim(), hash, 'PASSENGER']
        );

        res.json({
            success: true,
            message: 'Registration successful! You may now log in.',
            userId: result.insertId,
            name: name.trim()
        });
    } catch (err) {
        console.error('Register Error:', err);
        res.status(500).json({ error: 'Database error: ' + err.message });
    }
});

// Passenger Login
app.post('/api/auth/login', async (req, res) => {
    try {
        const { identifier, password } = req.body;
        if (!identifier || !password) {
            return res.status(400).json({ error: 'Email/mobile and password required.' });
        }

        const [rows] = await pool.query(
            'SELECT * FROM users WHERE email = ? OR phone = ?',
            [identifier.trim().toLowerCase(), identifier.trim()]
        );

        if (rows.length === 0) {
            return res.status(401).json({ error: 'Invalid credentials. User not found.' });
        }

        const user = rows[0];
        const match = await bcrypt.compare(password, user.password_hash);
        if (!match && password !== 'Pass@123') { // Fallback for demo convenience
            return res.status(401).json({ error: 'Invalid password. Please verify your credentials.' });
        }

        res.json({
            success: true,
            message: 'Login successful!',
            user: {
                userId: user.user_id,
                name: user.name,
                email: user.email,
                phone: user.phone,
                role: user.role
            }
        });
    } catch (err) {
        console.error('Login Error:', err);
        res.status(500).json({ error: 'Database error: ' + err.message });
    }
});

// ==========================================
// 2. TRAIN SEARCH & STATIONS
// ==========================================

// Search Trains with filters
app.get('/api/trains/search', async (req, res) => {
    try {
        const { from, to, date, class: travelClass, quota, available } = req.query;
        let sql = 'SELECT * FROM trains WHERE 1=1';
        const params = [];

        if (from && from.trim() !== '') {
            sql += ' AND LOWER(source) LIKE ?';
            params.push(`%${from.trim().toLowerCase()}%`);
        }
        if (to && to.trim() !== '') {
            sql += ' AND LOWER(destination) LIKE ?';
            params.push(`%${to.trim().toLowerCase()}%`);
        }
        if (available === 'true') {
            sql += ' AND available_seats > 0';
        }

        sql += ' ORDER BY departure_time ASC';
        const [trains] = await pool.query(sql, params);
        res.json(trains);
    } catch (err) {
        console.error('Train Search Error:', err);
        res.status(500).json({ error: 'Failed to search trains.' });
    }
});

// Get all stations
app.get('/api/trains/stations', async (req, res) => {
    try {
        const [stations] = await pool.query('SELECT * FROM stations ORDER BY station_name ASC');
        res.json(stations);
    } catch (err) {
        res.status(500).json({ error: 'Failed to fetch stations.' });
    }
});

// Get single train details
app.get('/api/trains/:id', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM trains WHERE train_id = ? OR train_number = ?', [req.params.id, req.params.id]);
        if (rows.length === 0) return res.status(404).json({ error: 'Train not found' });
        res.json(rows[0]);
    } catch (err) {
        res.status(500).json({ error: 'Database error' });
    }
});

// ==========================================
// 3. TATKAL-STYLE BOOKING ENGINE
// ==========================================

app.post('/api/bookings/create', upload.single('identityDocument'), async (req, res) => {
    const conn = await pool.getConnection();
    try {
        await conn.beginTransaction();

        let { trainId, journeyDate, travelClass, quota, passengers, userId } = req.body;
        if (!userId) userId = 2; // Default passenger demo

        if (typeof passengers === 'string') {
            passengers = JSON.parse(passengers);
        }

        if (!trainId || !journeyDate || !passengers || passengers.length === 0) {
            await conn.rollback();
            return res.status(400).json({ error: 'Missing booking parameters or passengers.' });
        }

        const [trainRows] = await conn.query('SELECT * FROM trains WHERE train_id = ?', [trainId]);
        if (trainRows.length === 0) {
            await conn.rollback();
            return res.status(404).json({ error: 'Train not found' });
        }
        const train = trainRows[0];

        // Coach Prefix
        let coach = 'S3';
        if (travelClass && travelClass.includes('AC 1')) coach = 'H1';
        else if (travelClass && travelClass.includes('AC 2')) coach = 'A1';
        else if (travelClass && travelClass.includes('AC 3')) coach = 'B2';
        else if (travelClass && travelClass.includes('Chair')) coach = 'C1';

        // Calculate Fare
        let baseFare = (travelClass && travelClass.includes('AC')) ? parseFloat(train.fare_ac) : parseFloat(train.fare_sleeper);
        if (quota === 'Tatkal') {
            baseFare += parseFloat(train.tatkal_surcharge || 150);
        }
        const totalFare = baseFare * passengers.length;

        const pnr = generatePNR();
        const bookingId = generateBookingID();

        // 1. Insert Booking
        await conn.query(
            `INSERT INTO bookings (booking_id, pnr, user_id, train_id, journey_date, source, destination, travel_class, quota, fare, status)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [bookingId, pnr, userId, trainId, journeyDate, train.source, train.destination, travelClass || 'Sleeper', quota || 'General', totalFare, 'WAITING LIST']
        );

        // 2. Insert Passengers with Senior Citizen Logic
        const seniorAlerts = [];
        let baseSeat = 14 + Math.floor(Math.random() * 30);

        for (let i = 0; i < passengers.length; i++) {
            const p = passengers[i];
            const age = parseInt(p.age);
            let isSenior = false;
            let berth = p.berthPreference || 'No Preference';
            let seatStr = '';

            // CRITICAL REQUIREMENT 11:
            // "If Age > 60 then automatically assign: Lower Berth / Down Seat"
            if (age > 60) {
                isSenior = true;
                berth = 'Lower';
                const lowerSeatNum = Math.floor(baseSeat / 8) * 8 + 4;
                seatStr = `${lowerSeatNum} (Lower Berth)`;
                seniorAlerts.push(`${p.name} is above 60 years. Lower berth has been automatically preferred.`);
            } else {
                if (berth === 'No Preference' || !berth) {
                    const opts = ['Lower', 'Middle', 'Upper', 'Side Lower', 'Side Upper'];
                    berth = opts[Math.floor(Math.random() * opts.length)];
                }
                seatStr = `${baseSeat} (${berth} Berth)`;
            }
            baseSeat++;

            const [pResult] = await conn.query(
                `INSERT INTO passengers (booking_id, name, age, gender, berth_preference, nationality, mobile_number, coach, seat, status, is_senior_citizen)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                [bookingId, p.name, age, p.gender, berth, p.nationality || 'Indian', p.mobileNumber || '', coach, seatStr, 'WAITING LIST', isSenior]
            );
            const passengerId = pResult.insertId;

            // Identity Document Record
            const maskedId = maskAadhaar(p.aadhaarNumber || '987654321234');
            const docFile = req.file ? req.file.filename : 'dummy_aadhaar_demo.pdf';
            const docPath = req.file ? `uploads/${req.file.filename}` : 'uploads/dummy_aadhaar_demo.pdf';

            await conn.query(
                `INSERT INTO identity_documents (passenger_id, document_type, file_name, encrypted_file_reference, masked_identifier, verification_status)
                 VALUES (?, ?, ?, ?, ?, ?)`,
                [passengerId, 'Aadhaar Card (Demo)', docFile, docPath, maskedId, 'VERIFIED']
            );
        }

        // 3. Generate Secure Random QR Verification Token
        // CRITICAL REQUIREMENT 13:
        // "The QR code should contain only the secure verification token or verification URL.
        // The QR must NOT contain: Full Aadhaar number, Aadhaar image, Password, Personal sensitive information."
        const rawToken = `RB-SECURE-${pnr}-${crypto.randomBytes(8).toString('hex')}`;
        const tokenHash = crypto.createHash('sha256').update(rawToken).digest('hex');
        const verificationUrl = `https://railbook.internal/ttr/verify/${rawToken}`;
        const expiresAt = new Date(Date.now() + 72 * 3600 * 1000); // 72 hours

        await conn.query(
            `INSERT INTO qr_verifications (booking_id, secure_token_hash, raw_token, verification_url, expires_at, status)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [bookingId, tokenHash, rawToken, verificationUrl, expiresAt, 'ACTIVE']
        );

        await conn.commit();

        // Generate QR Code image (base64)
        const qrCodeBase64 = await qrcode.toDataURL(verificationUrl, {
            errorCorrectionLevel: 'H',
            margin: 1,
            width: 260
        });

        res.json({
            success: true,
            message: 'Booking Submitted! Awaiting Admin Confirmation.',
            pnr,
            bookingId,
            status: 'WAITING LIST',
            totalFare,
            journeyDate,
            source: train.source,
            destination: train.destination,
            trainNumber: train.train_number,
            trainName: train.train_name,
            qrVerificationUrl: verificationUrl,
            rawToken: rawToken,
            qrCodeBase64: qrCodeBase64,
            seniorCitizenAlerts: seniorAlerts
        });

    } catch (err) {
        await conn.rollback();
        console.error('Booking Error:', err);
        res.status(500).json({ error: 'Booking transaction failed: ' + err.message });
    } finally {
        conn.release();
    }
});

// Passenger My Bookings
app.get('/api/bookings/my', async (req, res) => {
    try {
        const userId = req.query.userId || 2;
        const [bookings] = await pool.query(
            `SELECT b.*, t.train_number, t.train_name, t.departure_time, t.arrival_time
             FROM bookings b
             JOIN trains t ON b.train_id = t.train_id
             WHERE b.user_id = ?
             ORDER BY b.created_at DESC`,
            [userId]
        );

        for (const b of bookings) {
            const [passengers] = await pool.query(
                `SELECT p.*, d.masked_identifier, d.document_type
                 FROM passengers p
                 LEFT JOIN identity_documents d ON p.passenger_id = d.passenger_id
                 WHERE p.booking_id = ?`,
                [b.booking_id]
            );
            b.passengers = passengers;

            const [qrRows] = await pool.query(
                'SELECT * FROM qr_verifications WHERE booking_id = ? ORDER BY qr_id DESC LIMIT 1',
                [b.booking_id]
            );
            if (qrRows.length > 0) {
                b.qrVerification = qrRows[0];
            }
        }

        res.json(bookings);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Get Single Booking by PNR or Booking ID (For Digital Ticket)
app.get('/api/bookings/:identifier', async (req, res) => {
    try {
        const id = req.params.identifier.trim();
        const [bookings] = await pool.query(
            `SELECT b.*, t.train_number, t.train_name, t.departure_time, t.arrival_time, u.name as user_name, u.phone as user_phone, u.email as user_email
             FROM bookings b
             JOIN trains t ON b.train_id = t.train_id
             JOIN users u ON b.user_id = u.user_id
             WHERE b.pnr = ? OR b.booking_id = ?`,
            [id, id]
        );

        if (bookings.length === 0) {
            return res.status(404).json({ error: 'No booking found for identifier: ' + id });
        }

        const booking = bookings[0];

        const [passengers] = await pool.query(
            `SELECT p.*, d.masked_identifier, d.document_type, d.document_id
             FROM passengers p
             LEFT JOIN identity_documents d ON p.passenger_id = d.passenger_id
             WHERE p.booking_id = ?`,
            [booking.booking_id]
        );
        booking.passengers = passengers;

        const [qrRows] = await pool.query(
            'SELECT * FROM qr_verifications WHERE booking_id = ? ORDER BY qr_id DESC LIMIT 1',
            [booking.booking_id]
        );

        if (qrRows.length > 0) {
            booking.qrVerification = qrRows[0];
            booking.qrCodeBase64 = await qrcode.toDataURL(qrRows[0].verification_url, {
                errorCorrectionLevel: 'H',
                margin: 1,
                width: 260
            });
        }

        res.json(booking);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Cancel Booking & Revoke QR
app.post('/api/bookings/cancel', async (req, res) => {
    try {
        const { bookingId } = req.body;
        if (!bookingId) return res.status(400).json({ error: 'Booking ID required' });

        await pool.query('UPDATE bookings SET status = ? WHERE booking_id = ?', ['CANCELLED', bookingId]);
        await pool.query('UPDATE passengers SET status = ? WHERE booking_id = ?', ['CANCELLED', bookingId]);
        await pool.query('UPDATE qr_verifications SET status = ? WHERE booking_id = ?', ['REVOKED', bookingId]);

        res.json({ success: true, message: 'Ticket cancelled successfully. Verification QR has been revoked.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Public PNR Status Lookup
app.get('/api/pnr/:pnr', async (req, res) => {
    try {
        const pnr = req.params.pnr.trim();
        const [bookings] = await pool.query(
            `SELECT b.*, t.train_number, t.train_name, t.departure_time, t.arrival_time
             FROM bookings b
             JOIN trains t ON b.train_id = t.train_id
             WHERE b.pnr = ?`,
            [pnr]
        );

        if (bookings.length === 0) {
            return res.status(404).json({ error: `PNR ${pnr} not found in the Railway database.` });
        }

        const booking = bookings[0];
        const [passengers] = await pool.query(
            `SELECT name, age, gender, berth_preference, coach, seat, status, is_senior_citizen FROM passengers WHERE booking_id = ?`,
            [booking.booking_id]
        );
        booking.passengers = passengers;

        const [qrRows] = await pool.query(
            'SELECT * FROM qr_verifications WHERE booking_id = ? ORDER BY qr_id DESC LIMIT 1',
            [booking.booking_id]
        );
        if (qrRows.length > 0) {
            booking.qrVerification = qrRows[0];
            booking.qrCodeBase64 = await qrcode.toDataURL(qrRows[0].verification_url, { margin: 1, width: 220 });
        }

        res.json(booking);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 4. ADMIN DASHBOARD & MANAGEMENT
// ==========================================

// Admin Login
app.post('/api/admin/login', (req, res) => {
    const { email, password } = req.body;
    const defaultEmail = process.env.ADMIN_EMAIL || 'kailashwar24@gmail.com';
    const defaultPass = process.env.ADMIN_PASSWORD || 'Kailash@24';

    if (email && email.trim() === defaultEmail && password === defaultPass) {
        return res.json({
            success: true,
            message: 'Admin authorization granted',
            role: 'ADMIN',
            name: 'System Administrator'
        });
    }
    return res.status(401).json({ error: 'Invalid Administrator credentials.' });
});

// Admin Dashboard Statistics
app.get('/api/admin/stats', async (req, res) => {
    try {
        const [stats] = await pool.query(`
            SELECT
                COUNT(*) as totalBookings,
                SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END) as confirmedTickets,
                SUM(CASE WHEN status = 'WAITING LIST' THEN 1 ELSE 0 END) as waitingList,
                SUM(CASE WHEN status = 'NOT SELECTED' THEN 1 ELSE 0 END) as notSelected,
                SUM(CASE WHEN DATE(created_at) = CURDATE() THEN 1 ELSE 0 END) as todayBookings
            FROM bookings
        `);

        const [pStats] = await pool.query('SELECT COUNT(*) as totalPassengers FROM passengers');

        res.json({
            totalBookings: stats[0].totalBookings || 0,
            confirmedTickets: stats[0].confirmedTickets || 0,
            waitingList: stats[0].waitingList || 0,
            notSelected: stats[0].notSelected || 0,
            todayBookings: stats[0].todayBookings || 0,
            totalPassengers: pStats[0].totalPassengers || 0
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Admin Passenger Bookings Table (Masked Aadhaar strictly enforced!)
app.get('/api/admin/bookings', async (req, res) => {
    try {
        const [bookings] = await pool.query(`
            SELECT b.*, t.train_number, t.train_name, u.name as user_name, u.phone as user_phone
            FROM bookings b
            JOIN trains t ON b.train_id = t.train_id
            JOIN users u ON b.user_id = u.user_id
            ORDER BY b.created_at DESC
        `);

        for (const b of bookings) {
            const [passengers] = await pool.query(`
                SELECT p.*, d.masked_identifier, d.document_type
                FROM passengers p
                LEFT JOIN identity_documents d ON p.passenger_id = d.passenger_id
                WHERE p.booking_id = ?
            `, [b.booking_id]);
            b.passengers = passengers;
        }

        res.json(bookings);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Admin Ticket Decision: CONFIRM TICKET / WAITING LIST / NOT SELECTED
app.post('/api/admin/decide', async (req, res) => {
    try {
        const { bookingId, decision } = req.body;
        if (!bookingId || !decision) {
            return res.status(400).json({ error: 'Booking ID and decision action required.' });
        }

        const validDecisions = ['CONFIRMED', 'WAITING LIST', 'NOT SELECTED'];
        if (!validDecisions.includes(decision.toUpperCase())) {
            return res.status(400).json({ error: 'Decision must be CONFIRMED, WAITING LIST, or NOT SELECTED.' });
        }

        const newStatus = decision.toUpperCase();
        await pool.query('UPDATE bookings SET status = ? WHERE booking_id = ?', [newStatus, bookingId]);
        await pool.query('UPDATE passengers SET status = ? WHERE booking_id = ?', [newStatus, bookingId]);

        if (newStatus === 'NOT SELECTED') {
            await pool.query('UPDATE qr_verifications SET status = ? WHERE booking_id = ?', ['REVOKED', bookingId]);
        }

        res.json({
            success: true,
            message: `Ticket status updated to ${newStatus}`,
            bookingId,
            newStatus
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Admin Train Management CRUD
app.get('/api/admin/trains', async (req, res) => {
    try {
        const [trains] = await pool.query('SELECT * FROM trains ORDER BY train_number ASC');
        res.json(trains);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/admin/trains/add', async (req, res) => {
    try {
        const { trainNumber, trainName, source, destination, departureTime, arrivalTime, totalSeats, availableSeats, acSeats, sleeperSeats, tatkalSeats, fareAc, fareSleeper } = req.body;

        const [result] = await pool.query(
            `INSERT INTO trains (train_number, train_name, source, destination, departure_time, arrival_time, total_seats, available_seats, ac_seats, sleeper_seats, tatkal_seats, fare_ac, fare_sleeper)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [trainNumber, trainName, source, destination, departureTime, arrivalTime, totalSeats || 120, availableSeats || 80, acSeats || 40, sleeperSeats || 60, tatkalSeats || 20, fareAc || 1200, fareSleeper || 450]
        );

        res.json({ success: true, message: 'Train added successfully!', trainId: result.insertId });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/api/admin/trains/delete', async (req, res) => {
    try {
        const { trainId } = req.body;
        await pool.query('DELETE FROM trains WHERE train_id = ?', [trainId]);
        res.json({ success: true, message: 'Train deleted successfully.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ==========================================
// 5. TTR VERIFICATION & SCANNER
// ==========================================

// TTR Login
app.post('/api/ttr/login', (req, res) => {
    const { ttrId, password } = req.body;
    if ((ttrId === 'ttr_officer' || ttrId === 'TTR-2026') && password === 'TTR@123') {
        return res.json({
            success: true,
            message: 'TTR Verification Officer authenticated',
            ttr: {
                ttrId: 'TTR-2026',
                name: 'Inspector K. Ramanathan',
                badgeNumber: 'SR-TTR-8942',
                division: 'Southern Railway - Chennai Division'
            }
        });
    }
    return res.status(401).json({ error: 'Invalid TTR Officer ID or Password.' });
});

// TTR QR Token Verification Flow
app.post('/api/ttr/verify', async (req, res) => {
    try {
        let { token, action } = req.body;
        if (!token) return res.status(400).json({ error: 'Token required.' });

        action = action || 'QR_SCAN';
        let cleanToken = token.trim();
        if (cleanToken.includes('/ttr/verify/')) {
            cleanToken = cleanToken.split('/ttr/verify/').pop();
        }

        // Query verification registry
        const [qrRows] = await pool.query(
            `SELECT * FROM qr_verifications WHERE raw_token = ? OR secure_token_hash = SHA2(?, 256)`,
            [cleanToken, cleanToken]
        );

        if (qrRows.length === 0) {
            // Log failed scan attempt
            await pool.query(
                `INSERT INTO verification_audit_log (ttr_id, booking_id, result, action, notes)
                 VALUES (?, ?, ?, ?, ?)`,
                ['TTR-2026', 'UNKNOWN', 'INVALID_TOKEN', action, 'QR verification failed: Token not found in railway registry']
            );

            return res.json({
                verified: false,
                statusBadge: 'INVALID',
                title: '✗ VERIFICATION FAILED',
                message: 'Invalid QR / Verification Token not found in Railway database.'
            });
        }

        const qr = qrRows[0];
        const [bRows] = await pool.query(
            `SELECT b.*, t.train_number, t.train_name, t.departure_time, t.arrival_time
             FROM bookings b
             JOIN trains t ON b.train_id = t.train_id
             WHERE b.booking_id = ?`,
            [qr.booking_id]
        );

        if (bRows.length === 0) {
            return res.json({
                verified: false,
                statusBadge: 'INVALID',
                title: '✗ VERIFICATION FAILED',
                message: 'Booking not found for this token.'
            });
        }

        const booking = bRows[0];

        // Check if ticket is Cancelled, Revoked, or Expired
        if (booking.status === 'CANCELLED' || qr.status === 'REVOKED' || booking.status === 'NOT SELECTED') {
            await pool.query(
                `INSERT INTO verification_audit_log (ttr_id, booking_id, result, action, notes)
                 VALUES (?, ?, ?, ?, ?)`,
                ['TTR-2026', booking.booking_id, 'CANCELLED', action, `Attempted scan on ${booking.status} ticket`]
            );

            return res.json({
                verified: false,
                statusBadge: 'INVALID',
                title: '✗ VERIFICATION FAILED',
                message: `Ticket Status: ${booking.status}. Travel unauthorized.`,
                pnr: booking.pnr
            });
        }

        // Check expiration
        if (new Date(qr.expires_at) < new Date()) {
            return res.json({
                verified: false,
                statusBadge: 'INVALID',
                title: '✗ VERIFICATION FAILED',
                message: 'Ticket QR has expired for this journey.',
                pnr: booking.pnr
            });
        }

        // Fetch passengers and masked identity info
        const [passengers] = await pool.query(
            `SELECT p.*, d.document_id, d.document_type, d.masked_identifier, d.verification_status
             FROM passengers p
             LEFT JOIN identity_documents d ON p.passenger_id = d.passenger_id
             WHERE p.booking_id = ?`,
            [booking.booking_id]
        );
        booking.passengers = passengers;

        // Log successful verification audit
        await pool.query(
            `INSERT INTO verification_audit_log (ttr_id, booking_id, result, action, notes)
             VALUES (?, ?, ?, ?, ?)`,
            ['TTR-2026', booking.booking_id, 'VERIFIED', action, `Successful verification on train ${booking.train_number}`]
        );

        return res.json({
            verified: true,
            statusBadge: 'VERIFIED',
            title: '✓ TICKET VERIFIED',
            message: 'Authorized Passenger Travel Confirmed',
            booking: booking
        });

    } catch (err) {
        console.error('TTR Verify Error:', err);
        res.status(500).json({ error: err.message });
    }
});

// TTR Secure Identity Document View (Controlled with audit log)
app.get('/api/documents/view', async (req, res) => {
    try {
        const { id, ttrId } = req.query;
        if (!id) return res.status(400).json({ error: 'Document ID required' });

        const [rows] = await pool.query(
            `SELECT d.*, p.name as passenger_name, p.age, p.gender, p.booking_id, b.pnr
             FROM identity_documents d
             JOIN passengers p ON d.passenger_id = p.passenger_id
             JOIN bookings b ON p.booking_id = b.booking_id
             WHERE d.document_id = ?`,
            [id]
        );

        if (rows.length === 0) return res.status(404).json({ error: 'Document record not found' });
        const doc = rows[0];

        // Audit Log entry
        await pool.query(
            `INSERT INTO verification_audit_log (ttr_id, booking_id, result, action, notes)
             VALUES (?, ?, ?, ?, ?)`,
            [ttrId || 'TTR-2026', doc.booking_id, 'VERIFIED', 'VIEW_IDENTITY_DOC', `Authorized inspection of demo identity document for ${doc.passenger_name}`]
        );

        res.json({
            success: true,
            documentId: doc.document_id,
            documentType: doc.document_type,
            passengerName: doc.passenger_name,
            maskedIdentifier: doc.masked_identifier,
            verificationStatus: 'VERIFIED',
            notice: 'Demo Identity Verification – Use Test/Dummy Documents Only. Document access logged.',
            svgBadge: `<svg width="100%" height="220" viewBox="0 0 380 220" xmlns="http://www.w3.org/2000/svg">
                <rect width="100%" height="100%" rx="12" fill="#0d3b66"/>
                <rect x="8" y="8" width="364" height="204" rx="8" fill="#ffffff"/>
                <rect x="8" y="8" width="364" height="42" rx="8" fill="#0d3b66"/>
                <text x="25" y="34" font-family="'Inter', sans-serif" font-size="14" font-weight="700" fill="#ffffff" letter-spacing="1">DEMO IDENTITY PROOF CARD</text>
                <text x="25" y="68" font-family="'Inter', sans-serif" font-size="10" font-weight="600" fill="#dc2626">STUDENT PROJECT / DEMO RECORD ONLY</text>
                <line x1="25" y1="78" x2="355" y2="78" stroke="#e2e8f0" stroke-width="1.5"/>
                <rect x="25" y="90" width="70" height="85" rx="6" fill="#f1f5f9" stroke="#cbd5e1"/>
                <text x="45" y="140" font-family="'Inter', sans-serif" font-size="28" fill="#94a3b8">👤</text>
                <text x="110" y="105" font-family="'Inter', sans-serif" font-size="11" fill="#64748b">Passenger Name</text>
                <text x="110" y="123" font-family="'Inter', sans-serif" font-size="14" font-weight="700" fill="#0f172a">${doc.passenger_name}</text>
                <text x="110" y="145" font-family="'Inter', sans-serif" font-size="11" fill="#64748b">Identity Number (Masked)</text>
                <text x="110" y="165" font-family="'Courier New', monospace" font-size="16" font-weight="700" fill="#0284c7" letter-spacing="2">${doc.masked_identifier}</text>
                <rect x="25" y="185" width="330" height="20" rx="4" fill="#ecfdf5"/>
                <text x="100" y="199" font-family="'Inter', sans-serif" font-size="10" font-weight="700" fill="#059669">✓ VERIFIED FOR RAILWAY TRAVEL</text>
            </svg>`
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// TTR Audit Logs List
app.get('/api/ttr/audit-logs', async (req, res) => {
    try {
        const [logs] = await pool.query(`
            SELECT a.*, t.name as ttr_name, b.pnr
            FROM verification_audit_log a
            LEFT JOIN ttr_users t ON a.ttr_id = t.ttr_id
            LEFT JOIN bookings b ON a.booking_id = b.booking_id
            ORDER BY a.verification_time DESC
            LIMIT 50
        `);
        res.json(logs);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Start Server
app.listen(PORT, () => {
    console.log(`=======================================================`);
    console.log(` RailBook - Railway Ticket Management System`);
    console.log(` Running on: http://localhost:${PORT}`);
    console.log(` Unified Login:    http://localhost:${PORT}/login.html`);
    console.log(` (Passenger Main Login, Admin Button & TTR Button on same page)`);
    console.log(`=======================================================`);
});
