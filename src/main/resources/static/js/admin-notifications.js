/**
 * admin-notifications.js — AJ Transportation
 *
 * Polls /bookings/pending-count every 3 seconds.
 * Shows a hard modal lock when pending bookings exist.
 *
 * FIX: Post-action pause increased from 2000ms to 3500ms to prevent the
 * accept loop where the booking briefly re-appeared as PENDING on Supabase
 * before the commit fully landed.
 */

(function () {

    const POLL_INTERVAL_MS  = 3000;
    const POST_ACTION_PAUSE = 3500;

    let pollTimer       = null;
    let currentPopupIds = [];
    let isPaused        = false;

    function injectModal() {
        if (document.getElementById('admin-notif-overlay')) return;

        const overlay = document.createElement('div');
        overlay.id = 'admin-notif-overlay';
        overlay.style.cssText = `
            display: none;
            position: fixed;
            inset: 0;
            z-index: 9999;
            background: rgba(0,0,0,0.65);
            backdrop-filter: blur(6px);
            align-items: center;
            justify-content: center;
            padding: 24px;
            font-family: 'DM Sans', sans-serif;
        `;

        overlay.innerHTML = `
            <div style="
                background: white;
                border-radius: 24px;
                width: 100%;
                max-width: 520px;
                max-height: 85vh;
                overflow-y: auto;
                box-shadow: 0 24px 64px rgba(0,0,0,0.25);
            ">
                <div style="
                    padding: 20px 24px 16px;
                    border-bottom: 1px solid #e5e7eb;
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    position: sticky;
                    top: 0;
                    background: white;
                    z-index: 1;
                    border-radius: 24px 24px 0 0;
                ">
                    <div style="
                        width: 40px; height: 40px;
                        background: #fff8e6;
                        border-radius: 50%;
                        display: flex; align-items: center; justify-content: center;
                        font-size: 1.2rem;
                        flex-shrink: 0;
                        animation: pulse-bell 1.5s ease-in-out infinite;
                    ">🔔</div>
                    <div>
                        <div style="font-family:'Syne',sans-serif;font-weight:700;font-size:1.05rem;color:#0d1117;">
                            New Booking Request
                        </div>
                        <div style="font-size:0.8rem;color:#6b7280;">
                            You must respond before continuing
                        </div>
                    </div>
                </div>
                <div id="admin-notif-list" style="padding: 16px 24px;"></div>
            </div>

            <style>
                @keyframes pulse-bell {
                    0%, 100% { transform: scale(1); }
                    50%       { transform: scale(1.15); }
                }
                .notif-booking-card {
                    background: #f9fafb;
                    border: 1.5px solid #e5e7eb;
                    border-radius: 14px;
                    padding: 16px;
                    margin-bottom: 12px;
                }
                .notif-booking-card:last-child { margin-bottom: 0; }
                .notif-row {
                    display: flex;
                    justify-content: space-between;
                    font-size: 0.85rem;
                    padding: 4px 0;
                    border-bottom: 1px solid #e5e7eb;
                }
                .notif-row:last-of-type { border-bottom: none; }
                .notif-label { color: #6b7280; font-weight: 500; }
                .notif-value { font-weight: 600; color: #0d1117; text-align: right; max-width: 60%; }
                .notif-actions { display: flex; gap: 10px; margin-top: 14px; }
                .notif-btn {
                    flex: 1; padding: 10px; border-radius: 10px; border: none;
                    font-size: 0.875rem; font-weight: 600;
                    font-family: 'DM Sans', sans-serif;
                    cursor: pointer; transition: opacity 0.15s;
                }
                .notif-btn:hover { opacity: 0.85; }
                .notif-btn-accept { background: #0a7c6e; color: white; }
                .notif-btn-reject { background: #fee2e2; color: #dc2626; }
                .notif-btn:disabled { opacity: 0.5; cursor: not-allowed; }
                .notif-processing {
                    font-size: 0.8rem; color: #6b7280;
                    text-align: center; padding: 8px 0;
                    font-style: italic;
                }
            </style>
        `;

        document.body.appendChild(overlay);
    }

    function buildBookingCard(booking) {
        const div = document.createElement('div');
        div.className = 'notif-booking-card';
        div.id = `notif-card-${booking.id}`;

        const date    = new Date(booking.date + 'T00:00:00');
        const dateStr = date.toLocaleDateString('en-ZA', {
            weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
        });
        const timeStr = booking.time.substring(0, 5);

        div.innerHTML = `
            <div class="notif-row">
                <span class="notif-label">User</span>
                <span class="notif-value">${escHtml(booking.userEmail)}</span>
            </div>
            <div class="notif-row">
                <span class="notif-label">Date</span>
                <span class="notif-value">${dateStr} at ${timeStr}</span>
            </div>
            <div class="notif-row">
                <span class="notif-label">Pickup</span>
                <span class="notif-value">${escHtml(booking.pickup)}</span>
            </div>
            <div class="notif-row">
                <span class="notif-label">Dropoff</span>
                <span class="notif-value">${escHtml(booking.dropoff)}</span>
            </div>
            <div class="notif-row">
                <span class="notif-label">Fare</span>
                <span class="notif-value" style="color:#0a7c6e;font-size:1rem;">${escHtml(booking.fare)}</span>
            </div>
            <div class="notif-actions">
                <button class="notif-btn notif-btn-accept"
                        onclick="adminRespond('${booking.id}', 'accept')">✓ Accept</button>
                <button class="notif-btn notif-btn-reject"
                        onclick="adminRespond('${booking.id}', 'reject')">✗ Reject</button>
            </div>
        `;
        return div;
    }

    function showModal(bookings) {
        const overlay = document.getElementById('admin-notif-overlay');
        const list    = document.getElementById('admin-notif-list');
        if (!overlay || !list) return;

        const newIds = bookings.map(b => b.id).sort().join(',');
        const curIds = currentPopupIds.slice().sort().join(',');
        if (newIds === curIds && overlay.style.display === 'flex') return;

        currentPopupIds = bookings.map(b => b.id);
        list.innerHTML  = '';
        bookings.forEach(b => list.appendChild(buildBookingCard(b)));

        overlay.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function hideModal() {
        const overlay = document.getElementById('admin-notif-overlay');
        if (overlay) overlay.style.display = 'none';
        document.body.style.overflow = '';
        currentPopupIds = [];
    }

    window.adminRespond = async function (bookingId, action) {
        const card    = document.getElementById(`notif-card-${bookingId}`);
        const buttons = card ? card.querySelectorAll('.notif-btn') : [];
        buttons.forEach(b => b.disabled = true);

        // Show processing message so admin knows it's working
        if (card) {
            const msg = document.createElement('div');
            msg.className   = 'notif-processing';
            msg.textContent = action === 'accept' ? 'Accepting...' : 'Rejecting...';
            card.appendChild(msg);
        }

        // Stop polling immediately
        clearInterval(pollTimer);
        pollTimer = null;
        isPaused  = true;

        try {
            const res = await fetch(`/admin/bookings/${action}/${bookingId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-CSRF-TOKEN': getCsrfToken()
                }
            });

            if (res.ok || res.redirected) {
                if (card) card.remove();
                currentPopupIds = currentPopupIds.filter(id => id !== bookingId);

                const list = document.getElementById('admin-notif-list');
                if (!list || list.children.length === 0) hideModal();

                // Wait 3500ms before resuming — gives Supabase time to fully
                // commit so the next poll doesn't re-show the same booking
                setTimeout(() => {
                    isPaused  = false;
                    poll();
                    pollTimer = setInterval(poll, POLL_INTERVAL_MS);
                }, POST_ACTION_PAUSE);

            } else {
                buttons.forEach(b => b.disabled = false);
                if (card) {
                    const msg = card.querySelector('.notif-processing');
                    if (msg) msg.remove();
                }
                isPaused  = false;
                pollTimer = setInterval(poll, POLL_INTERVAL_MS);
                alert('Something went wrong. Please try again.');
            }
        } catch (e) {
            buttons.forEach(b => b.disabled = false);
            if (card) {
                const msg = card.querySelector('.notif-processing');
                if (msg) msg.remove();
            }
            isPaused  = false;
            pollTimer = setInterval(poll, POLL_INTERVAL_MS);
            alert('Network error. Please try again.');
        }
    };

    function getCsrfToken() {
        const input = document.querySelector('input[name="_csrf"]');
        if (input) return input.value;
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta) return meta.getAttribute('content');
        return '';
    }

    async function poll() {
        if (isPaused) return;
        try {
            const res  = await fetch('/bookings/pending-count');
            const data = await res.json();
            if (data.count > 0) showModal(data.bookings);
            else hideModal();
        } catch (e) {}
    }

    function escHtml(s) {
        return String(s)
            .replace(/&/g,'&amp;').replace(/</g,'&lt;')
            .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    document.addEventListener('DOMContentLoaded', () => {
        injectModal();
        poll();
        pollTimer = setInterval(poll, POLL_INTERVAL_MS);
    });

})();