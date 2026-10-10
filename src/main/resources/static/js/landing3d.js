/**
 * StockGuard 3D Interactive Hero Visualization (Three.js)
 * Tasteful 3D Shield and Floating Inventory Stock Cubes
 */
function initLanding3D() {
  const container = document.getElementById('canvas-3d-hero');
  if (!container || typeof THREE === 'undefined') return;

  // Check WebGL availability
  try {
    const testCanvas = document.createElement('canvas');
    if (!Boolean(window.WebGLRenderingContext && (testCanvas.getContext('webgl') || testCanvas.getContext('experimental-webgl')))) {
      renderFallback(container);
      return;
    }
  } catch (e) {
    renderFallback(container);
    return;
  }

  const width = container.clientWidth || 500;
  const height = container.clientHeight || 440;

  // Scene & Camera
  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
  camera.position.set(0, 0, 7.5);

  const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  container.appendChild(renderer.domElement);

  // Group for all floating elements
  const group = new THREE.Group();
  scene.add(group);

  // 1. Central 3D Shield Geometry (Custom Extruded Shape)
  const shieldShape = new THREE.Shape();
  shieldShape.moveTo(0, 1.4);
  shieldShape.quadraticCurveTo(1.2, 1.3, 1.2, 0.2);
  shieldShape.quadraticCurveTo(1.1, -1.0, 0, -1.6);
  shieldShape.quadraticCurveTo(-1.1, -1.0, -1.2, 0.2);
  shieldShape.quadraticCurveTo(-1.2, 1.3, 0, 1.4);

  const extrudeSettings = {
    depth: 0.35,
    bevelEnabled: true,
    bevelSegments: 4,
    steps: 1,
    bevelSize: 0.1,
    bevelThickness: 0.1,
  };

  const shieldGeo = new THREE.ExtrudeGeometry(shieldShape, extrudeSettings);
  const shieldMat = new THREE.MeshStandardMaterial({
    color: 0x1677FF,
    metalness: 0.4,
    roughness: 0.2,
    emissive: 0x072B66,
  });
  const shieldMesh = new THREE.Mesh(shieldGeo, shieldMat);
  shieldMesh.position.set(0, 0, 0);
  group.add(shieldMesh);

  // Inner Emblem (Checkmark / Inner Crest)
  const innerShape = new THREE.Shape();
  innerShape.moveTo(0, 1.1);
  innerShape.quadraticCurveTo(0.85, 1.0, 0.85, 0.15);
  innerShape.quadraticCurveTo(0.8, -0.75, 0, -1.25);
  innerShape.quadraticCurveTo(-0.8, -0.75, -0.85, 0.15);
  innerShape.quadraticCurveTo(-0.85, 1.0, 0, 1.1);

  const innerGeo = new THREE.ExtrudeGeometry(innerShape, { depth: 0.4, bevelEnabled: false });
  const innerMat = new THREE.MeshStandardMaterial({
    color: 0x28D978,
    metalness: 0.3,
    roughness: 0.3,
    emissive: 0x094825,
  });
  const innerMesh = new THREE.Mesh(innerGeo, innerMat);
  innerMesh.position.set(0, 0, 0.05);
  group.add(innerMesh);

  // 2. Floating Inventory Stock Cubes & Grocery Cartons
  const boxGeometry = new THREE.BoxGeometry(0.5, 0.5, 0.5);
  const materials = [
    new THREE.MeshStandardMaterial({ color: 0x1677FF, roughness: 0.3 }),
    new THREE.MeshStandardMaterial({ color: 0x28D978, roughness: 0.3 }),
    new THREE.MeshStandardMaterial({ color: 0xF59E0B, roughness: 0.3 }),
    new THREE.MeshStandardMaterial({ color: 0x0B1530, roughness: 0.4 }),
  ];

  const floatingCubes = [];
  const cubeCoords = [
    { x: -2.2, y: 1.2, z: 0.5, matIdx: 1, rotSpeed: 0.015 },
    { x: 2.1, y: 1.4, z: -0.4, matIdx: 0, rotSpeed: -0.018 },
    { x: -1.9, y: -1.3, z: 0.8, matIdx: 2, rotSpeed: 0.012 },
    { x: 2.3, y: -1.0, z: 0.2, matIdx: 0, rotSpeed: -0.014 },
    { x: 0.0, y: 2.2, z: -0.5, matIdx: 3, rotSpeed: 0.01 },
  ];

  cubeCoords.forEach(coord => {
    const cube = new THREE.Mesh(boxGeometry, materials[coord.matIdx]);
    cube.position.set(coord.x, coord.y, coord.z);
    cube.userData = {
      baseY: coord.y,
      rotSpeed: coord.rotSpeed,
      floatOffset: Math.random() * Math.PI * 2,
    };
    group.add(cube);
    floatingCubes.push(cube);
  });

  // Lighting
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
  scene.add(ambientLight);

  const dirLight = new THREE.DirectionalLight(0xffffff, 1.2);
  dirLight.position.set(5, 8, 6);
  scene.add(dirLight);

  const blueLight = new THREE.PointLight(0x1677FF, 2, 10);
  blueLight.position.set(-3, -2, 3);
  scene.add(blueLight);

  const greenLight = new THREE.PointLight(0x28D978, 2, 10);
  greenLight.position.set(3, 2, 3);
  scene.add(greenLight);

  // Mouse Parallax Interaction
  let mouseX = 0, mouseY = 0;
  let targetRotX = 0, targetRotY = 0;

  window.addEventListener('mousemove', (e) => {
    const rect = container.getBoundingClientRect();
    if (rect.top < window.innerHeight && rect.bottom > 0) {
      mouseX = ((e.clientX - rect.left) / width - 0.5) * 2;
      mouseY = ((e.clientY - rect.top) / height - 0.5) * 2;
      targetRotY = mouseX * 0.45;
      targetRotX = -mouseY * 0.35;
    }
  });

  // Animation Loop
  let clock = new THREE.Clock();

  function animate() {
    requestAnimationFrame(animate);
    const elapsedTime = clock.getElapsedTime();

    // Gentle Shield breathing and float
    shieldMesh.rotation.y = Math.sin(elapsedTime * 0.8) * 0.12;
    innerMesh.rotation.y = shieldMesh.rotation.y;

    // Smooth Parallax Interpolation
    group.rotation.y += (targetRotY - group.rotation.y) * 0.05;
    group.rotation.x += (targetRotX - group.rotation.x) * 0.05;

    // Floating cubes motion
    floatingCubes.forEach(cube => {
      cube.rotation.x += cube.userData.rotSpeed;
      cube.rotation.y += cube.userData.rotSpeed;
      cube.position.y = cube.userData.baseY + Math.sin(elapsedTime * 1.5 + cube.userData.floatOffset) * 0.15;
    });

    renderer.render(scene, camera);
  }

  animate();

  // Resize handler
  window.addEventListener('resize', () => {
    const newW = container.clientWidth;
    const newH = container.clientHeight;
    camera.aspect = newW / newH;
    camera.updateProjectionMatrix();
    renderer.setSize(newW, newH);
  });
}

function renderFallback(container) {
  container.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:center;height:100%;background:rgba(22,119,255,0.08);border-radius:24px;border:1px solid rgba(255,255,255,0.1);">
      <div style="text-align:center;padding:2rem;">
        <i class="fa-solid fa-shield-halved" style="font-size:5rem;color:#1677FF;margin-bottom:1rem;display:block;"></i>
        <h4 style="color:#fff;font-weight:700;">StockGuard 3D Protected Shield</h4>
        <p style="color:#94A3B8;font-size:0.85rem;">Active Real-Time Inventory & Expiry Surveillance</p>
      </div>
    </div>
  `;
}

document.addEventListener('DOMContentLoaded', initLanding3D);
