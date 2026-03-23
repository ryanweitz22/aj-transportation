/**
 * calendar.js — AJ Transportation
 *
 * Calendar always starts from today and shows 7 days forward.
 * Past days are hidden entirely.
 * Past time slots on today are greyed out relative to current time.
 */

let currentWeekStart = null;

document.addEventListener('DOMContentLoaded', function () {
    let trips = [];
    let businessHours = {};
    try { trips = JSON.parse(TRIPS_DATA); }                  catch (e) { trips = []; }
    try { businessHours = JSON.parse(BUSINESS_HOURS_DATA); } catch (e) { businessHours = {}; }

    if (typeof WEEK_START !== 'undefined' && WEEK_START) {
        currentWeekStart = parseLocalDate(WEEK_START);
    } else {
        currentWeekStart = new Date();
        currentWeekStart.setHours(0,0,0,0);
    }

    renderCalendar(trips, businessHours, currentWeekStart);
    updateNavButtons();

    const monthSel = document.getElementById('monthSelect');
    if (monthSel) {
        const MONTHS = ['January','February','March','April','May','June',
                        'July','August','September','October','November','December'];
        const today = new Date();
        for (let i = 0; i < 12; i++) {
            const d = new Date(today.getFullYear(), today.getMonth() + i, 1);
            const opt = document.createElement('option');
            // Jump to that calendar date directly, not Monday of that week
            opt.value = formatDate(d);
            opt.textContent = MONTHS[d.getMonth()] + ' ' + d.getFullYear();
            if (i === 0) opt.selected = true;
            monthSel.appendChild(opt);
        }
    }
});

function getPxHour() {
    const w = window.innerWidth;
    if (w >= 1024) return 56;
    if (w >= 640)  return 48;
    return 40;
}

