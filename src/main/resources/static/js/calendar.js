/**
 * calendar.js — AJ Transportation
 * Phase 6 + Phase 8 fix:
 *  - Trip slots are fully clickable (no pointer-events issues)
 *  - Booking modal opens correctly on click
 *  - 4am–12pm window with correct pixel math
 *  - Month/week navigation
 */

// ─── State ────────────────────────────────────────────────────────────────────
let currentWeekStart = null;

// ─── Init ─────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {

    let trips = [];
    try {
        trips = JSON.parse(TRIPS_DATA);
    } catch (e) {
        console.error('Failed to parse TRIPS_DATA:', e);
        trips = [];
    }

    trips = trips.map(normaliseTrip);
    window._allTrips = trips;

    currentWeekStart = parseLocalDate(WEEK_START);

    buildMonthDropdown();
    renderCalendar(trips, currentWeekStart);
    updateNavButtons();

    // Close modal on backdrop click
    const backdrop = document.getElementById('modal-backdrop');
    if (backdrop) {
        backdrop.addEventListener('click', closeBookingModal);
    }

    // Close modal on Escape key
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeBookingModal();
    });
});

// ─── Normalise Jackson array format → strings ─────────────────────────────────
function normaliseTrip(t) {
    return {
        ...t,
        date:      normaliseDate(t.date),
        startTime: normaliseTime(t.startTime),
        endTime:   normaliseTime(t.endTime),
    };
}

function normaliseDate(d) {
    if (!d) return '';
    if (Array.isArray(d)) {
        return `${d[0]}-${String(d[1]).padStart(2,'0')}-${String(d[2]).padStart(2,'0')}`;
    }
    return String(d);
}

function normaliseTime(t) {
    if (!t) return '';
    if (Array.isArray(t)) {
        return `${String(t[0]).padStart(2,'0')}:${String(t[1]).padStart(2,'0')}`;
    }
    return String(t).substring(0, 5);
}

// ─── Month Dropdown ───────────────────────────────────────────────────────────
function buildMonthDropdown() {
    // Target whichever monthSelect exists on the page (there may be two)
    const selects = document.querySelectorAll('#monthSelect');
    if (!selects.length) return;

    const now         = new Date();
    const currentYear = now.getFullYear();
    const MONTHS      = ['January','February','March','April','May','June',
                         'July','August','September','October','November','December'];

    selects.forEach(select => {
        // Clear existing options (prevent duplicates on re-render)
        select.innerHTML = '';
        for (let m = 0; m < 12; m++) {
            const opt = document.createElement('option');
            opt.value       = `${currentYear}-${String(m + 1).padStart(2,'0')}-01`;
            opt.textContent = `${MONTHS[m]} ${currentYear}`;
            if (m === currentWeekStart.getMonth() && currentYear === currentWeekStart.getFullYear()) {
                opt.selected = true;
            }
            select.appendChild(opt);
        }
    });
}

function jumpToMonth(value) {
    if (!value) return;
    const d = parseLocalDate(value);
    currentWeekStart = getMondayOf(d);
    navigateToWeek(currentWeekStart);
}

function navigateToWeek(mondayDate) {
    window.location.href = `/bookings?week=${formatDate(mondayDate)}`;
}

