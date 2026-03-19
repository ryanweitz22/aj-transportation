/**
 * bookings.js — AJ Transportation
 * 2-step booking modal with live fare calculation via Google Maps Distance Matrix.
 */

// ── Google Places Autocomplete ────────────────────────────────────────────────
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
}

function attachAutocomplete(inputId, options) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const ac = new google.maps.places.Autocomplete(input, options);
    ac.addListener('place_changed', () => {
        const place = ac.getPlace();
        if (place && place.formatted_address) input.value = place.formatted_address;
    });
}

window.initAutocomplete = window.initAutocomplete || function () {};


// ── Step 1 → Step 2 with fare calculation ────────────────────────────────────
async function bookStep1Next() {
    const pickup  = document.getElementById('book-pickup').value.trim();
    const dropoff = document.getElementById('book-dropoff').value.trim();

    if (!pickup)  { showInputError('book-pickup',  'Please enter a pickup address.'); return; }
    if (!dropoff) { showInputError('book-dropoff', 'Please enter a dropoff address.'); return; }

    // Copy to hidden fields
    document.getElementById('book-pickup-hidden').value  = pickup;
    document.getElementById('book-dropoff-hidden').value = dropoff;

    // Update confirm display
    document.getElementById('book-confirm-pickup').textContent  = pickup;
    document.getElementById('book-confirm-dropoff').textContent = dropoff;

    // Show loading state on fare while we calculate
    const fareEl = document.getElementById('book-confirm-fare');
    if (fareEl) fareEl.textContent = 'Calculating...';

    // Move to step 2 immediately so user sees the screen
    setStep('book', 2);
    document.getElementById('booking-modal-title').textContent = 'Your Trip';

    // Disable confirm button while calculating
    const confirmBtn = document.getElementById('book-confirm-btn');
    if (confirmBtn) { confirmBtn.disabled = true; confirmBtn.textContent = 'Calculating fare...'; }

    // Call backend to calculate fare
    try {
        const res = await fetch(`/bookings/calculate-fare?pickup=${encodeURIComponent(pickup)}&dropoff=${encodeURIComponent(dropoff)}`);
        const data = await res.json();

        if (data.success) {
            const fareFormatted = `R${parseFloat(data.fare).toFixed(2)}`;
            const distFormatted = `${data.distanceKm} km`;
            if (fareEl) fareEl.innerHTML = `<strong style="color:var(--primary);font-size:1.1rem;">${fareFormatted}</strong> <span style="color:var(--text-muted);font-size:0.8rem;">(${distFormatted} × R8/km, min R50)</span>`;
            // Store fare in hidden field
            const fareInput = document.getElementById('book-fare-hidden');
            if (fareInput) fareInput.value = data.fare;
        } else {
            if (fareEl) fareEl.innerHTML = `<span style="color:var(--text-muted);">Fare will be confirmed by driver</span>`;
        }
    } catch (e) {
        if (fareEl) fareEl.innerHTML = `<span style="color:var(--text-muted);">Fare will be confirmed by driver</span>`;
    }

    // Re-enable confirm button
    if (confirmBtn) { confirmBtn.disabled = false; confirmBtn.textContent = 'Proceed to Payment'; }
}

function bookGoBack() {
    setStep('book', 1);
    document.getElementById('booking-modal-title').textContent = 'Where are you going?';
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


// ── Close + reset ─────────────────────────────────────────────────────────────
function closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('open'));
    document.body.style.overflow = '';
    resetBookingModal();
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


// ── Backdrop + Escape ─────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    ['booking-modal'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener('click', e => { if (e.target === el) closeAllModals(); });
    });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeAllModals(); });
});