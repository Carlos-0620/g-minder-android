'use strict';
const parameters = new URLSearchParams(window.location.search);
const latitude = Number(parameters.get('lat'));
const longitude = Number(parameters.get('lon'));
const map = L.map('map', { worldCopyJump: true }).setView([latitude, longitude], 14);
window.refreshMapSize = () => {
  document.getElementById('map').style.height = Math.max(1, window.innerHeight) + 'px';
  map.invalidateSize({ pan: false });
};
window.addEventListener('resize', window.refreshMapSize);
if (typeof ResizeObserver !== 'undefined') new ResizeObserver(window.refreshMapSize).observe(document.getElementById('map'));
requestAnimationFrame(window.refreshMapSize);
map.attributionControl.setPrefix(false);
const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom: 19,
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
});
tiles.on('tileload', () => GMinderMap.tilesAvailable(true));
tiles.on('tileerror', () => GMinderMap.tilesAvailable(false));
tiles.addTo(map);
let pin = null;
function selectPin(point) {
  const wrapped = point.wrap();
  if (pin) pin.setLatLng(wrapped);
  else pin = L.marker(wrapped, {
    icon: L.divIcon({ className: 'map-pin', html: '<span></span>', iconSize: [32, 38], iconAnchor: [16, 38] }),
    title: 'Selected reminder location',
    alt: 'Selected reminder location'
  }).addTo(map);
  GMinderMap.select(wrapped.lat, wrapped.lng);
}
map.on('click', event => selectPin(event.latlng));
window.selectSearchPin = (lat, lon) => {
  if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return;
  map.setView([lat, lon], 17);
  selectPin(L.latLng(lat, lon));
};
if (parameters.get('selected') === 'true') selectPin(L.latLng(latitude, longitude));
