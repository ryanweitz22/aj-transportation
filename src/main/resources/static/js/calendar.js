/**
 * calendar.js — AJ Transportation
 * Phase 6: Reads real trip data from TRIPS_DATA (injected by Thymeleaf in bookings.html)
 *
 * Replaces the old SAMPLE_TRIPS dummy data entirely.
 * Renders a 7-day weekly calendar with time slots from 04:00–12:00.
 */

document.addEventListener('DOMContentLoaded', function () {

    // TRIPS_DATA is injected by Thymeleaf in bookings.html as a JSON string
    // It contains real Trip objects from the database
    let trips = [];
    try {
        trips = JSON.parse(TRIPS_DATA);
    } catch (e) {
        console.error('Failed to parse TRIPS_DATA:', e);
        trips = [];
    }

    const container = document.getElementById('calendar-container');
    if (!container) return;

    // Build the 7-day header (Mon–Sun) from WEEK_START
    const weekStart = new Date(WEEK_START + 'T00:00:00'); // force local time
    const dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

    // Build day columns
    const days = [];
    for (let i = 0; i < 7; i++) {
        const d = new Date(weekStart);
        d.setDate(weekStart.getDate() + i);
        days.push(d);
    }

    // Build the calendar HTML
    let html = '<div class="calendar-grid">';

    // Header row — day names + dates
    html += '<div class="calendar-header-row">';
    html += '<div class="time-gutter"></div>'; // empty top-left corner
    days.forEach(day => {
        const isToday = isSameDay(day, new Date());
        html += `
            <div class="cal-day-header ${isToday ? 'today' : ''}">
                <span class="day-name">${dayNames[day.getDay() === 0 ? 6 : day.getDay() - 1]}</span>
                <span class="day-date">${day.getDate()} ${monthNames[day.getMonth()]}</span>
            </div>`;
    });
    html += '</div>'; // end header row

    // Time slots — 04:00 to 12:00, one row per hour
    html += '<div class="calendar-body">';
    html += '<div class="time-column">';
    for (let hour = 4; hour <= 12; hour++) {
        html += `<div class="time-label">${String(hour).padStart(2,'0')}:00</div>`;
    }
    html += '</div>'; // end time column

    // Day columns — one per day
    days.forEach(day => {
        const dateStr = formatDate(day); // 'yyyy-MM-dd'
        const dayTrips = trips.filter(t => {
        const tripDate = Array.isArray(t.date)
            ? `${t.date[0]}-${String(t.date[1]).padStart(2,'0')}-${String(t.date[2]).padStart(2,'0')}`
            : t.date;
        return tripDate === dateStr;
    });

        html += `<div class="cal-day-column" data-date="${dateStr}">`;

        // Show each trip slot as a card in the correct position
        dayTrips.forEach(trip => {
            const startTimeStr = Array.isArray(trip.startTime)
                ? `${String(trip.startTime[0]).padStart(2,'0')}:${String(trip.startTime[1]).padStart(2,'0')}`
                : trip.startTime;
            const endTimeStr = Array.isArray(trip.endTime)
                ? `${String(trip.endTime[0]).padStart(2,'0')}:${String(trip.endTime[1]).padStart(2,'0')}`
                : trip.endTime;
            const startMinutes = timeToMinutes(startTimeStr);
            const endMinutes = timeToMinutes(endTimeStr);
            const calendarStartMinutes = 4 * 60; // 04:00 in minutes

            // Calculate position and height within the 04:00–12:00 window (480 min total)
            const topPercent = ((startMinutes - calendarStartMinutes) / 480) * 100;
            const heightPercent = ((endMinutes - startMinutes) / 480) * 100;

            const statusClass = trip.status === 'AVAILABLE' ? 'slot-available' :
                                trip.status === 'BOOKED'    ? 'slot-booked'    : 'slot-blocked';

            const isClickable = trip.status === 'AVAILABLE';

            html += `
                <div class="trip-slot ${statusClass}"
                     style="top: ${topPercent}%; height: ${heightPercent}%; min-height: 40px;"
                     ${isClickable ? `onclick="openBookingModal('${trip.id}', '${trip.label}', '${Array.isArray(trip.startTime) ? String(trip.startTime[0]).padStart(2,'0')+':'+String(trip.startTime[1]).padStart(2,'0') : trip.startTime}', '${Array.isArray(trip.endTime) ? String(trip.endTime[0]).padStart(2,'0')+':'+String(trip.endTime[1]).padStart(2,'0') : trip.endTime}', '${Array.isArray(trip.date) ? trip.date[0]+'-'+String(trip.date[1]).padStart(2,'0')+'-'+String(trip.date[2]).padStart(2,'0') : trip.date}', '${trip.fee ?? ''}')"` : ''}
                     title="${trip.label}">
                    <span class="slot-time">${formatTime(startTimeStr)}</span>
                    <span class="slot-label">${trip.label || ''}</span>
                    ${trip.fee ? `<span class="slot-fee">R${parseFloat(trip.fee).toFixed(2)}</span>` : ''}
                    ${trip.status === 'BOOKED' ? '<span class="slot-status-tag">Booked</span>' : ''}
                </div>`;
        });

        // Hour grid lines (background reference lines)
        for (let hour = 4; hour <= 12; hour++) {
            const topPercent = ((hour * 60 - 4 * 60) / 480) * 100;
            html += `<div class="hour-line" style="top: ${topPercent}%"></div>`;
        }

        html += '</div>'; // end cal-day-column
    });

    html += '</div>'; // end calendar-body
    html += '</div>'; // end calendar-grid

    container.innerHTML = html;
});