// ─── Calendar Render ──────────────────────────────────────────────────────────
function renderCalendar(trips, weekStart) {
    const container = document.getElementById('calendar-container');
    if (!container) return;

    const today     = new Date();
    today.setHours(0, 0, 0, 0);

    const dayNames   = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
    const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

    // Build 7 day objects
    const days = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(weekStart);
        d.setDate(weekStart.getDate() + i);
        days.push(d);
    }

    // Calendar window: 04:00–12:00 = 480 min
    const CAL_START_MIN = 4 * 60;   // 240
    const CAL_TOTAL_MIN = 8 * 60;   // 480

    // Row height per hour in pixels (used for accurate slot positioning)
    const PX_PER_HOUR = 60;
    const TOTAL_HEIGHT = PX_PER_HOUR * 8; // 480px for 04:00–12:00

    let html = '<div class="calendar-grid">';

    // ── Header row ──
    html += '<div class="calendar-header-row">';
    html += '<div class="time-gutter"></div>';
    days.forEach((day, i) => {
        const isToday = day.getTime() === today.getTime();
        const isPast  = day < today;
        html += `
            <div class="cal-day-header ${isToday ? 'today' : ''} ${isPast && !isToday ? 'past-day' : ''}">
                <span class="day-name">${dayNames[i]}</span>
                <span class="day-date">${day.getDate()} ${monthNames[day.getMonth()]}</span>
            </div>`;
    });
    html += '</div>';

    // ── Body ──
    html += `<div class="calendar-body" style="height:${TOTAL_HEIGHT}px;">`;

    // Time labels column
    html += '<div class="time-column">';
    for (let h = 4; h <= 12; h++) {
        html += `<div class="time-label" style="height:${PX_PER_HOUR}px;">${String(h).padStart(2,'0')}:00</div>`;
    }
    html += '</div>';

    // Day columns
    days.forEach((day) => {
        const dateStr  = formatDate(day);
        const isPast   = day < today;
        const dayTrips = trips
            .filter(t => t.date === dateStr)
            .sort((a, b) => timeToMinutes(a.startTime) - timeToMinutes(b.startTime));

        html += `<div class="cal-day-column ${isPast ? 'past-col' : ''}" style="height:${TOTAL_HEIGHT}px;">`;

        // Hour grid lines
        for (let h = 0; h <= 8; h++) {
            const topPx = h * PX_PER_HOUR;
            html += `<div class="hour-line" style="top:${topPx}px;"></div>`;
        }

        // Trip slots — rendered as clickable divs
        dayTrips.forEach(trip => {
            const startMin = timeToMinutes(trip.startTime);
            const endMin   = timeToMinutes(trip.endTime);

            // Skip trips completely outside 04:00–12:00
            if (endMin <= CAL_START_MIN || startMin >= CAL_START_MIN + CAL_TOTAL_MIN) return;

            const clampedStart = Math.max(startMin, CAL_START_MIN);
            const clampedEnd   = Math.min(endMin, CAL_START_MIN + CAL_TOTAL_MIN);

            const topPx    = ((clampedStart - CAL_START_MIN) / 60) * PX_PER_HOUR;
            const heightPx = Math.max(((clampedEnd - clampedStart) / 60) * PX_PER_HOUR, 36);

            const isAvailable = trip.status === 'AVAILABLE' && !isPast;
            const isBooked    = trip.status === 'BOOKED';

            const statusClass = isAvailable ? 'slot-available'
                              : isBooked    ? 'slot-booked'
                              : 'slot-blocked';

            const feeText = trip.fee
                ? `R${parseFloat(trip.fee).toFixed(2)}`
                : 'Fee TBC';

            const bookedTag = isBooked
                ? '<span class="slot-status-tag">Booked</span>'
                : '';

            // Available slots get onclick; booked/past slots show a "not available" cursor
            if (isAvailable) {
                const tripData = {
                    id:        trip.id,
                    label:     trip.label || '',
                    startTime: trip.startTime,
                    endTime:   trip.endTime,
                    date:      trip.date,
                    fee:       trip.fee || ''
                };
                const dataAttr = escAttrJson(tripData);

                html += `
                    <div class="trip-slot ${statusClass}"
                         style="top:${topPx}px;height:${heightPx}px;"
                         data-trip='${dataAttr}'
                         onclick="handleSlotClick(this)"
                         role="button"
                         tabindex="0"
                         title="Click to book: ${escAttr(trip.label)}">
                        <span class="slot-time">${trip.startTime} – ${trip.endTime}</span>
                        <span class="slot-label">${escHtml(trip.label || '')}</span>
                        <span class="slot-fee">${feeText}</span>
                        <span class="slot-cta">Tap to book →</span>
                    </div>`;
            } else {
                html += `
                    <div class="trip-slot ${statusClass}"
                         style="top:${topPx}px;height:${heightPx}px;"
                         title="${isBooked ? 'Already booked' : 'Unavailable'}">
                        <span class="slot-time">${trip.startTime} – ${trip.endTime}</span>
                        <span class="slot-label">${escHtml(trip.label || '')}</span>
                        <span class="slot-fee">${feeText}</span>
                        ${bookedTag}
                    </div>`;
            }
        });

        html += '</div>'; // end cal-day-column
    });

    html += '</div>'; // end calendar-body
    html += '</div>'; // end calendar-grid

    container.innerHTML = html;

    // Attach keyboard support (Enter/Space fires click)
    container.querySelectorAll('.trip-slot[role="button"]').forEach(el => {
        el.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleSlotClick(this);
            }
        });
    });
}

