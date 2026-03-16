/* ================================================
   AJ TRANSPORTATION — CALENDAR JS
   Weekly slot view with navigation
   ================================================ */

// ---- STATE ----
let currentWeekStart = null;   // Monday of displayed week
let selectedSlot = null;

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];
const SHORT_MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

// Working hours displayed: 06:00 to 19:00
const START_HOUR = 6;
const END_HOUR = 19;

// ---- SAMPLE TRIP DATA (will be replaced with backend API calls) ----
// Format: { date: 'YYYY-MM-DD', time: 'HH:MM', status: 'available'|'booked'|'pending', price: 150, label: 'Route name' }
const SAMPLE_TRIPS = [
    { date: getTodayStr(), time: '08:00', status: 'available', price: 120, label: 'City Route' },
    { date: getTodayStr(), time: '10:00', status: 'booked',    price: 120, label: 'City Route' },
    { date: getTodayStr(), time: '14:00', status: 'available', price: 150, label: 'Airport Run' },
    { date: getDateStr(1), time: '07:00', status: 'available', price: 120, label: 'Morning Run' },
    { date: getDateStr(1), time: '09:00', status: 'available', price: 120, label: 'City Route' },
    { date: getDateStr(1), time: '12:00', status: 'pending',   price: 130, label: 'Midday Trip' },
    { date: getDateStr(1), time: '16:00', status: 'available', price: 120, label: 'Afternoon Run' },
    { date: getDateStr(2), time: '08:00', status: 'available', price: 120, label: 'City Route' },
    { date: getDateStr(2), time: '11:00', status: 'booked',    price: 150, label: 'Special Trip' },
    { date: getDateStr(3), time: '09:00', status: 'available', price: 120, label: 'City Route' },
    { date: getDateStr(4), time: '08:00', status: 'available', price: 120, label: 'City Route' },
    { date: getDateStr(4), time: '15:00', status: 'available', price: 180, label: 'Long Haul' },
];

// ---- UTILS ----
function getTodayStr() {
    return toDateStr(new Date());
}
function getDateStr(daysFromNow) {
    const d = new Date();
    d.setDate(d.getDate() + daysFromNow);
    return toDateStr(d);
}
function toDateStr(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}
function getMondayOf(date) {
    const d = new Date(date);
    const day = d.getDay(); // 0=Sun
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
}
function addDays(date, n) {
    const d = new Date(date);
    d.setDate(d.getDate() + n);
    return d;
}
function formatHour(h) {
    return String(h).padStart(2, '0') + ':00';
}
function isPast(dateStr, timeStr) {
    const now = new Date();
    const [y, m, d] = dateStr.split('-').map(Number);
    const [hh, mm] = timeStr.split(':').map(Number);
    const slot = new Date(y, m - 1, d, hh, mm);
    return slot < now;
}

// ---- INIT ----
document.addEventListener('DOMContentLoaded', () => {
    currentWeekStart = getMondayOf(new Date());
    buildMonthJump();
    renderWeek();
});

// ---- RENDER WEEK ----
function renderWeek() {
    const today = getTodayStr();

    // Update week range label
    const weekEnd = addDays(currentWeekStart, 6);
    const rangeLabel = document.getElementById('weekRangeLabel');
    if (rangeLabel) {
        rangeLabel.textContent =
            `${currentWeekStart.getDate()} ${SHORT_MONTHS[currentWeekStart.getMonth()]} — ` +
            `${weekEnd.getDate()} ${SHORT_MONTHS[weekEnd.getMonth()]} ${weekEnd.getFullYear()}`;
    }

    // Update day headers
    for (let i = 0; i < 7; i++) {
        const dayDate = addDays(currentWeekStart, i);
        const dateStr = toDateStr(dayDate);
        const hdr = document.getElementById(`hdr-${i}`);
        if (!hdr) continue;
        hdr.innerHTML = `
            <span class="day-name">${DAYS[i]}</span>
            <span class="day-num">${dayDate.getDate()}</span>
        `;
        hdr.className = 'week-day-header' + (dateStr === today ? ' today' : '');
    }

    // Build time rows
    const timeRowsContainer = document.getElementById('timeRows');
    if (!timeRowsContainer) return;
    timeRowsContainer.innerHTML = '';

    for (let hour = START_HOUR; hour <= END_HOUR; hour++) {
        const timeStr = formatHour(hour);

        // Time label cell
        const timeLabel = document.createElement('div');
        timeLabel.className = 'time-label';
        timeLabel.textContent = timeStr;
        timeRowsContainer.appendChild(timeLabel);

        // 7 day cells for this hour
        for (let i = 0; i < 7; i++) {
            const dayDate = addDays(currentWeekStart, i);
            const dateStr = toDateStr(dayDate);

            // Find trip for this slot
            const trip = SAMPLE_TRIPS.find(t => t.date === dateStr && t.time === timeStr);
            const past = isPast(dateStr, timeStr);

            const cell = document.createElement('div');
            cell.className = 'week-cell';

            if (past) {
                cell.classList.add('cell-past');
            } else if (trip) {
                cell.classList.add(`cell-${trip.status}`);
                const pill = document.createElement('div');
                pill.className = `slot-pill pill-${trip.status}`;
                pill.innerHTML = `
                    <span class="pill-label">${trip.label}</span>
                    <span class="pill-time">${trip.time}</span>
                    ${trip.status === 'available' ? `<span class="pill-price">R${trip.price}</span>` : ''}
                `;
                cell.appendChild(pill);

                if (trip.status === 'available') {
                    cell.onclick = () => openBookingModal(trip, dateStr, dayDate);
                }
            } else {
                cell.classList.add('cell-empty');
            }

            timeRowsContainer.appendChild(cell);
        }
    }

    // Update upcoming list
    renderUpcomingList();
}

