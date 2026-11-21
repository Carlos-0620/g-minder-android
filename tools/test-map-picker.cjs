const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/map/picker.js'), 'utf8');

function runPicker(search) {
  const events = {};
  const tileEvents = {};
  const selected = [];
  const availability = [];
  let markers = 0;
  let moved = null;
  let center = null;
  const map = {
    setView(point) { center = point; return this; },
    on(event, callback) { events[event] = callback; },
    attributionControl: { setPrefix() {} }
  };
  const point = (lat, lng) => ({ lat, lng, wrap() {
    return { lat, lng: ((lng + 180) % 360 + 360) % 360 - 180 };
  } });
  vm.runInNewContext(source, {
    URLSearchParams,
    window: { location: { search } },
    GMinderMap: {
      select(lat, lng) { selected.push([lat, lng]); },
      tilesAvailable(value) { availability.push(value); }
    },
    L: {
      map() { return map; },
      latLng: point,
      tileLayer() { return { on(event, callback) { tileEvents[event] = callback; }, addTo() {} }; },
      divIcon(options) { return options; },
      marker(position) {
        markers++;
        return { addTo() { return this; }, setLatLng(value) { moved = value; } };
      }
    }
  });
  return { events, tileEvents, selected, availability, point,
    get markers() { return markers; }, get moved() { return moved; }, get center() { return center; } };
}

const picker = runPicker('?lat=53.3498&lon=-6.2603&selected=false');
assert.deepEqual(Array.from(picker.center), [53.3498, -6.2603]);
assert.equal(picker.selected.length, 0, 'Initial centre must not select a reminder destination');
picker.events.click({ latlng: picker.point(53.3, -6.2) });
picker.events.click({ latlng: picker.point(53.4, 354) });
assert.equal(picker.markers, 1, 'A new tap must move the existing pin');
assert.deepEqual(picker.selected.at(-1), [53.4, -6], 'Wrapped world copies must return a valid longitude');
assert.equal(picker.moved.lng, -6);
picker.tileEvents.tileerror();
picker.tileEvents.tileload();
assert.deepEqual(picker.availability, [false, true]);
const restored = runPicker('?lat=53.3&lon=-6.2&selected=true');
assert.equal(restored.selected.length, 1, 'A previously confirmed UI selection must restore its marker');
console.log('Map picker checks passed: no default selection, moving pin, longitude wrapping, tile feedback and restoration.');
