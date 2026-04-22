-- ============================================================
-- OpenRoof — SEED COMPLETO (Adaptado a Modelos Java)
-- Ejecutar en: Supabase Dashboard → SQL Editor
-- ============================================================

BEGIN;

-- ============================================================
-- 1. EXTERIOR FEATURES (catálogo)
-- ============================================================
INSERT INTO exterior_features (name, category, created_at, updated_at, version)
SELECT name, category, NOW(), NOW(), 0
FROM (VALUES
    ('Piscina',            'AMENITY'),
    ('Jardín',             'OUTDOOR'),
    ('Garage',             'AMENITY'),
    ('Parrilla / Quincho', 'OUTDOOR'),
    ('Cerco eléctrico',    'SECURITY'),
    ('Cámara de seguridad','SECURITY'),
    ('Terraza',            'OUTDOOR'),
    ('Portón automático',  'SECURITY'),
    ('Cancha deportiva',   'AMENITY'),
    ('Salón de eventos',   'AMENITY')
) AS t(name, category)
WHERE NOT EXISTS (SELECT 1 FROM exterior_features LIMIT 1);

-- ============================================================
-- 2. INTERIOR FEATURES (catálogo)
-- ============================================================
INSERT INTO interior_features (name, category, created_at, updated_at, version)
SELECT name, category, NOW(), NOW(), 0
FROM (VALUES
    ('Aire acondicionado',   'APPLIANCE'),
    ('Piso de porcelanato',  'FINISH'),
    ('Piso de madera',       'FINISH'),
    ('Closet empotrado',     'FURNITURE'),
    ('Cocina equipada',      'APPLIANCE'),
    ('Calefón',              'APPLIANCE'),
    ('Ventilador de techo',  'APPLIANCE'),
    ('Mesada de granito',    'FINISH')
) AS t(name, category)
WHERE NOT EXISTS (SELECT 1 FROM interior_features LIMIT 1);

-- ============================================================
-- 3. LOCATIONS (ciudades y barrios de Paraguay)
-- ============================================================
INSERT INTO locations (name, city, department, country, created_at, updated_at, version)
SELECT name, city, department, country, NOW(), NOW(), 0
FROM (VALUES
    ('Barrio Herrera',         'Asunción',               'Capital',     'Paraguay'),
    ('Villa Morra',            'Asunción',               'Capital',     'Paraguay'),
    ('Carmelitas',             'Asunción',               'Capital',     'Paraguay'),
    ('Manorá',                 'Asunción',               'Capital',     'Paraguay'),
    ('Centro',                 'Asunción',               'Capital',     'Paraguay'),
    ('San Lorenzo',            'San Lorenzo',            'Central',     'Paraguay'),
    ('Luque',                  'Luque',                  'Central',     'Paraguay'),
    ('Lambaré',                'Lambaré',                'Central',     'Paraguay'),
    ('Fernando de la Mora',    'Fernando de la Mora',    'Central',     'Paraguay'),
    ('Encarnación',            'Encarnación',            'Itapúa',      'Paraguay'),
    ('Ciudad del Este',        'Ciudad del Este',        'Alto Paraná', 'Paraguay'),
    ('Mariano Roque Alonso',   'Mariano Roque Alonso',   'Central',     'Paraguay')
) AS t(name, city, department, country)
WHERE NOT EXISTS (SELECT 1 FROM locations LIMIT 1);

-- ============================================================
-- 4. LEAD STATUSES
-- ============================================================
INSERT INTO lead_statuses (name, created_at, version)
SELECT name, NOW(), 0
FROM (VALUES
    ('Nuevo'),
    ('Contactado'),
    ('Calificado'),
    ('Propuesta'),
    ('Cerrado'),
    ('Perdido')
) AS t(name)
WHERE NOT EXISTS (SELECT 1 FROM lead_statuses LIMIT 1);

