/**
 * calendar.js — AJ Transportation
 * Teal/ghost = open business hours slot → click to book
 * Green      = AVAILABLE trip created by admin → click to book
 * Amber      = PENDING trip → awaiting admin approval, not bookable
 * Red        = BOOKED trip → taken
 * Grey       = past / blocked / closed
 *
 * Calendar spans 04:00 → 23:00 (19 hours).
 * PX_HOUR is responsive — shrinks on smaller screens so the full
 * day fits on screen without any sidebar scrolling.
 */

let currentWeekStart = null;

document.addEventListener('DOMContentLoaded', function () {
    let trips = [];
    let businessHours = {};
    try { trips = JSON.parse(TRIPS_DATA); }                    catch (e) { trips = []; }
    try { businessHours = JSON.parse(BUSINESS_HOURS_DATA); }   catch (e) { businessHours = {}; }

    if (typeof WEEK_START !== 'undefined' && WEEK_START) {
        currentWeekStart = parseLocalDate(WEEK_START);
    } else {
        currentWeekStart = getMondayOf(new Date());
    }

    renderCalendar(trips, businessHours, currentWeekStart);
    updateNavButtons();

    // Populate month jump selector
    const monthSel = document.getElementById('monthSelect');
    if (monthSel) {
        const MONTHS = ['January','February','March','April','May','June',
                        'July','August','September','October','November','December'];
        const today = new Date();
        for (let i = 0; i < 12; i++) {
            const d = new Date(today.getFullYear(), today.getMonth() + i, 1);
            const opt = document.createElement('option');
            opt.value = formatDate(getMondayOf(d));
            opt.textContent = MONTHS[d.getMonth()] + ' ' + d.getFullYear();
            if (i === 0) opt.selected = true;
            monthSel.appendChild(opt);
        }
    }
});

// ── Responsive pixel-per-hour ──────────────────────────────────────────────────
// Works out how tall each hour row must be so all 19 hours (04:00–23:00)
// fit on screen with zero scrolling on the calendar body itself.
function getResponsivePxHour() {
    const TOTAL_HOURS = 19;
    const viewportH   = window.innerHeight;
    // Space consumed by navbar, page header, week-nav, legend, info-banner, padding
    const reservedPx  = 284;
    const available   = Math.max(viewportH - reservedPx, 300);
    const computed    = Math.floor(available / TOTAL_HOURS);
    // 28px = minimum on small phones, 64px = maximum on large desktop
    return Math.max(28, Math.min(64, computed));
}

function renderCalendar(trips, businessHours, weekStart) {
    const container = document.getElementById('calendar-container');
    if (!container) return;

    const today = new Date(); today.setHours(0,0,0,0);
    const DAY_NAMES   = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
    const MONTH_NAMES = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

    const days = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(weekStart);
        d.setDate(weekStart.getDate() + i);
        days.push(d);
    }

    const CAL_START   = 4 * 60;   // 04:00 in minutes
    const CAL_END     = 23 * 60;  // 23:00 in minutes
    const TOTAL_HOURS = 19;
    const PX_HOUR     = getResponsivePxHour();
    const TOTAL_H     = PX_HOUR * TOTAL_HOURS;

    // Dynamic style so time-label heights track PX_HOUR exactly
    let styleTag = document.getElementById('cal-dynamic-style');
    if (!styleTag) {
        styleTag = document.createElement('style');
        styleTag.id = 'cal-dynamic-style';
        document.head.appendChild(styleTag);
    }
    styleTag.textContent = `
        #calendar-container { min-height: unset !important; overflow: hidden !important; }
        .calendar-body      { overflow: hidden !important; }
        .cal-day-column     { overflow: hidden !important; }
        .time-label         { height: ${PX_HOUR}px !important;
                              font-size: ${PX_HOUR < 36 ? '0.58rem' : '0.68rem'} !important; }
        .trip-slot          { min-height: ${Math.max(PX_HOUR - 4, 18)}px; }
        @media (max-width: 640px) {
            .cal-day-header .day-month { display: none; }
            .calendar-container { margin: 0 8px !important; }
            .week-nav           { padding: 0 8px !important; }
            .calendar-legend    { padding: 0 8px !important; }
        }
    `;

    let html = '<div class="calendar-grid">';

    // ── Header row ─────────────────────────────────────────────────────────────
    html += '<div class="calendar-header-row"><div class="time-gutter"></div>';
    days.forEach((day, i) => {
        const isToday = day.getTime() === today.getTime();
        const isPast  = day < today;
        html += `<div class="cal-day-header ${isToday ? 'today' : ''} ${isPast && !isToday ? 'past-day' : ''}">
            <span class="day-name">${DAY_NAMES[i]}</span>
            <span class="day-date">${day.getDate()} <span class="day-month">${MONTH_NAMES[day.getMonth()]}</span></span>
        </div>`;
    });
    html += '</div>';

    // ── Body ───────────────────────────────────────────────────────────────────
    html += `<div class="calendar-body" style="height:${TOTAL_H}px;">`;

    // Time gutter 04:00 → 23:00
    html += '<div class="time-column">';
    for (let h = 4; h <= 23; h++) {
        html += `<div class="time-label" style="height:${PX_HOUR}px;">${String(h).padStart(2,'0')}:00</div>`;
    }
    html += '</div>';

    // ── Seven day columns ──────────────────────────────────────────────────────
    days.forEach(day => {
        const dateStr  = formatDate(day);
        const isPast   = day < today;
        const dayTrips = trips
            .filter(t => t.date === dateStr)
            .sort((a, b) => timeToMin(a.startTime) - timeToMin(b.startTime));

        const bh      = businessHours[dateStr] || {};
        const bhOpen  = bh.open  ? timeToMin(bh.open)  : null;
        const bhClose = bh.close ? timeToMin(bh.close) : null;
        const isOpen  = bhOpen !== null && bhClose !== null;

        html += `<div class="cal-day-column ${isPast ? 'past-col' : ''}" style="height:${TOTAL_H}px;">`;

        // Hour divider lines
        for (let h = 0; h <= TOTAL_HOURS; h++) {
            html += `<div class="hour-line" style="top:${h * PX_HOUR}px;"></div>`;
        }

        // ── Open business hours ghost slots (teal dashed) ──────────────────────
        if (!isPast && isOpen) {
            let t = bhOpen;
            while (t < bhClose) {
                if (t >= CAL_START && t < CAL_END) {
                    const covered = dayTrips.some(trip => {
                        const s = timeToMin(trip.startTime);
                        const e = trip.endTime ? timeToMin(trip.endTime) : s + 60;
                        return t >= s && t < e;
                    });
                    if (!covered) {
                        const topPx   = ((t - CAL_START) / 60) * PX_HOUR;
                        const slotH   = Math.max((30 / 60) * PX_HOUR - 2, 18);
                        const timeStr = minToTime(t);
                        const slotData = JSON.stringify({ date: dateStr, time: timeStr }).replace(/'/g, '&#39;');
                        html += `<div class="trip-slot slot-open-hours"
                            style="top:${topPx}px;height:${slotH}px;"
                            data-open='${slotData}'
                            onclick="handleOpenSlotClick(this)"
                            role="button" tabindex="0"
                            title="Request a trip at ${timeStr}">
                            <span class="slot-time">${timeStr}</span>
                            <span class="slot-cta">Book →</span>
                        </div>`;
                    }
                }
                t += 30;
            }
        }

        // ── Admin-created trips ────────────────────────────────────────────────
        dayTrips.forEach(trip => {
            const s = timeToMin(trip.startTime);
            const e = trip.endTime ? timeToMin(trip.endTime) : s + 60;
            if (e <= CAL_START || s >= CAL_END) return;

            const cs    = Math.max(s, CAL_START);
            const ce    = Math.min(e, CAL_END);
            const topPx = ((cs - CAL_START) / 60) * PX_HOUR;
            const hPx   = Math.max(((ce - cs) / 60) * PX_HOUR, Math.max(PX_HOUR - 4, 18));

            const isAvailable = trip.status === 'AVAILABLE' && !isPast;
            const isPending   = trip.status === 'PENDING';
            const isBooked    = trip.status === 'BOOKED';
            const fee         = trip.fee ? `R${parseFloat(trip.fee).toFixed(2)}` : '';
            const endLbl      = trip.endTime ? ` – ${trip.endTime}` : '';

            if (isAvailable) {
                const data = JSON.stringify({
                    id: trip.id, label: trip.label || '', startTime: trip.startTime,
                    endTime: trip.endTime || '', date: trip.date, fee: trip.fee || ''
                }).replace(/'/g, '&#39;');
                html += `<div class="trip-slot slot-available"
                    style="top:${topPx}px;height:${hPx}px;"
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
                    style="top:${topPx}px;height:${hPx}px;cursor:default;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                    <span class="slot-status-tag">Awaiting Approval</span>
                </div>`;
            } else if (isBooked) {
                html += `<div class="trip-slot slot-booked"
                    style="top:${topPx}px;height:${hPx}px;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                    <span class="slot-status-tag">Booked</span>
                </div>`;
            } else {
                html += `<div class="trip-slot slot-blocked"
                    style="top:${topPx}px;height:${hPx}px;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label || '')}</span>
                </div>`;
            }
        });

        html += '</div>';
    });

    html += '</div></div>';
    container.innerHTML = html;

    // Keyboard accessibility
    container.querySelectorAll('.trip-slot[role="button"]').forEach(el => {
        el.addEventListener('keydown', e => {
            if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); el.click(); }
        });
    });
}