function renderCalendar(trips, businessHours, weekStart) {
    const container = document.getElementById('calendar-container');
    if (!container) return;

    const now        = new Date();
    const today      = new Date(); today.setHours(0,0,0,0);
    const DAY_NAMES  = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
    const MONTH_NAMES= ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

    // Build exactly 7 days starting from weekStart
    // Skip any days before today
    const days = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(weekStart);
        d.setDate(weekStart.getDate() + i);
        // Only include today and future days
        if (d >= today) days.push(d);
    }

    if (days.length === 0) {
        container.innerHTML = '<p style="padding:24px;color:var(--text-muted);">No upcoming days in this range.</p>';
        return;
    }

    const CAL_START   = 4 * 60;
    const CAL_END     = 23 * 60;
    const TOTAL_HOURS = 19;
    const PX_HOUR     = getPxHour();
    const TOTAL_H     = PX_HOUR * TOTAL_HOURS;
    const SLOT_MIN    = 30;
    const SLOT_H      = Math.max((SLOT_MIN / 60) * PX_HOUR - 3, 24);

    // Current time in minutes for greying out past slots on today
    const nowMinutes  = now.getHours() * 60 + now.getMinutes();

    let styleTag = document.getElementById('cal-dynamic-style');
    if (!styleTag) {
        styleTag = document.createElement('style');
        styleTag.id = 'cal-dynamic-style';
        document.head.appendChild(styleTag);
    }
    styleTag.textContent = `
        #calendar-container            { min-height: unset !important; overflow: visible !important; }
        .calendar-grid                 { overflow: visible !important; }
        .calendar-body                 { overflow: visible !important; }
        .cal-day-column                { overflow: visible !important; }
        .time-label { height: ${PX_HOUR}px !important; font-size: 0.72rem !important; }
        .calendar-header-row { position: sticky; top: var(--nav-h, 64px); z-index: 10;
                               background: var(--bg); border-bottom: 2px solid var(--border); }
        .trip-slot { font-size: 0.78rem; padding: 5px 7px; }
        .slot-past-today {
            background: #f3f4f6 !important;
            border: 1px solid #e5e7eb !important;
            color: #9ca3af !important;
            cursor: not-allowed !important;
            pointer-events: none !important;
        }
        @media (max-width: 640px) {
            .cal-day-header            { padding: 8px 3px; }
            .cal-day-header .day-name  { font-size: 0.6rem; }
            .cal-day-header .day-date  { font-size: 0.75rem; }
            .cal-day-header .day-month { display: none; }
            .calendar-container        { margin: 0 6px !important; }
            .week-nav                  { padding: 0 6px !important; }
            .calendar-legend           { padding: 0 6px !important; }
            .time-label                { font-size: 0.6rem !important; padding-right: 4px !important; }
        }
    `;

    let html = '<div class="calendar-grid">';

    // ── Header row ────────────────────────────────────────────────────────────
    html += '<div class="calendar-header-row"><div class="time-gutter"></div>';
    days.forEach(day => {
        const isToday = day.getTime() === today.getTime();
        html += `<div class="cal-day-header ${isToday ? 'today' : ''}">
            <span class="day-name">${DAY_NAMES[day.getDay()]}</span>
            <span class="day-date">${day.getDate()} <span class="day-month">${MONTH_NAMES[day.getMonth()]}</span></span>
        </div>`;
    });
    html += '</div>';

    html += `<div class="calendar-body" style="height:${TOTAL_H}px; position:relative;">`;

    // ── Time column ───────────────────────────────────────────────────────────
    html += '<div class="time-column">';
    for (let h = 4; h <= 23; h++) {
        html += `<div class="time-label" style="height:${PX_HOUR}px;">${String(h).padStart(2,'0')}:00</div>`;
    }
    html += '</div>';

    // ── Day columns ───────────────────────────────────────────────────────────
    days.forEach(day => {
        const dateStr   = formatDate(day);
        const isToday   = day.getTime() === today.getTime();
        const dayTrips  = trips
            .filter(t => t.date === dateStr)
            .sort((a, b) => timeToMin(a.startTime) - timeToMin(b.startTime));

        const bh      = businessHours[dateStr] || {};
        const bhOpen  = bh.open  ? timeToMin(bh.open)  : null;
        const bhClose = bh.close ? timeToMin(bh.close) : null;
        const isOpen  = bhOpen !== null && bhClose !== null;

        html += `<div class="cal-day-column" style="height:${TOTAL_H}px;">`;

        for (let h = 0; h <= TOTAL_HOURS; h++) {
            html += `<div class="hour-line" style="top:${h * PX_HOUR}px;"></div>`;
        }

        // ── Ghost / open-hours slots ──────────────────────────────────────────
        if (isOpen) {
            let t = bhOpen;
            while (t < bhClose) {
                if (t >= CAL_START && t < CAL_END) {

                    // On today, grey out slots that are in the past
                    const isPastTime = isToday && t < nowMinutes;

                    const covered = dayTrips.some(trip => {
                        const s = timeToMin(trip.startTime);
                        const e = trip.endTime
                            ? timeToMin(trip.endTime)
                            : (trip.status === 'AVAILABLE' ? s + 60 : bhClose);
                        return t >= s && t < e;
                    });

                    if (!covered) {
                        const topPx   = ((t - CAL_START) / 60) * PX_HOUR;
                        const timeStr = minToTime(t);

                        if (isPastTime) {
                            // Grey out past slots on today — not clickable
                            html += `<div class="trip-slot slot-past-today"
                                style="top:${topPx}px; height:${SLOT_H}px;">
                                <span class="slot-time">${timeStr}</span>
                            </div>`;
                        } else {
                            const slotData = JSON.stringify({ date: dateStr, time: timeStr }).replace(/'/g, '&#39;');
                            html += `<div class="trip-slot slot-open-hours"
                                style="top:${topPx}px; height:${SLOT_H}px;"
                                data-open='${slotData}'
                                onclick="handleOpenSlotClick(this)"
                                role="button" tabindex="0"
                                title="Book a trip at ${timeStr}">
                                <span class="slot-time">${timeStr}</span>
                                <span class="slot-cta">Book →</span>
                            </div>`;
                        }
                    }
                }
                t += SLOT_MIN;
            }
        }

        // ── Admin-created trips ───────────────────────────────────────────────
        dayTrips.forEach(trip => {
            const s = timeToMin(trip.startTime);
            const e = trip.endTime ? timeToMin(trip.endTime) : s + 60;
            if (e <= CAL_START || s >= CAL_END) return;

            const cs    = Math.max(s, CAL_START);
            const ce    = Math.min(e, CAL_END);
            const topPx = ((cs - CAL_START) / 60) * PX_HOUR;
            const hPx   = Math.max(((ce - cs) / 60) * PX_HOUR - 3, 28);

            // On today, grey out trips that are entirely in the past
            const isPastTime = isToday && e <= nowMinutes;

            const isAvailable = trip.status === 'AVAILABLE' && !isPastTime;
            const isPending   = trip.status === 'PENDING';
            const isBooked    = trip.status === 'BOOKED';
            const isBlocked   = trip.status === 'BLOCKED';
            const fee         = trip.fee ? `R${parseFloat(trip.fee).toFixed(2)}` : '';
            const endLbl      = trip.endTime ? ` – ${trip.endTime}` : '';

            if (isPastTime) {
                html += `<div class="trip-slot slot-past-today"
                    style="top:${topPx}px; height:${hPx}px;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                </div>`;
            } else if (isAvailable) {
                const data = JSON.stringify({
                    id: trip.id, label: trip.label || '', startTime: trip.startTime,
                    endTime: trip.endTime || '', date: trip.date, fee: trip.fee || ''
                }).replace(/'/g, '&#39;');
                html += `<div class="trip-slot slot-available"
                    style="top:${topPx}px; height:${hPx}px;"
                    data-trip='${data}'
                    onclick="handleSlotClick(this)"
                    role="button" tabindex="0">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                    <span class="slot-fee">${fee}</span>
                    <span class="slot-cta">Tap to book →</span>
                </div>`;
            } else if (isPending) {
                html += `<div class="trip-slot slot-pending"
                    style="top:${topPx}px; height:${hPx}px; cursor:default;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                    <span class="slot-status-tag">Awaiting Approval</span>
                </div>`;
            } else if (isBooked) {
                html += `<div class="trip-slot slot-booked"
                    style="top:${topPx}px; height:${hPx}px;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                    <span class="slot-status-tag">Booked</span>
                </div>`;
            } else if (isBlocked) {
                html += `<div class="trip-slot slot-blocked"
                    style="top:${topPx}px; height:${hPx}px; cursor:not-allowed;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-status-tag">Unavailable</span>
                </div>`;
            }
        });

        html += '</div>';
    });

    html += '</div></div>';
    container.innerHTML = html;

    container.querySelectorAll('.trip-slot[role="button"]').forEach(el => {
        el.addEventListener('keydown', e => {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); el.click(); }
        });
    });
}

