'use strict';
const parameters = new URLSearchParams(window.location.search);
const latitude = Number(parameters.get('lat'));
const longitude = Number(parameters.get('lon'));
const map = L.map('map', { worldCopyJump: true }).setView([latitude, longitude], 14);
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
if (parameters.get('selected') === 'true') selectPin(L.latLng(latitude, longitude));
