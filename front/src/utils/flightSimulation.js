// Simulación de Vuelos para MoraPack
/* global L */
export class MoraPackFlightSimulation {
  constructor(mapElement) {
    this.map = null;
    this.mapElement = mapElement;
    this.flights = [];
    this.airports = [];
    this.routes = [];
    this.isPlaying = false;
    this.simulationRunning = false;
    this.speed = 1;
    this.simulationTime = new Date(2024, 7, 27, 8, 0, 0);
    this.startTime = new Date();
    this.flightMarkers = [];
    this.airportMarkers = [];
    this.routeLines = [];
    this.routeMarkers = [];
    this.currentRoute = null;
    this.selectedFlightId = null;
    this.currentView = 'flights';
    this.elapsedDays = 0;
    this.elapsedHours = 0;
    this.elapsedMinutes = 0;
    
    this.init();
  }

  init() {
    if (this.mapElement) {
      this.initMap();
      this.generateAirports();
      this.generateFlights();
      this.startSimulation();
    }
  }

  initMap() {
    // Verificar si Leaflet está disponible
    if (typeof L === 'undefined') {
      console.error('Leaflet no está disponible');
      return;
    }

    this.map = L.map(this.mapElement, {
      center: [20.0, 10.0],
      zoom: 2,
      zoomControl: true,
      attributionControl: false,
      maxBounds: [[-85, -180], [85, 180]],
      maxBoundsViscosity: 0.8,
      worldCopyJump: false,
      minZoom: 2,
      maxZoom: 7,
      wheelPxPerZoomLevel: 120,
      doubleClickZoom: true,
      scrollWheelZoom: true,
      dragging: true,
      touchZoom: true
    });
    
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      noWrap: true,
      maxZoom: 7,
      subdomains: ['a', 'b', 'c']
    }).addTo(this.map);

    this.map.getContainer().style.background = '#a8d8ea';
    this.map.zoomControl.setPosition('bottomright');
    
    this.map.on('zoomend', () => {
      const currentZoom = this.map.getZoom();
      if (currentZoom > 7) {
        this.map.setZoom(7);
      } else if (currentZoom < 2) {
        this.map.setZoom(2);
      }
    });
  }

  generateAirports() {
    this.airports = [
      // SEDE PRINCIPAL: LIMA, PERÚ
      { name: 'Lima-Jorge Chávez', code: 'LIM', lat: -12.0219, lng: -77.1143, capacity: 1200, isSede: true, region: 'América del Sur', country: 'Perú' },
      { name: 'Bogotá-El Dorado', code: 'BOG', lat: 4.7016, lng: -74.1469, capacity: 900, region: 'América del Sur', country: 'Colombia' },
      { name: 'Quito-Mariscal Sucre', code: 'UIO', lat: -0.1292, lng: -78.3576, capacity: 600, region: 'América del Sur', country: 'Ecuador' },
      
      // SEDE PRINCIPAL: BRUSELAS, BÉLGICA
      { name: 'Bruselas', code: 'BRU', lat: 50.9010, lng: 4.4844, capacity: 1100, isSede: true, region: 'Europa', country: 'Bélgica' },
      { name: 'Amsterdam-Schiphol', code: 'AMS', lat: 52.3105, lng: 4.7683, capacity: 1200, region: 'Europa', country: 'Países Bajos' },
      { name: 'París-Charles de Gaulle', code: 'CDG', lat: 49.0097, lng: 2.5479, capacity: 1300, region: 'Europa', country: 'Francia' },
      
      // SEDE PRINCIPAL: BAKU, AZERBAIYÁN
      { name: 'Baku-Heydar Aliyev', code: 'GYD', lat: 40.4675, lng: 50.0467, capacity: 1000, isSede: true, region: 'Asia Central', country: 'Azerbaiyán' },
      { name: 'Estambul', code: 'IST', lat: 41.2619, lng: 28.7419, capacity: 1100, region: 'Asia Central', country: 'Turquía' },
      { name: 'Dubai', code: 'DXB', lat: 25.2532, lng: 55.3657, capacity: 1200, region: 'Asia Central', country: 'EAU' }
    ];
  }

  generateFlights() {
    this.flights = [];
    const flightTypes = ['MP', 'PACK'];
    
    for (let i = 0; i < 20; i++) {
      const origin = this.airports[Math.floor(Math.random() * this.airports.length)];
      let destination = this.airports[Math.floor(Math.random() * this.airports.length)];
      
      while (destination.code === origin.code) {
        destination = this.airports[Math.floor(Math.random() * this.airports.length)];
      }
      
      const flight = {
        id: `MP${String(i + 1000).padStart(4, '0')}`,
        type: flightTypes[Math.floor(Math.random() * flightTypes.length)],
        origin: origin,
        destination: destination,
        departure: new Date(this.simulationTime.getTime() + Math.random() * 24 * 60 * 60 * 1000),
        capacity: Math.floor(Math.random() * 200) + 100,
        currentLat: origin.lat,
        currentLng: origin.lng,
        progress: 0,
        status: 'scheduled'
      };
      
      this.flights.push(flight);
    }
  }

  startSimulation() {
    this.simulationRunning = true;
    this.isPlaying = true;
    this.updateSimulation();
  }

  updateSimulation() {
    if (!this.simulationRunning || !this.isPlaying) return;
    
    // Actualizar tiempo de simulación
    this.simulationTime = new Date(this.simulationTime.getTime() + (this.speed * 60000)); // 1 minuto por segundo
    
    // Actualizar vuelos
    this.flights.forEach(flight => {
      if (flight.status === 'scheduled' && flight.departure <= this.simulationTime) {
        flight.status = 'in-flight';
      }
      
      if (flight.status === 'in-flight') {
        flight.progress += 0.01 * this.speed;
        if (flight.progress >= 1) {
          flight.progress = 1;
          flight.status = 'arrived';
        }
        
        // Interpolar posición
        const lat = flight.origin.lat + (flight.destination.lat - flight.origin.lat) * flight.progress;
        const lng = flight.origin.lng + (flight.destination.lng - flight.origin.lng) * flight.progress;
        flight.currentLat = lat;
        flight.currentLng = lng;
      }
    });
    
    this.updateDisplay();
    
    setTimeout(() => this.updateSimulation(), 1000);
  }

  updateDisplay() {
    if (!this.map) return;
    
    // Limpiar marcadores existentes
    this.flightMarkers.forEach(marker => this.map.removeLayer(marker));
    this.flightMarkers = [];
    
    // Mostrar vuelos en vuelo
    this.flights.forEach(flight => {
      if (flight.status === 'in-flight') {
        const marker = L.marker([flight.currentLat, flight.currentLng], {
          icon: L.divIcon({
            className: 'flight-marker',
            html: `<div class="flight-icon"><i class="fas fa-plane"></i></div>`,
            iconSize: [20, 20]
          })
        }).addTo(this.map);
        
        marker.bindPopup(`
          <div class="flight-popup">
            <h4>${flight.id}</h4>
            <p><strong>Origen:</strong> ${flight.origin.name}</p>
            <p><strong>Destino:</strong> ${flight.destination.name}</p>
            <p><strong>Progreso:</strong> ${Math.round(flight.progress * 100)}%</p>
          </div>
        `);
        
        this.flightMarkers.push(marker);
      }
    });
  }

  play() {
    this.isPlaying = true;
    if (!this.simulationRunning) {
      this.startSimulation();
    }
  }

  pause() {
    this.isPlaying = false;
  }

  stop() {
    this.isPlaying = false;
    this.simulationRunning = false;
    this.flights.forEach(flight => {
      flight.status = 'scheduled';
      flight.progress = 0;
      flight.currentLat = flight.origin.lat;
      flight.currentLng = flight.origin.lng;
    });
    this.updateDisplay();
  }

  setSpeed(speed) {
    this.speed = speed;
  }

  getCurrentTime() {
    return this.simulationTime.toLocaleString();
  }

  getElapsedTime() {
    const elapsed = Date.now() - this.startTime.getTime();
    const hours = Math.floor(elapsed / (1000 * 60 * 60));
    const minutes = Math.floor((elapsed % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((elapsed % (1000 * 60)) / 1000);
    
    return `${String(hours).padStart(2, '0')} : ${String(minutes).padStart(2, '0')} : ${String(seconds).padStart(2, '0')}`;
  }
}