// ---- NAVIGATION ----
function changeWeek(direction) {
    currentWeekStart = addDays(currentWeekStart, direction * 7);
    renderWeek();
    updateMonthJumpSelected();
}

function goToToday() {
    currentWeekStart = getMondayOf(new Date());
    renderWeek();
    updateMonthJumpSelected();
}

function jumpToMonth(value) {
    if (!value) return;
    const [year, month] = value.split('-').map(Number);
    const firstOfMonth = new Date(year, month - 1, 1);
    currentWeekStart = getMondayOf(firstOfMonth);
    renderWeek();
}

// ---- MONTH JUMP SELECT ----
function buildMonthJump() {
    const sel = document.getElementById('monthJump');
    if (!sel) return;
    const now = new Date();
    // Show 12 months: current month - 1 to + 10
    for (let i = -1; i <= 11; i++) {
        const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
        const val = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
        const label = `${MONTHS[d.getMonth()]} ${d.getFullYear()}`;
        const opt = document.createElement('option');
        opt.value = val;
        opt.textContent = label;
        if (i === 0) opt.selected = true;
        sel.appendChild(opt);
    }
}

function updateMonthJumpSelected() {
    const sel = document.getElementById('monthJump');
    if (!sel) return;
    const val = `${currentWeekStart.getFullYear()}-${String(currentWeekStart.getMonth() + 1).padStart(2, '0')}`;
    sel.value = val;
}

// ---- UPCOMING LIST ----
function renderUpcomingList() {
    const list = document.getElementById('upcomingList');
    const summary = document.getElementById('weekSummary');
    if (!list) return;

    const weekDates = Array.from({length: 7}, (_, i) => toDateStr(addDays(currentWeekStart, i)));
    const weekTrips = SAMPLE_TRIPS.filter(t => weekDates.includes(t.date)).sort((a, b) => {
        if (a.date !== b.date) return a.date.localeCompare(b.date);
        return a.time.localeCompare(b.time);
    });

    const available = weekTrips.filter(t => t.status === 'available').length;
    if (summary) summary.textContent = `${weekTrips.length} trips · ${available} available`;

    if (weekTrips.length === 0) {
        list.innerHTML = `
            <div class="upcoming-empty">
                <div style="font-size:2rem;margin-bottom:12px;">📅</div>
                <p>No trips scheduled this week yet.</p>
                <p style="font-size:0.8rem;color:var(--text-muted);">Check back soon or contact the admin.</p>
            </div>`;
        return;
    }

    list.innerHTML = weekTrips.map(trip => {
        const [y, m, d] = trip.date.split('-').map(Number);
        const tripDate = new Date(y, m - 1, d);
        const dayNum = tripDate.getDate();
        const monStr = SHORT_MONTHS[tripDate.getMonth()];
        const dayStr = DAYS[tripDate.getDay() === 0 ? 6 : tripDate.getDay() - 1];
        const statusBadge = trip.status === 'available'
            ? `<span class="badge badge-success"><span class="badge-dot"></span>Available</span>`
            : trip.status === 'pending'
            ? `<span class="badge badge-warning"><span class="badge-dot"></span>Pending</span>`
            : `<span class="badge badge-danger"><span class="badge-dot"></span>Booked</span>`;
        return `
            <div class="upcoming-item">
                <div class="upcoming-date-box">
                    <div class="udb-day">${dayNum}</div>
                    <div class="udb-mon">${monStr}</div>
                </div>
                <div class="upcoming-info">
                    <h4>${trip.label} — ${trip.time}</h4>
                    <p>${dayStr}, ${monStr} ${dayNum} · ${trip.time}</p>
                </div>
                ${statusBadge}
                <div class="upcoming-price">R${trip.price}</div>
            </div>`;
    }).join('');
}

// ---- BOOKING MODAL ----
function openBookingModal(trip, dateStr, dayDate) {
    selectedSlot = { trip, dateStr, dayDate };
    const modal = document.getElementById('bookingModal');
    const infoDiv = document.getElementById('modalTripInfo');
    const priceEl = document.getElementById('modalPrice');
    const [y, m, d] = dateStr.split('-').map(Number);
    const tripDate = new Date(y, m - 1, d);
    const dayName = DAYS[tripDate.getDay() === 0 ? 6 : tripDate.getDay() - 1];

    if (infoDiv) {
        infoDiv.innerHTML = `
            <div class="modal-trip-row">
                <span class="modal-trip-label">Route</span>
                <span class="modal-trip-value">${trip.label}</span>
            </div>
            <div class="modal-trip-row">
                <span class="modal-trip-label">Date</span>
                <span class="modal-trip-value">${dayName}, ${d} ${SHORT_MONTHS[m - 1]} ${y}</span>
            </div>
            <div class="modal-trip-row">
                <span class="modal-trip-label">Time</span>
                <span class="modal-trip-value">${trip.time}</span>
            </div>`;
    }
    if (priceEl) priceEl.textContent = `R${trip.price}`;
    if (modal) modal.classList.add('open');
}

function closeBookingModal() {
    const modal = document.getElementById('bookingModal');
    if (modal) modal.classList.remove('open');
    selectedSlot = null;
}

function closeModal(event) {
    if (event.target.id === 'bookingModal') closeBookingModal();
}

function confirmBooking() {
    if (!selectedSlot) return;
    // TODO: POST to /api/bookings with selectedSlot data, then redirect to Ozow payment
    alert(`Booking confirmed for ${selectedSlot.trip.label} at ${selectedSlot.trip.time} on ${selectedSlot.dateStr}.\n\nPayment integration coming in Phase 7.`);
    closeBookingModal();
}