-- ============================================================
-- 5. USUARIOS (contraseña: Test1234!)
-- Adaptado al enum UserRole: USER, AGENT, ADMIN
-- ============================================================
INSERT INTO users (email, password_hash, name, phone, role, email_verified_at, created_at, updated_at, version)
SELECT email, password_hash, name, phone, role, NOW(), NOW(), NOW(), 0
FROM (VALUES
    ('admin@openroof.com',          '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Admin OpenRoof',    '+595981000001', 'ADMIN'),
    ('agente1@openroof.com',        '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Carlos Mendoza',    '+595981000002', 'AGENT'),
    ('agente2@openroof.com',        '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'María González',    '+595981000003', 'AGENT'),
    ('propietario1@openroof.com',   '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Roberto Fernández', '+595981000004', 'USER'),
    ('propietario2@openroof.com',   '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Ana Benítez',       '+595981000005', 'USER'),
    ('comprador1@openroof.com',     '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Luis Ramírez',      '+595981000006', 'USER'),
    ('comprador2@openroof.com',     '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Sofía Villalba',    '+595981000007', 'USER'),
    ('inquilino1@openroof.com',     '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Diego Acosta',      '+595981000008', 'USER')
) AS t(email, password_hash, name, phone, role)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@openroof.com');

-- ============================================================
-- 6. AGENT PROFILES
-- ============================================================
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id,
    'Especialista en propiedades residenciales en Asunción con más de 8 años de experiencia.',
    'Mendoza Inmobiliaria', 8, 'PY-AG-2018-001', 4.7, 23,
    NOW(), NOW(), 0
FROM users u
WHERE u.email = 'agente1@openroof.com'
  AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2018-001');

INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id,
    'Agente certificada en bienes raíces comerciales y residenciales. Enfoque en zona Gran Asunción.',
    'GonzálezProp', 5, 'PY-AG-2021-042', 4.9, 15,
    NOW(), NOW(), 0
FROM users u
WHERE u.email = 'agente2@openroof.com'
  AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2021-042');


-- ============================================================
-- 6b. AGENT SPECIALTIES (catálogo de especialidades)
-- ============================================================
INSERT INTO agent_specialties (name, created_at, updated_at, version)
SELECT name, NOW(), NOW(), 0
FROM (VALUES
    ('residencial'),
    ('casas'),
    ('departamentos'),
    ('apartamentos'),
    ('terrenos'),
    ('lotes'),
    ('comercial'),
    ('oficinas'),
    ('industrial'),
    ('depósitos'),
    ('bodegas'),
    ('rural'),
    ('campos'),
    ('estancias'),
    ('agrícola'),
    ('lujo'),
    ('inversiones')
) AS t(name)
WHERE NOT EXISTS (
    SELECT 1 FROM agent_specialties s2 WHERE s2.name = t.name
);

-- ============================================================
-- 6c. AGENT TO SPECIALTIES (relación agentes-especialidades)
-- ============================================================
-- Agente 1 (Carlos Mendoza): residencial, casas, departamentos, lujo
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id
FROM agent_profiles ap
CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2018-001'
  AND s.name IN ('residencial', 'casas', 'departamentos', 'lujo')
  AND NOT EXISTS (
      SELECT 1 FROM agent_to_specialties ats 
      WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id
  );

-- Agente 2 (María González): comercial, terrenos, oficinas, inversiones, lotes
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id
FROM agent_profiles ap
CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2021-042'
  AND s.name IN ('comercial', 'terrenos', 'oficinas', 'inversiones', 'lotes', 'industrial')
  AND NOT EXISTS (
      SELECT 1 FROM agent_to_specialties ats 
      WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id
  );

-- ============================================================
-- 7. PROPIEDADES (10 propiedades variadas)
-- Incluye adaptaciones para buyer_id y tenant_id mapeados a users(id)
-- ============================================================
INSERT INTO properties (
    title, description, address, price, property_type, status, visibility, category,
    availability, bedrooms, bathrooms, built_area, surface_area, parking_spaces,
    floors_count, construction_year, lat, lng, highlighted,
    owner_id, agent_id, buyer_id, tenant_id, location_id, created_at, updated_at, version
)
SELECT
    p.title, p.description, p.address, p.price, p.property_type, p.status, p.visibility, p.category,
    p.availability, p.bedrooms, p.bathrooms, p.built_area, p.surface_area, p.parking_spaces,
    p.floors_count, p.construction_year, p.lat, p.lng, p.highlighted,
    u.id AS owner_id, ap.id AS agent_id, b.id AS buyer_id, t.id AS tenant_id, l.id AS location_id,
    NOW(), NOW(), 0
FROM (VALUES
    -- 1. Propiedad con COMPRADOR asignado (Venta cerrada)
    ('Casa moderna en Villa Morra',
     'Hermosa casa de 3 plantas con acabados de lujo. Venta concretada recientemente.',
     'Av. Mariscal López 1234, Villa Morra',
     285000.00::numeric, 'HOUSE', 'SOLD', 'PUBLIC', 'SALE', 'IMMEDIATE',
     4, 3, 320.00::numeric, 450.00::numeric, 2, 3, 2020,
     -25.2867, -57.5800, true,
     'propietario1@openroof.com', 'PY-AG-2018-001', 'comprador1@openroof.com', NULL, 'Villa Morra'),

    -- 2. Propiedad con INQUILINO asignado (Alquiler activo)
    ('Departamento amoblado en Carmelitas',
     'Moderno departamento de 2 dormitorios. Actualmente rentado.',
     'Calle Senador Long 456, Carmelitas',
     1200.00::numeric, 'APARTMENT', 'RENTED', 'PUBLIC', 'RENT', 'IMMEDIATE',
     2, 1, 85.00::numeric, 85.00::numeric, 1, 1, 2022,
     -25.2810, -57.5750, false,
     'propietario1@openroof.com', 'PY-AG-2018-001', NULL, 'inquilino1@openroof.com', 'Carmelitas'),

    -- 3. Propiedad disponible (Sin buyer ni tenant)
    ('Terreno amplio en Luque',
     'Excelente terreno de 1200 m² ideal para proyecto habitacional.',
     'Ruta Luque - San Bernardino Km 5',
     65000.00::numeric, 'LAND', 'PUBLISHED', 'PUBLIC', 'SALE', 'IMMEDIATE',
     0, 0, 0.00::numeric, 1200.00::numeric, 0, 0, NULL,
     -25.2700, -57.4800, false,
     'propietario2@openroof.com', 'PY-AG-2021-042', NULL, NULL, 'Luque'),

    -- 4. Oficina con nuevo INQUILINO asignado
    ('Oficina corporativa en el Centro',
     'Oficina de 150 m² en edificio corporativo. Contrato de alquiler iniciado.',
     'Calle Palma 789, Centro',
     2500.00::numeric, 'OFFICE', 'RENTED', 'PUBLIC', 'RENT', 'IMMEDIATE',
     0, 2, 150.00::numeric, 150.00::numeric, 3, 1, 2019,
     -25.2820, -57.6350, false,
     'propietario1@openroof.com', 'PY-AG-2018-001', NULL, 'inquilino1@openroof.com', 'Centro'),

    -- 5. Propiedad con COMPRADOR (En proceso de cierre)
    ('Penthouse en Manorá',
     'Espectacular penthouse de 200 m². Oferta aceptada por el comprador.',
     'Av. Santa Teresa 321, Manorá',
     350000.00::numeric, 'APARTMENT', 'SOLD', 'PUBLIC', 'SALE', 'IN_30_DAYS',
     3, 3, 200.00::numeric, 220.00::numeric, 2, 1, 2023,
     -25.2900, -57.5850, true,
     'propietario1@openroof.com', 'PY-AG-2018-001', 'comprador2@openroof.com', NULL, 'Manorá'),

    -- 6. Propiedad libre
    ('Casa familiar en Lambaré',
     'Acogedora casa de 3 dormitorios con patio grande.',
     'Barrio San Isidro, Lambaré',
     95000.00::numeric, 'HOUSE', 'PUBLISHED', 'PUBLIC', 'SALE', 'IMMEDIATE',
     3, 2, 180.00::numeric, 350.00::numeric, 1, 1, 2015,
     -25.3400, -57.6200, false,
     'propietario2@openroof.com', 'PY-AG-2021-042', NULL, NULL, 'Lambaré')

) AS p(
    title, description, address, price, property_type, status, visibility, category,
    availability, bedrooms, bathrooms, built_area, surface_area, parking_spaces,
    floors_count, construction_year, lat, lng, highlighted,
    owner_email, license_number, buyer_email, tenant_email, location_name
)
JOIN users           u  ON u.email           = p.owner_email
JOIN agent_profiles  ap ON ap.license_number = p.license_number
LEFT JOIN users      b  ON b.email           = p.buyer_email
LEFT JOIN users      t  ON t.email           = p.tenant_email
JOIN locations       l  ON l.name            = p.location_name
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE title = p.title);
-- ============================================================
-- 8. FAVORITOS
-- ============================================================
INSERT INTO favorites (property_id, user_id, created_at, updated_at, version)
SELECT p.id, u.id, NOW(), NOW(), 0
FROM (VALUES
    ('Casa moderna en Villa Morra',        'comprador1@openroof.com'),
    ('Penthouse en Manorá',                'comprador1@openroof.com'),
    ('Departamento amoblado en Carmelitas','comprador2@openroof.com'),
    ('Casa familiar en Lambaré',           'inquilino1@openroof.com')
) AS t(prop_title, user_email)
JOIN properties p ON p.title = t.prop_title
JOIN users      u ON u.email = t.user_email
WHERE NOT EXISTS (SELECT 1 FROM favorites LIMIT 1);

-- ============================================================
-- 9. REVIEWS DE AGENTES
-- ============================================================
INSERT INTO agent_reviews (rating, comment, user_id, agent_id, property_id, created_at, updated_at, version)
SELECT t.rating, t.comment, u.id, ap.id, p.id, NOW(), NOW(), 0
FROM (VALUES
    (5, 'Excelente atención, me ayudó a encontrar la casa perfecta. Muy profesional.',
     'comprador1@openroof.com', 'PY-AG-2018-001', 'Casa moderna en Villa Morra'),
    (4, 'Buena experiencia, respondió rápido a todas mis consultas.',
     'comprador2@openroof.com', 'PY-AG-2021-042', NULL),
    (5, 'La mejor agente que tuve. Muy dedicada y honesta.',
     'inquilino1@openroof.com', 'PY-AG-2021-042', NULL)
) AS t(rating, comment, user_email, license_number, prop_title)
JOIN users          u  ON u.email          = t.user_email
JOIN agent_profiles ap ON ap.license_number = t.license_number
LEFT JOIN properties p ON p.title           = t.prop_title
WHERE NOT EXISTS (SELECT 1 FROM agent_reviews LIMIT 1);

-- ============================================================
-- 10. MENSAJES
-- ============================================================
INSERT INTO messages (content, sender_id, receiver_id, property_id, created_at, updated_at, version)
SELECT t.content, s.id, r.id, p.id, t.sent_at, t.sent_at, 0
FROM (VALUES
    ('Hola, me interesa la casa en Villa Morra. ¿Podríamos agendar una visita?',
     'comprador1@openroof.com', 'agente1@openroof.com', 'Casa moderna en Villa Morra',
     (NOW() - INTERVAL '2 days')::timestamp),
    ('¡Por supuesto! ¿Le parece este sábado a las 10:00?',
     'agente1@openroof.com', 'comprador1@openroof.com', 'Casa moderna en Villa Morra',
     (NOW() - INTERVAL '1 day')::timestamp),
    ('Perfecto, ahí estaré. ¡Gracias!',
     'comprador1@openroof.com', 'agente1@openroof.com', 'Casa moderna en Villa Morra',
     (NOW() - INTERVAL '23 hours')::timestamp),
    ('Buenos días, ¿el departamento de Carmelitas acepta mascotas?',
     'comprador2@openroof.com', 'agente1@openroof.com', 'Departamento amoblado en Carmelitas',
     (NOW() - INTERVAL '3 hours')::timestamp)
) AS t(content, sender_email, receiver_email, prop_title, sent_at)
JOIN users       s ON s.email = t.sender_email
JOIN users       r ON r.email = t.receiver_email
JOIN properties  p ON p.title = t.prop_title
WHERE NOT EXISTS (SELECT 1 FROM messages LIMIT 1);

-- ============================================================
-- 11. VISITAS
-- ============================================================
INSERT INTO visits (scheduled_at, status, buyer_notes, property_id, buyer_id, agent_id, created_at, updated_at, version)
SELECT t.scheduled_at, t.status, t.buyer_notes, p.id, u.id, ap.id, NOW(), NOW(), 0
FROM (VALUES
    ((NOW() + INTERVAL '3 days')::timestamp,  'CONFIRMED',
     'Visita para ver la casa con su esposa',
     'Casa moderna en Villa Morra', 'comprador1@openroof.com', 'PY-AG-2018-001'),
    ((NOW() + INTERVAL '5 days')::timestamp, 'PENDING',
     'Primera visita al terreno - quiere medir',
     'Terreno amplio en Luque', 'comprador2@openroof.com', 'PY-AG-2021-042'),
    ((NOW() - INTERVAL '7 days')::timestamp, 'COMPLETED',
     'Visitó el depto y le gustó mucho. Pidió datos del contrato.',
     'Departamento amoblado en Carmelitas', 'inquilino1@openroof.com', 'PY-AG-2018-001')
) AS t(scheduled_at, status, buyer_notes, prop_title, buyer_email, license_number)
JOIN properties     p  ON p.title          = t.prop_title
JOIN users          u  ON u.email          = t.buyer_email
JOIN agent_profiles ap ON ap.license_number = t.license_number
WHERE NOT EXISTS (SELECT 1 FROM visits LIMIT 1);

-- ============================================================
-- 12. OFERTAS
-- ============================================================
INSERT INTO offers (amount, message, status, expires_at, property_id, buyer_id, created_at, updated_at, version)
SELECT t.amount, t.message, t.status, t.expires_at, p.id, u.id, NOW(), NOW(), 0
FROM (VALUES
    (270000.00::numeric,
     'Me encantó la propiedad, ofrezco USD 270.000. Pago al contado.',
     'NEGOTIATING',
     (NOW() + INTERVAL '15 days')::timestamp,
     'Casa moderna en Villa Morra', 'comprador1@openroof.com'),
    (60000.00::numeric,
     'Ofrezco 60.000 por el terreno. Financio 50% con banco.',
     'SENT',
     (NOW() + INTERVAL '10 days')::timestamp,
     'Terreno amplio en Luque', 'comprador2@openroof.com')
) AS t(amount, message, status, expires_at, prop_title, buyer_email)
JOIN properties p ON p.title = t.prop_title
JOIN users      u ON u.email = t.buyer_email
WHERE NOT EXISTS (SELECT 1 FROM offers LIMIT 1);

COMMIT;

-- ============================================================
-- VERIFICACIÓN RÁPIDA (correr después del seed)
-- ============================================================
SELECT 'exterior_features' AS tabla, COUNT(*) AS registros FROM exterior_features
UNION ALL SELECT 'interior_features', COUNT(*) FROM interior_features
UNION ALL SELECT 'locations',         COUNT(*) FROM locations
UNION ALL SELECT 'lead_statuses',     COUNT(*) FROM lead_statuses
UNION ALL SELECT 'users',             COUNT(*) FROM users
UNION ALL SELECT 'agent_profiles',    COUNT(*) FROM agent_profiles
UNION ALL SELECT 'agent_specialties', COUNT(*) FROM agent_specialties
UNION ALL SELECT 'agent_to_specialties', COUNT(*) FROM agent_to_specialties
UNION ALL SELECT 'properties',        COUNT(*) FROM properties
UNION ALL SELECT 'favorites',         COUNT(*) FROM favorites
UNION ALL SELECT 'agent_reviews',     COUNT(*) FROM agent_reviews
UNION ALL SELECT 'messages',          COUNT(*) FROM messages
UNION ALL SELECT 'visits',            COUNT(*) FROM visits
UNION ALL SELECT 'offers',            COUNT(*) FROM offers
ORDER BY 1;