let _resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(_resizeTimer);
    _resizeTimer = setTimeout(() => {
        let trips = [];
        let businessHours = {};
        try { trips = JSON.parse(TRIPS_DATA); }                  catch (e) { trips = []; }
        try { businessHours = JSON.parse(BUSINESS_HOURS_DATA); } catch (e) { businessHours = {}; }
        if (currentWeekStart) renderCalendar(trips, businessHours, currentWeekStart);
    }, 150);
});

function handleSlotClick(el) {
    try {
        const t = JSON.parse(el.getAttribute('data-trip'));
        openBookingModal(t.id, t.label, t.startTime, t.endTime, t.date, t.fee);
    } catch (e) { console.error(e); }
}

function handleOpenSlotClick(el) {
    try {
        const s = JSON.parse(el.getAttribute('data-open'));
        const slotDateEl = document.getElementById('book-slot-date');
        const slotTimeEl = document.getElementById('book-slot-time');
        if (slotDateEl) slotDateEl.value = s.date;
        if (slotTimeEl) slotTimeEl.value = s.time;
        openBookingModal(null, null, s.time, null, s.date, null);
    } catch (e) { console.error(e); }
}

function openBookingModal(tripId, label, startTime, endTime, date, fee) {
    const modal   = document.getElementById('booking-modal');
    const details = document.getElementById('booking-details');
    const input   = document.getElementById('tripIdInput');
    if (!modal || !details) return;

    const displayDate = new Date(date + 'T00:00:00').toLocaleDateString('en-ZA',
        { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    const timeLbl = endTime ? `${startTime} – ${endTime}` : startTime;
    const feeLine = fee && fee !== ''
        ? `<div class="booking-detail-row"><span class="detail-label">Fare</span><span class="detail-value fee-highlight">R${parseFloat(fee).toFixed(2)}</span></div>`
        : `<div class="booking-detail-row"><span class="detail-label">Fare</span><span class="detail-value" style="color:var(--text-muted);">To be confirmed by driver</span></div>`;

    details.innerHTML = `
        <div class="booking-detail-row"><span class="detail-label">Date</span><span class="detail-value">${displayDate}</span></div>
        <div class="booking-detail-row"><span class="detail-label">Time</span><span class="detail-value">${timeLbl}</span></div>
        ${feeLine}`;

    if (input) input.value = tripId || '';
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
    document.body.style.overflow = '';
    resetBookingModal();
}

function updateNavButtons() {
    const prev = document.getElementById('prevWeekBtn');
    if (!prev) return;
    // Prev button disabled when we're already on today's window
    const todayStr = formatDate(new Date());
    const startStr = formatDate(currentWeekStart);
    if (startStr <= todayStr) {
        prev.classList.add('btn-disabled');
        prev.setAttribute('aria-disabled', 'true');
    } else {
        prev.classList.remove('btn-disabled');
        prev.removeAttribute('aria-disabled');
    }
}

function goToPrevWeek() {
    const p = new Date(currentWeekStart);
    p.setDate(p.getDate() - 7);
    navigateToWeek(p);
}

function goToNextWeek() {
    const n = new Date(currentWeekStart);
    n.setDate(n.getDate() + 7);
    navigateToWeek(n);
}

function navigateToWeek(date) {
    window.location.href = '/bookings?week=' + formatDate(date);
}

function jumpToMonth(weekStr) {
    if (weekStr) window.location.href = '/bookings?week=' + weekStr;
}

function timeToMin(t)      { if (!t) return 0; const [h, m] = String(t).split(':').map(Number); return h * 60 + (m || 0); }
function minToTime(m)      { return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`; }
function formatDate(d)     { return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
function parseLocalDate(s) { if (!s) return new Date(); const [y, mo, d] = String(s).split('-').map(Number); return new Date(y, mo - 1, d); }
function escHtml(s)        { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }