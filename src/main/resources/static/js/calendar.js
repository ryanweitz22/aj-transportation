/**
 * calendar.js — AJ Transportation
 * Teal/ghost = open business hours slot → click to book
 * Green      = AVAILABLE trip created by admin → click to book
 * Amber      = PENDING trip → awaiting admin approval, not bookable
 * Red        = BOOKED trip → taken
 * Grey       = past / blocked / closed
 */

let currentWeekStart = null;

document.addEventListener('DOMContentLoaded', function () {
    let trips = [];
    let businessHours = {};
    try { trips = JSON.parse(TRIPS_DATA); }                    catch (e) { trips = []; }
    try { businessHours = JSON.parse(BUSINESS_HOURS_DATA); }   catch (e) { businessHours = {}; }

    trips = trips.map(normaliseTrip);
    currentWeekStart = parseLocalDate(WEEK_START);

    buildMonthDropdown();
    renderCalendar(trips, businessHours, currentWeekStart);
    updateNavButtons();

    ['booking-modal'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('click', e => { if (e.target === el) closeAllModals(); });
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeAllModals(); });
});

// ─── Normalise ────────────────────────────────────────────────────────────────
function normaliseTrip(t) {
    return { ...t, date: normaliseDate(t.date), startTime: normaliseTime(t.startTime), endTime: normaliseTime(t.endTime) };
}
function normaliseDate(d) {
    if (!d) return '';
    if (Array.isArray(d)) return `${d[0]}-${String(d[1]).padStart(2,'0')}-${String(d[2]).padStart(2,'0')}`;
    return String(d);
}
function normaliseTime(t) {
    if (!t) return '';
    if (Array.isArray(t)) return `${String(t[0]).padStart(2,'0')}:${String(t[1]).padStart(2,'0')}`;
    return String(t).substring(0, 5);
}

// ─── Month dropdown ───────────────────────────────────────────────────────────
function buildMonthDropdown() {
    const selects = document.querySelectorAll('#monthSelect');
    if (!selects.length) return;
    const now = new Date();
    const yr  = now.getFullYear();
    const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];
    selects.forEach(sel => {
        sel.innerHTML = '';
        for (let m = 0; m < 12; m++) {
            const o = document.createElement('option');
            o.value = `${yr}-${String(m+1).padStart(2,'0')}-01`;
            o.textContent = `${MONTHS[m]} ${yr}`;
            if (m === currentWeekStart.getMonth() && yr === currentWeekStart.getFullYear()) o.selected = true;
            sel.appendChild(o);
        }
    });
}
function jumpToMonth(v) { if (v) navigateToWeek(getMondayOf(parseLocalDate(v))); }
function navigateToWeek(d) { window.location.href = `/bookings?week=${formatDate(d)}`; }

// ─── Render ───────────────────────────────────────────────────────────────────
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

    const CAL_START = 4 * 60;
    const PX_HOUR   = 60;
    const TOTAL_H   = PX_HOUR * 8;

    let html = '<div class="calendar-grid">';

    // Header
    html += '<div class="calendar-header-row"><div class="time-gutter"></div>';
    days.forEach((day, i) => {
        const isToday = day.getTime() === today.getTime();
        const isPast  = day < today;
        html += `<div class="cal-day-header ${isToday?'today':''} ${isPast&&!isToday?'past-day':''}">
            <span class="day-name">${DAY_NAMES[i]}</span>
            <span class="day-date">${day.getDate()} ${MONTH_NAMES[day.getMonth()]}</span>
        </div>`;
    });
    html += '</div>';

    // Body
    html += `<div class="calendar-body" style="height:${TOTAL_H}px;">`;

    // Time column
    html += '<div class="time-column">';
    for (let h = 4; h <= 12; h++) {
        html += `<div class="time-label" style="height:${PX_HOUR}px;">${String(h).padStart(2,'0')}:00</div>`;
    }
    html += '</div>';

    // Day columns
    days.forEach(day => {
        const dateStr  = formatDate(day);
        const isPast   = day < today;
        const dayTrips = trips
            .filter(t => t.date === dateStr)
            .sort((a,b) => timeToMin(a.startTime) - timeToMin(b.startTime));

        const bh      = businessHours[dateStr] || {};
        const bhOpen  = bh.open  ? timeToMin(bh.open)  : null;
        const bhClose = bh.close ? timeToMin(bh.close) : null;
        const isOpen  = bhOpen !== null && bhClose !== null;

        html += `<div class="cal-day-column ${isPast?'past-col':''}" style="height:${TOTAL_H}px;">`;

        // Hour lines
        for (let h = 0; h <= 8; h++) {
            html += `<div class="hour-line" style="top:${h*PX_HOUR}px;"></div>`;
        }

        // ── Open business hours slots (teal ghost) ────────────────────────────
        if (!isPast && isOpen) {
            let t = bhOpen;
            while (t < bhClose) {
                if (t >= CAL_START && t < CAL_START + 8*60) {
                    const covered = dayTrips.some(trip => {
                        const s = timeToMin(trip.startTime);
                        const e = trip.endTime ? timeToMin(trip.endTime) : s + 60;
                        return t >= s && t < e;
                    });
                    if (!covered) {
                        const topPx  = ((t - CAL_START) / 60) * PX_HOUR;
                        const hPx    = Math.max((30/60)*PX_HOUR - 2, 22);
                        const timeStr = minToTime(t);
                        const slotData = JSON.stringify({ date: dateStr, time: timeStr }).replace(/'/g,'&#39;');
                        html += `<div class="trip-slot slot-open-hours"
                            style="top:${topPx}px;height:${hPx}px;"
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

        // ── Existing trips ────────────────────────────────────────────────────
        dayTrips.forEach(trip => {
            const s = timeToMin(trip.startTime);
            const e = trip.endTime ? timeToMin(trip.endTime) : s + 60;
            if (e <= CAL_START || s >= CAL_START + 8*60) return;

            const cs   = Math.max(s, CAL_START);
            const ce   = Math.min(e, CAL_START + 8*60);
            const topPx = ((cs - CAL_START)/60)*PX_HOUR;
            const hPx   = Math.max(((ce-cs)/60)*PX_HOUR, 36);

            const isAvailable = trip.status === 'AVAILABLE' && !isPast;
            const isPending   = trip.status === 'PENDING';
            const isBooked    = trip.status === 'BOOKED';
            const fee         = trip.fee ? `R${parseFloat(trip.fee).toFixed(2)}` : '';
            const endLbl      = trip.endTime ? ` – ${trip.endTime}` : '';

            if (isAvailable) {
                const data = JSON.stringify({
                    id:trip.id, label:trip.label||'', startTime:trip.startTime,
                    endTime:trip.endTime||'', date:trip.date, fee:trip.fee||''
                }).replace(/'/g,'&#39;');
                html += `<div class="trip-slot slot-available"
                    style="top:${topPx}px;height:${hPx}px;"
                    data-trip='${data}'
                    onclick="handleSlotClick(this)"
                    role="button" tabindex="0">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label||'')}</span>
                    <span class="slot-fee">${fee}</span>
                    <span class="slot-cta">Tap to book →</span>
                </div>`;
            } else if (isPending) {
                html += `<div class="trip-slot slot-pending"
                    style="top:${topPx}px;height:${hPx}px;cursor:default;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label||'')}</span>
                    <span class="slot-status-tag">Awaiting Approval</span>
                </div>`;
            } else if (isBooked) {
                html += `<div class="trip-slot slot-booked"
                    style="top:${topPx}px;height:${hPx}px;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label||'')}</span>
                    <span class="slot-status-tag">Booked</span>
                </div>`;
            } else {
                html += `<div class="trip-slot slot-blocked"
                    style="top:${topPx}px;height:${hPx}px;">
                    <span class="slot-time">${trip.startTime}${endLbl}</span>
                    <span class="slot-label">${escHtml(trip.label||'')}</span>
                </div>`;
            }
        });

        html += '</div>';
    });

    html += '</div></div>';
    container.innerHTML = html;

    container.querySelectorAll('.trip-slot[role="button"]').forEach(el => {
        el.addEventListener('keydown', e => {
            if (e.key==='Enter'||e.key===' ') { e.preventDefault(); el.click(); }
        });
    });
}

// ─── Click handlers ───────────────────────────────────────────────────────────
function handleSlotClick(el) {
    try {
        const t = JSON.parse(el.getAttribute('data-trip'));
        openBookingModal(t.id, t.label, t.startTime, t.endTime, t.date, t.fee);
    } catch(e) { console.error(e); }
}

function handleOpenSlotClick(el) {
    try {
        const s = JSON.parse(el.getAttribute('data-open'));
        // Store date + time for open slot form submission
        const slotDateEl = document.getElementById('book-slot-date');
        const slotTimeEl = document.getElementById('book-slot-time');
        if (slotDateEl) slotDateEl.value = s.date;
        if (slotTimeEl) slotTimeEl.value = s.time;
        openBookingModal(null, null, s.time, null, s.date, null);
    } catch(e) { console.error(e); }
}
// ─── Booking modal ────────────────────────────────────────────────────────────
function openBookingModal(tripId, label, startTime, endTime, date, fee) {
    const modal   = document.getElementById('booking-modal');
    const details = document.getElementById('booking-details');
    const input   = document.getElementById('tripIdInput');
    if (!modal || !details) return;

    const displayDate = new Date(date+'T00:00:00').toLocaleDateString('en-ZA',
        {weekday:'long',day:'numeric',month:'long',year:'numeric'});
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

// ─── Close ────────────────────────────────────────────────────────────────────
function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
    document.body.style.overflow = '';
    resetBookingModal();
}

// ─── Navigation ───────────────────────────────────────────────────────────────
function updateNavButtons() {
    const prev = document.getElementById('prevWeekBtn');
    const next = document.getElementById('nextWeekBtn');
    if (!prev||!next) return;
    const todayMon = getMondayOf(new Date());
    if (currentWeekStart <= todayMon) { prev.classList.add('btn-disabled'); prev.setAttribute('aria-disabled','true'); }
    else { prev.classList.remove('btn-disabled'); prev.removeAttribute('aria-disabled'); }
}
function goToPrevWeek() { const p=new Date(currentWeekStart); p.setDate(p.getDate()-7); navigateToWeek(p); }
function goToNextWeek() { const n=new Date(currentWeekStart); n.setDate(n.getDate()+7); navigateToWeek(n); }

// ─── Utilities ────────────────────────────────────────────────────────────────
function timeToMin(t) { if(!t)return 0; const[h,m]=String(t).split(':').map(Number); return h*60+(m||0); }
function minToTime(m) { return `${String(Math.floor(m/60)).padStart(2,'0')}:${String(m%60).padStart(2,'0')}`; }
function formatDate(d) { return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; }
function parseLocalDate(s) { if(!s)return new Date(); const[y,m,d]=String(s).split('-').map(Number); return new Date(y,m-1,d); }
function getMondayOf(d) { const x=new Date(d); x.setHours(0,0,0,0); const day=x.getDay(); x.setDate(x.getDate()+(day===0?-6:1-day)); return x; }
function escHtml(s) { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }