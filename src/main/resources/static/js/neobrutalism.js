/**
 * Neo-Brutalist Interactive Scrapbook Experience
 * 1. Matrix Scramble Typing Effect
 * 2. Falling Decorative Gravity Icons
 * 3. Scroll-Driven Highlighter Pen Animations
 * 4. 3D Book-Flip Treasure Map Timeline
 * 5. Interactive Leaflet.js Journey Map
 * 6. Light / Dark Mode Brutal Switcher
 */

document.addEventListener('DOMContentLoaded', () => {
  initThemeToggle();
  initMatrixScramble();
  initScrollHighlighters();
  initPhysicsIcons();
  initBookFlip();
  initLeafletMap();
});

/* =========================================================
   1. THEME TOGGLE
   ========================================================= */
function initThemeToggle() {
  const toggleBtn = document.getElementById('theme-toggle-btn');
  if (!toggleBtn) return;

  const currentTheme = localStorage.getItem('stockguard_theme') || localStorage.getItem('neo_theme') || 'light';
  if (currentTheme === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark');
    toggleBtn.innerHTML = '<i class="fa-solid fa-sun"></i> <span>LIGHT MODE</span>';
  }

  toggleBtn.addEventListener('click', () => {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    if (isDark) {
      document.documentElement.removeAttribute('data-theme');
      localStorage.setItem('neo_theme', 'light');
      localStorage.setItem('stockguard_theme', 'light');
      toggleBtn.innerHTML = '<i class="fa-solid fa-moon"></i> <span>DARK MODE</span>';
    } else {
      document.documentElement.setAttribute('data-theme', 'dark');
      localStorage.setItem('neo_theme', 'dark');
      localStorage.setItem('stockguard_theme', 'dark');
      toggleBtn.innerHTML = '<i class="fa-solid fa-sun"></i> <span>LIGHT MODE</span>';
    }
  });
}

/* =========================================================
   2. MATRIX SCRAMBLE TYPING EFFECT
   ========================================================= */
function initMatrixScramble() {
  const el = document.getElementById('matrix-type-target');
  if (!el) return;

  const phrases = [
    "HEY THERE, I'M ATHARV RAUT.",
    "FULL-STACK JAVA & MODERN UI ARCHITECT.",
    "UNAPOLOGETICALLY LOUD NEO-BRUTALIST.",
    "CODE IS POETRY. BORDERS ARE THICK."
  ];

  const chars = '!<>-_\\/[]{}—=+*^?#_0101X@';
  let phraseIndex = 0;

  function scrambleTo(targetText) {
    let frame = 0;
    const length = targetText.length;
    let resolved = '';

    const timer = setInterval(() => {
      resolved = '';
      for (let i = 0; i < length; i++) {
        if (i < frame / 2.5) {
          resolved += targetText[i];
        } else {
          resolved += chars[Math.floor(Math.random() * chars.length)];
        }
      }
      el.textContent = resolved;

      if (frame / 2.5 >= length) {
        clearInterval(timer);
        setTimeout(() => {
          phraseIndex = (phraseIndex + 1) % phrases.length;
          scrambleTo(phrases[phraseIndex]);
        }, 2800);
      }
      frame++;
    }, 35);
  }

  scrambleTo(phrases[0]);
}

/* =========================================================
   3. SCROLL-DRIVEN HIGHLIGHTER PEN MARKERS
   ========================================================= */
function initScrollHighlighters() {
  const highlighters = document.querySelectorAll('.highlight-pen');
  if (!highlighters.length) return;

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('active');
      }
    });
  }, {
    threshold: 0.3
  });

  highlighters.forEach(h => observer.observe(h));
}

/* =========================================================
   4. FALLING DECORATIVE ICONS (GRAVITY & PHYSICS)
   ========================================================= */
function initPhysicsIcons() {
  const playground = document.getElementById('physics-playground');
  if (!playground) return;

  const items = Array.from(playground.querySelectorAll('.physics-item'));
  const floorHeight = 24;

  const physicsObjects = items.map((el, i) => {
    const rect = el.getBoundingClientRect();
    const playRect = playground.getBoundingClientRect();

    const startX = 30 + i * 85;
    const startY = 30 + (i % 3) * 45;

    el.style.left = `${startX}px`;
    el.style.top = `${startY}px`;

    return {
      el,
      x: startX,
      y: startY,
      vx: (Math.random() - 0.5) * 4,
      vy: 0,
      rotation: (Math.random() - 0.5) * 20,
      vRot: (Math.random() - 0.5) * 3,
      isFalling: false,
      isDragging: false,
      width: rect.width || 80,
      height: rect.height || 60,
    };
  });

  let hasDropped = false;

  function triggerFall() {
    if (hasDropped) return;
    hasDropped = true;
    physicsObjects.forEach(obj => {
      obj.isFalling = true;
      obj.vy = Math.random() * 3 + 2;
    });
  }

  // Trigger fall upon scroll
  window.addEventListener('scroll', () => {
    if (window.scrollY > 40) triggerFall();
  }, { once: true });

  // Or trigger when playground is clicked
  playground.addEventListener('click', triggerFall);

  // Dragging support
  physicsObjects.forEach(obj => {
    obj.el.addEventListener('mousedown', (e) => {
      obj.isDragging = true;
      obj.isFalling = false;
      const playRect = playground.getBoundingClientRect();

      function onMouseMove(moveEvent) {
        if (!obj.isDragging) return;
        obj.x = moveEvent.clientX - playRect.left - obj.width / 2;
        obj.y = moveEvent.clientY - playRect.top - obj.height / 2;
        obj.el.style.left = `${obj.x}px`;
        obj.el.style.top = `${obj.y}px`;
      }

      function onMouseUp() {
        obj.isDragging = false;
        obj.isFalling = true;
        obj.vy = 2;
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
      }

      window.addEventListener('mousemove', onMouseMove);
      window.addEventListener('mouseup', onMouseUp);
    });
  });

  // Physics animation loop
  const gravity = 0.65;
  const restitution = 0.58; // Bounciness
  const friction = 0.94;

  function updatePhysics() {
    requestAnimationFrame(updatePhysics);
    const bounds = playground.getBoundingClientRect();
    const maxY = bounds.height - floorHeight;

    physicsObjects.forEach(obj => {
      if (!obj.isFalling || obj.isDragging) return;

      obj.vy += gravity;
      obj.x += obj.vx;
      obj.y += obj.vy;
      obj.rotation += obj.vRot;

      // Wall collisions
      if (obj.x < 0) {
        obj.x = 0;
        obj.vx = -obj.vx * restitution;
      } else if (obj.x + obj.width > bounds.width) {
        obj.x = bounds.width - obj.width;
        obj.vx = -obj.vx * restitution;
      }

      // Floor collision with bounce
      if (obj.y + obj.height >= maxY) {
        obj.y = maxY - obj.height;
        obj.vy = -obj.vy * restitution;
        obj.vx *= friction;
        obj.vRot *= friction;

        // Settle when velocity is minimal
        if (Math.abs(obj.vy) < 1) {
          obj.vy = 0;
        }
      }

      obj.el.style.left = `${obj.x}px`;
      obj.el.style.top = `${obj.y}px`;
      obj.el.style.transform = `rotate(${obj.rotation}deg)`;
    });
  }

  updatePhysics();
}

