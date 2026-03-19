/**
 * bookings.js — AJ Transportation
 * Handles the 2-step Uber-style booking and request modals,
 * including Google Places Autocomplete.
 */

// ── Google Places Autocomplete ────────────────────────────────────────────────
// Called automatically by the Maps API once it finishes loading
function initAutocomplete() {
    const SA_BOUNDS = new google.maps.LatLngBounds(
        new google.maps.LatLng(-35.0, 17.5),
        new google.maps.LatLng(-22.0, 33.0)
    );
    const OPTIONS = {
        bounds: SA_BOUNDS,
        componentRestrictions: { country: 'za' },
        fields: ['formatted_address', 'geometry', 'name'],
        strictBounds: false,
        types: ['geocode', 'establishment']
    };

    attachAutocomplete('book-pickup',  OPTIONS);
    attachAutocomplete('book-dropoff', OPTIONS);
    attachAutocomplete('req-pickup',   OPTIONS);
    attachAutocomplete('req-dropoff',  OPTIONS);
}

function attachAutocomplete(inputId, options) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const ac = new google.maps.places.Autocomplete(input, options);
    ac.addListener('place_changed', () => {
        const place = ac.getPlace();
        if (place && place.formatted_address) {
            input.value = place.formatted_address;
        }
    });
}

// Fallback — if Maps API never loads, inputs still work as plain text
window.initAutocomplete = window.initAutocomplete || function () {};


// ── GREEN SLOT — Step logic ───────────────────────────────────────────────────
function bookStep1Next() {
    const pickup  = document.getElementById('book-pickup').value.trim();
    const dropoff = document.getElementById('book-dropoff').value.trim();

    if (!pickup)  { showInputError('book-pickup',  'Please enter a pickup address.'); return; }
    if (!dropoff) { showInputError('book-dropoff', 'Please enter a dropoff address.'); return; }

    document.getElementById('book-pickup-hidden').value  = pickup;
    document.getElementById('book-dropoff-hidden').value = dropoff;
    document.getElementById('book-confirm-pickup').textContent  = pickup;
    document.getElementById('book-confirm-dropoff').textContent = dropoff;

    setStep('book', 2);
    document.getElementById('booking-modal-title').textContent = 'Confirm Your Booking';
}

function bookGoBack() {
    setStep('book', 1);
    document.getElementById('booking-modal-title').textContent = 'Where are you going?';
}


// ── AMBER SLOT — Step logic ───────────────────────────────────────────────────
function reqStep1Next() {
    const pickup  = document.getElementById('req-pickup').value.trim();
    const dropoff = document.getElementById('req-dropoff').value.trim();

    if (!pickup)  { showInputError('req-pickup',  'Please enter a pickup address.'); return; }
    if (!dropoff) { showInputError('req-dropoff', 'Please enter a dropoff address.'); return; }

    document.getElementById('req-pickup-hidden').value  = pickup;
    document.getElementById('req-dropoff-hidden').value = dropoff;
    document.getElementById('req-confirm-pickup').textContent  = pickup;
    document.getElementById('req-confirm-dropoff').textContent = dropoff;

    setStep('req', 2);
    document.getElementById('request-modal-title').textContent = 'Review Your Request';
}

function reqGoBack() {
    setStep('req', 1);
    document.getElementById('request-modal-title').textContent = 'Where are you going?';
}


// ── Step switcher ─────────────────────────────────────────────────────────────
function setStep(prefix, step) {
    const s1 = document.getElementById(`${prefix}-step-1`);
    const s2 = document.getElementById(`${prefix}-step-2`);
    const d1 = document.getElementById(`${prefix}-dot-1`);
    const d2 = document.getElementById(`${prefix}-dot-2`);
    const l1 = document.getElementById(`${prefix}-line-1`);

    if (step === 2) {
        s1.classList.add('hidden');
        s2.classList.remove('hidden');
        d1.className = 'step-dot done'; d1.textContent = '✓';
        d2.className = 'step-dot active';
        l1.className = 'step-line done';
    } else {
        s2.classList.add('hidden');
        s1.classList.remove('hidden');
        d1.className = 'step-dot active'; d1.textContent = '1';
        d2.className = 'step-dot inactive';
        l1.className = 'step-line';
    }
}


// ── Close + reset all modals ──────────────────────────────────────────────────
function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
    document.body.style.overflow = '';
    resetBookingModal();
    resetRequestModal();
}

function resetBookingModal() {
    const p = document.getElementById('book-pickup');
    const d = document.getElementById('book-dropoff');
    if (p) p.value = '';
    if (d) d.value = '';
    setStep('book', 1);
    const t = document.getElementById('booking-modal-title');
    if (t) t.textContent = 'Where are you going?';
    document.querySelectorAll('.input-error-msg').forEach(e => e.remove());
}

function resetRequestModal() {
    const p = document.getElementById('req-pickup');
    const d = document.getElementById('req-dropoff');
    if (p) p.value = '';
    if (d) d.value = '';
    setStep('req', 1);
    const t = document.getElementById('request-modal-title');
    if (t) t.textContent = 'Where are you going?';
}


// ── Input error helper ────────────────────────────────────────────────────────
function showInputError(inputId, message) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const wrap = input.closest('.location-input-group');
    if (wrap) wrap.style.borderColor = '#dc2626';

    const existing = wrap && wrap.parentElement.querySelector('.input-error-msg');
    if (existing) existing.remove();

    const msg = document.createElement('p');
    msg.className = 'input-error-msg';
    msg.style.cssText = 'color:#dc2626;font-size:0.78rem;margin:4px 0 0 14px;';
    msg.textContent = message;
    if (wrap) wrap.after(msg);

    input.addEventListener('input', () => {
        if (wrap) wrap.style.borderColor = '';
        msg.remove();
    }, { once: true });

    input.focus();
}


// ── Close on backdrop click + Escape ─────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    ['booking-modal', 'request-modal'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('click', e => { if (e.target === el) closeAllModals(); });
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeAllModals(); });
});