// ─── Slot Click Handler ───────────────────────────────────────────────────────
function handleSlotClick(el) {
    try {
        const trip = JSON.parse(el.getAttribute('data-trip'));
        openBookingModal(trip.id, trip.label, trip.startTime, trip.endTime, trip.date, trip.fee);
    } catch (e) {
        console.error('Failed to parse trip data:', e);
    }
}

// ─── Navigation ───────────────────────────────────────────────────────────────
function updateNavButtons() {
    const prevBtn = document.getElementById('prevWeekBtn');
    const nextBtn = document.getElementById('nextWeekBtn');
    if (!prevBtn || !nextBtn) return;

    const todayMonday = getMondayOf(new Date());

    if (currentWeekStart <= todayMonday) {
        prevBtn.classList.add('btn-disabled');
        prevBtn.setAttribute('aria-disabled', 'true');
    } else {
        prevBtn.classList.remove('btn-disabled');
        prevBtn.removeAttribute('aria-disabled');
    }
}

function goToPrevWeek() {
    const prev = new Date(currentWeekStart);
    prev.setDate(prev.getDate() - 7);
    navigateToWeek(prev);
}

function goToNextWeek() {
    const next = new Date(currentWeekStart);
    next.setDate(next.getDate() + 7);
    navigateToWeek(next);
}

// ─── Booking Modal ────────────────────────────────────────────────────────────
function openBookingModal(tripId, label, startTime, endTime, date, fee) {
    const modal    = document.getElementById('booking-modal');
    const backdrop = document.getElementById('modal-backdrop');
    const details  = document.getElementById('booking-details');
    const input    = document.getElementById('tripIdInput');

    if (!modal || !details) {
        console.error('Modal elements not found');
        return;
    }

    const displayDate = new Date(date + 'T00:00:00')
        .toLocaleDateString('en-ZA', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

    const feeHtml = fee && fee !== ''
        ? `<div class="booking-detail-row">
               <span class="detail-label">Trip Fee</span>
               <span class="detail-value fee-highlight">R${parseFloat(fee).toFixed(2)}</span>
           </div>`
        : `<div class="booking-detail-row">
               <span class="detail-label">Trip Fee</span>
               <span class="detail-value" style="color:var(--text-muted);">To be confirmed</span>
           </div>`;

    details.innerHTML = `
        <div class="booking-detail-row">
            <span class="detail-label">Route</span>
            <span class="detail-value">${escHtml(label)}</span>
        </div>
        <div class="booking-detail-row">
            <span class="detail-label">Date</span>
            <span class="detail-value">${displayDate}</span>
        </div>
        <div class="booking-detail-row">
            <span class="detail-label">Time</span>
            <span class="detail-value">${startTime} – ${endTime}</span>
        </div>
        ${feeHtml}
    `;

    if (input) input.value = tripId;

    // Show modal
    modal.classList.remove('hidden');
    if (backdrop) backdrop.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
}

function closeBookingModal() {
    const modal    = document.getElementById('booking-modal');
    const backdrop = document.getElementById('modal-backdrop');
    if (modal)    modal.classList.add('hidden');
    if (backdrop) backdrop.classList.add('hidden');
    document.body.style.overflow = '';
}

// ─── Utilities ────────────────────────────────────────────────────────────────
function timeToMinutes(t) {
    if (!t) return 0;
    const [h, m] = String(t).split(':').map(Number);
    return h * 60 + (m || 0);
}

function formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

function parseLocalDate(str) {
    if (!str) return new Date();
    const [y, m, d] = String(str).split('-').map(Number);
    return new Date(y, m - 1, d);
}

function getMondayOf(date) {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    const day  = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    return d;
}

function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function escAttr(str) {
    return String(str || '').replace(/'/g, '&#39;').replace(/"/g, '&quot;');
}

function escAttrJson(obj) {
    // Serialize to JSON and escape single quotes for use in data-* attribute
    return JSON.stringify(obj).replace(/'/g, '&#39;');
}