// Re-render on resize so rows adapt to the new viewport height
let _resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(_resizeTimer);
    _resizeTimer = setTimeout(() => {
        let trips = [];
        let businessHours = {};
        try { trips = JSON.parse(TRIPS_DATA); }                    catch (e) { trips = []; }
        try { businessHours = JSON.parse(BUSINESS_HOURS_DATA); }   catch (e) { businessHours = {}; }
        if (currentWeekStart) renderCalendar(trips, businessHours, currentWeekStart);
    }, 150);
});

// ─── Click handlers ────────────────────────────────────────────────────────────

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

// ─── Booking modal ─────────────────────────────────────────────────────────────

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

// ─── Close ─────────────────────────────────────────────────────────────────────

function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
    document.body.style.overflow = '';
    resetBookingModal();
}

// ─── Week navigation ───────────────────────────────────────────────────────────

function updateNavButtons() {
    const prev = document.getElementById('prevWeekBtn');
    const next = document.getElementById('nextWeekBtn');
    if (!prev || !next) return;
    const todayMon = getMondayOf(new Date());
    if (currentWeekStart <= todayMon) {
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

function navigateToWeek(monday) {
    window.location.href = '/bookings?week=' + formatDate(monday);
}

function jumpToMonth(weekStr) {
    if (weekStr) window.location.href = '/bookings?week=' + weekStr;
}

// ─── Utilities ─────────────────────────────────────────────────────────────────

function timeToMin(t)      { if (!t) return 0; const [h, m] = String(t).split(':').map(Number); return h * 60 + (m || 0); }
function minToTime(m)      { return `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`; }
function formatDate(d)     { return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
function parseLocalDate(s) { if (!s) return new Date(); const [y, mo, d] = String(s).split('-').map(Number); return new Date(y, mo - 1, d); }
function getMondayOf(d)    { const x = new Date(d); x.setHours(0,0,0,0); const day = x.getDay(); x.setDate(x.getDate() + (day === 0 ? -6 : 1 - day)); return x; }
function escHtml(s)        { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }