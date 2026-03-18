/**
 * calendar.js — AJ Transportation
 * Enhanced Phase 6: Month/week navigation, smart next-available slot logic,
 * 4am–12pm window, proper available/booked visual states.
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

    // Normalise all dates/times from arrays (Jackson serialises LocalDate/LocalTime as arrays)
    trips = trips.map(normaliseTrip);

    // Store globally so month-jump can re-render
    window._allTrips = trips;

    // Parse week start from server
    currentWeekStart = parseLocalDate(WEEK_START);

    buildMonthDropdown();
    renderCalendar(trips, currentWeekStart);
    updateNavButtons();
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
    return d;
}

function normaliseTime(t) {
    if (!t) return '';
    if (Array.isArray(t)) {
        return `${String(t[0]).padStart(2,'0')}:${String(t[1]).padStart(2,'0')}`;
    }
    // Already a string like "08:00:00" or "08:00"
    return t.substring(0, 5);
}

// ─── Month Dropdown ───────────────────────────────────────────────────────────
function buildMonthDropdown() {
    const select = document.getElementById('monthSelect');
    if (!select) return;

    const now = new Date();
    const currentYear = now.getFullYear();
    const MONTHS = ['January','February','March','April','May','June',
                    'July','August','September','October','November','December'];

    // Build Jan–Dec of the current year
    for (let m = 0; m < 12; m++) {
        const opt = document.createElement('option');
        opt.value = `${currentYear}-${String(m + 1).padStart(2,'0')}-01`;
        opt.textContent = `${MONTHS[m]} ${currentYear}`;
        // Pre-select the month matching current week
        if (m === currentWeekStart.getMonth() && currentYear === currentWeekStart.getFullYear()) {
            opt.selected = true;
        }
        select.appendChild(opt);
    }
}

function jumpToMonth(value) {
    if (!value) return;
    const d = parseLocalDate(value);
    // Jump to the Monday of the first week of that month
    currentWeekStart = getMondayOf(d);
    navigateToWeek(currentWeekStart);
}

// Navigate by reloading the page with the correct ?week= param
// This keeps the server in sync (it fetches trips for the requested week)
function navigateToWeek(mondayDate) {
    const dateStr = formatDate(mondayDate);
    window.location.href = `/bookings?week=${dateStr}`;
}

// ─── Calendar Render ──────────────────────────────────────────────────────────
function renderCalendar(trips, weekStart) {
    const container = document.getElementById('calendar-container');
    if (!container) return;

    const today = new Date();
    today.setHours(0,0,0,0);

    const dayNames  = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
    const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

    // Build 7 day objects for this week
    const days = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(weekStart);
        d.setDate(weekStart.getDate() + i);
        days.push(d);
    }

    // Calendar window: 04:00 – 12:00 = 480 minutes
    const CAL_START_MIN = 4 * 60;
    const CAL_TOTAL_MIN = 8 * 60; // 480 min

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

    // ── Body: time column + 7 day columns ──
    html += '<div class="calendar-body">';

    // Time column labels
    html += '<div class="time-column">';
    for (let h = 4; h <= 12; h++) {
        html += `<div class="time-label">${String(h).padStart(2,'0')}:00</div>`;
    }
    html += '</div>';

    // Day columns
    days.forEach((day, i) => {
        const dateStr  = formatDate(day);
        const isPast   = day < today;
        const dayTrips = trips
            .filter(t => t.date === dateStr)
            .sort((a, b) => timeToMinutes(a.startTime) - timeToMinutes(b.startTime));

        html += `<div class="cal-day-column ${isPast ? 'past-col' : ''}" data-date="${dateStr}">`;

        // Hour grid lines
        for (let h = 4; h <= 12; h++) {
            const topPct = ((h * 60 - CAL_START_MIN) / CAL_TOTAL_MIN) * 100;
            html += `<div class="hour-line" style="top:${topPct}%"></div>`;
        }

        // Trip slots
        dayTrips.forEach(trip => {
            const startMin = timeToMinutes(trip.startTime);
            const endMin   = timeToMinutes(trip.endTime);

            // Skip if completely outside 04:00–12:00 window
            if (endMin <= CAL_START_MIN || startMin >= CAL_START_MIN + CAL_TOTAL_MIN) return;

            const clampedStart = Math.max(startMin, CAL_START_MIN);
            const clampedEnd   = Math.min(endMin, CAL_START_MIN + CAL_TOTAL_MIN);

            const topPct    = ((clampedStart - CAL_START_MIN) / CAL_TOTAL_MIN) * 100;
            const heightPct = Math.max(((clampedEnd - clampedStart) / CAL_TOTAL_MIN) * 100, 4);

            const isAvailable = trip.status === 'AVAILABLE' && !isPast;
            const statusClass = isAvailable ? 'slot-available'
                              : trip.status === 'BOOKED' ? 'slot-booked'
                              : 'slot-blocked';

            const clickAttr = isAvailable
                ? `onclick="openBookingModal('${trip.id}','${escAttr(trip.label)}','${trip.startTime}','${trip.endTime}','${trip.date}','${trip.fee ?? ''}')" role="button" tabindex="0"`
                : '';

            const feeHtml = trip.fee
                ? `<span class="slot-fee">R${parseFloat(trip.fee).toFixed(2)}</span>`
                : '';
            const bookedTag = trip.status === 'BOOKED'
                ? '<span class="slot-status-tag">Booked</span>'
                : '';

            html += `
                <div class="trip-slot ${statusClass}"
                     style="top:${topPct}%;height:${heightPct}%;min-height:38px;"
                     ${clickAttr}
                     title="${escAttr(trip.label)}">
                    <span class="slot-time">${trip.startTime} – ${trip.endTime}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                    ${feeHtml}
                    ${bookedTag}
                </div>`;
        });

        // "Next available" hint — shown when there are booked slots on a non-past day
        if (!isPast && dayTrips.length > 0) {
            const lastBooked = dayTrips
                .filter(t => t.status === 'BOOKED')
                .sort((a, b) => timeToMinutes(b.endTime) - timeToMinutes(a.endTime))[0];

            const nextAvail = dayTrips.find(t =>
                t.status === 'AVAILABLE' &&
                lastBooked &&
                timeToMinutes(t.startTime) >= timeToMinutes(lastBooked.endTime)
            );

            if (lastBooked && nextAvail) {
                const topPct = ((timeToMinutes(nextAvail.startTime) - CAL_START_MIN) / CAL_TOTAL_MIN) * 100;
                html += `
                    <div class="next-available-hint" style="top:calc(${topPct}% - 18px)">
                        ↓ Next available: ${nextAvail.startTime}
                    </div>`;
            }
        }

        html += '</div>'; // end cal-day-column
    });

    html += '</div>'; // end calendar-body
    html += '</div>'; // end calendar-grid

    container.innerHTML = html;
}

// ─── Navigation Button State ─────────────────────────────────────────────────
function updateNavButtons() {
    const prevBtn = document.getElementById('prevWeekBtn');
    const nextBtn = document.getElementById('nextWeekBtn');
    if (!prevBtn || !nextBtn) return;

    const todayMonday = getMondayOf(new Date());
    const maxMonday   = getMondayOf(new Date(new Date().getFullYear(), 11, 31)); // last week of year

    if (currentWeekStart <= todayMonday) {
        prevBtn.classList.add('btn-disabled');
        prevBtn.setAttribute('aria-disabled','true');
    } else {
        prevBtn.classList.remove('btn-disabled');
        prevBtn.removeAttribute('aria-disabled');
    }

    if (currentWeekStart >= maxMonday) {
        nextBtn.classList.add('btn-disabled');
        nextBtn.setAttribute('aria-disabled','true');
    } else {
        nextBtn.classList.remove('btn-disabled');
        nextBtn.removeAttribute('aria-disabled');
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
    if (!modal) return;

    const displayDate = new Date(date + 'T00:00:00')
        .toLocaleDateString('en-ZA', { weekday:'long', day:'numeric', month:'long', year:'numeric' });

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
        ${fee ? `<div class="booking-detail-row">
            <span class="detail-label">Fee</span>
            <span class="detail-value fee-highlight">R${parseFloat(fee).toFixed(2)}</span>
        </div>` : '<div class="booking-detail-row"><span class="detail-label">Fee</span><span class="detail-value">To be confirmed</span></div>'}
    `;

    if (input) input.value = tripId;

    modal.classList.remove('hidden');
    backdrop.classList.remove('hidden');
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
    const [h, m] = t.split(':').map(Number);
    return h * 60 + (m || 0);
}

function formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2,'0');
    const d = String(date.getDate()).padStart(2,'0');
    return `${y}-${m}-${d}`;
}

function parseLocalDate(str) {
    // "2026-03-16" → local midnight Date
    if (!str) return new Date();
    const [y, m, d] = str.split('-').map(Number);
    return new Date(y, m - 1, d);
}

function getMondayOf(date) {
    const d = new Date(date);
    d.setHours(0,0,0,0);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    return d;
}

function escHtml(str) {
    return String(str)
        .replace(/&/g,'&amp;').replace(/</g,'&lt;')
        .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function escAttr(str) {
    return String(str || '').replace(/'/g,'&#39;').replace(/"/g,'&quot;');
}