/* =========================================================
   5. BOOK FLIP TIMELINE
   ========================================================= */
function initBookFlip() {
  const flipWrapper = document.getElementById('book-flip-wrapper');
  const openBtn = document.getElementById('open-timeline-btn');
  const closeBtn = document.getElementById('close-timeline-btn');

  if (!flipWrapper) return;

  function toggleFlip(state) {
    if (state === undefined) {
      flipWrapper.classList.toggle('flipped');
    } else if (state) {
      flipWrapper.classList.add('flipped');
    } else {
      flipWrapper.classList.remove('flipped');
    }
  }

  if (openBtn) openBtn.addEventListener('click', () => toggleFlip(true));
  if (closeBtn) closeBtn.addEventListener('click', () => toggleFlip(false));
}

/* =========================================================
   6. INTERACTIVE LEAFLET.JS MAP WITH TIMELINE FLY-TO
   ========================================================= */
let journeyMap = null;
const mapWaypoints = [
  { id: 'step-pune', name: 'Pune, India', lat: 18.5204, lng: 73.8567, desc: 'Origins & Core Java Systems Engineering' },
  { id: 'step-blr', name: 'Bengaluru, India', lat: 12.9716, lng: 77.5946, desc: 'Silicon Plateau - Enterprise Scaled Cloud APIs' },
  { id: 'step-sf', name: 'San Francisco, USA', lat: 37.7749, lng: -122.4194, desc: 'Global Open-Source Collaboration & Cloud Architecture' },
  { id: 'step-london', name: 'London, UK', lat: 51.5074, lng: -0.1278, desc: 'Fintech & Multi-Tenant Inventory Architectures' },
  { id: 'step-tokyo', name: 'Tokyo, Japan', lat: 35.6762, lng: 139.6503, desc: 'Creative Neo-Brutalist & Spatial UI Design Lab' },
];

function initLeafletMap() {
  const mapEl = document.getElementById('leaflet-journey-map');
  if (!mapEl || typeof L === 'undefined') return;

  // Initialize Map centered on India / Global
  journeyMap = L.map('leaflet-journey-map', {
    scrollWheelZoom: false
  }).setView([20.5937, 78.9629], 4);

  // CartoDB Positron / OSM tiles with brutalist high-contrast filter
  L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
    maxZoom: 18,
  }).addTo(journeyMap);

  // Custom Neo-Brutalist Icon
  const brutalIcon = L.divIcon({
    className: 'brutal-map-marker',
    html: `
      <div style="background:#FFE600; width:34px; height:34px; border:3px solid #000; box-shadow:4px 4px 0 #000; display:flex; align-items:center; justify-content:center; font-size:16px; transform:rotate(-4deg);">
        🏴‍☠️
      </div>
    `,
    iconSize: [34, 34],
    iconAnchor: [17, 34],
    popupAnchor: [0, -34]
  });

  // Add markers
  const markers = {};
  mapWaypoints.forEach(wp => {
    const marker = L.marker([wp.lat, wp.lng], { icon: brutalIcon })
      .addTo(journeyMap)
      .bindPopup(`
        <div style="font-family:'Space Grotesk',sans-serif; padding:4px;">
          <h4 style="font-weight:800; font-size:1.1rem; text-transform:uppercase; margin-bottom:4px;">${wp.name}</h4>
          <p style="font-size:0.85rem; font-weight:600;">${wp.desc}</p>
        </div>
      `);
    markers[wp.id] = marker;
  });

  // Hook up timeline entry clicks
  document.querySelectorAll('.timeline-entry').forEach(entry => {
    entry.addEventListener('click', () => {
      const targetId = entry.getAttribute('data-waypoint-id');
      const point = mapWaypoints.find(w => w.id === targetId);
      if (point && journeyMap) {
        journeyMap.flyTo([point.lat, point.lng], 6, {
          duration: 1.5,
          easeLinearity: 0.25
        });

        setTimeout(() => {
          if (markers[targetId]) {
            markers[targetId].openPopup();
          }
        }, 1600);
      }
    });
  });
}