// ─── Booking Modal ────────────────────────────────────────────────────────────

function openBookingModal(tripId, label, startTime, endTime, date, fee) {
    const modal = document.getElementById('booking-modal');
    const backdrop = document.getElementById('modal-backdrop');
    const details = document.getElementById('booking-details');
    const tripIdInput = document.getElementById('tripIdInput');

    if (!modal) return;

    // Populate details
    details.innerHTML = `
        <div class="booking-detail-row">
            <span class="detail-label">Route</span>
            <span class="detail-value">${label}</span>
        </div>
        <div class="booking-detail-row">
            <span class="detail-label">Date</span>
            <span class="detail-value">${formatDisplayDate(date)}</span>
        </div>
        <div class="booking-detail-row">
            <span class="detail-label">Time</span>
            <span class="detail-value">${formatTime(startTime)} – ${formatTime(endTime)}</span>
        </div>
        ${fee ? `<div class="booking-detail-row">
            <span class="detail-label">Fee</span>
            <span class="detail-value fee-highlight">R${parseFloat(fee).toFixed(2)}</span>
        </div>` : ''}
    `;

    // Set the hidden trip ID in the form
    if (tripIdInput) tripIdInput.value = tripId;

    modal.classList.remove('hidden');
    backdrop.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
}

function closeBookingModal() {
    const modal = document.getElementById('booking-modal');
    const backdrop = document.getElementById('modal-backdrop');
    if (modal) modal.classList.add('hidden');
    if (backdrop) backdrop.classList.add('hidden');
    document.body.style.overflow = '';
}

// ─── Utility Functions ────────────────────────────────────────────────────────

// Convert 'HH:mm:ss' or 'HH:mm' to total minutes from midnight
function timeToMinutes(timeStr) {
    if (!timeStr) return 0;
    const parts = timeStr.split(':');
    return parseInt(parts[0]) * 60 + parseInt(parts[1]);
}

// Format 'HH:mm:ss' to 'HH:mm'
function formatTime(timeStr) {
    if (!timeStr) return '';
    return timeStr.substring(0, 5);
}

// Format Date object to 'yyyy-MM-dd'
function formatDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

// Format 'yyyy-MM-dd' to a readable display string e.g. 'Monday, 16 March 2026'
function formatDisplayDate(dateStr) {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('en-ZA', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
}

// Check if two Date objects are the same calendar day
function isSameDay(a, b) {
    return a.getFullYear() === b.getFullYear() &&
           a.getMonth() === b.getMonth() &&
           a.getDate() === b.getDate();
}
