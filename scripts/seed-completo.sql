-- ============================================================
-- OpenRoof — SEED COMPLETO
-- Ejecutar en: Supabase Dashboard → SQL Editor
--
-- Incluye:
--   1. Catálogo: exterior_features, interior_features, locations
--   2. Lead statuses
--   3. Usuarios (Admin, 2 Agentes, 2 Owners, 2 Buyers, 1 Tenant)
--   4. Agent profiles
--   5. Propiedades (10 variadas en Paraguay)
--   6. Favoritos
--   7. Reviews de agentes
--   8. Mensajes
--   9. Visitas
--  10. Ofertas
--
-- Contraseña de todos los usuarios de prueba: Test1234!
-- BCrypt hash: $2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq
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
    ('Barrio Herrera',        'Asunción',               'Capital',     'Paraguay'),
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
-- ============================================================
INSERT INTO users (email, password_hash, name, phone, role, email_verified_at, created_at, updated_at, version)
SELECT email, password_hash, name, phone, role, NOW(), NOW(), NOW(), 0
FROM (VALUES
    ('admin@openroof.com',          '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Admin OpenRoof',    '+595981000001', 'ADMIN'),
    ('agente1@openroof.com',        '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Carlos Mendoza',    '+595981000002', 'AGENT'),
    ('agente2@openroof.com',        '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'María González',    '+595981000003', 'AGENT'),
    ('propietario1@openroof.com',   '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Roberto Fernández', '+595981000004', 'OWNER'),
    ('propietario2@openroof.com',   '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Ana Benítez',       '+595981000005', 'OWNER'),
    ('comprador1@openroof.com',     '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Luis Ramírez',      '+595981000006', 'BUYER'),
    ('comprador2@openroof.com',     '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Sofía Villalba',    '+595981000007', 'BUYER'),
    ('inquilino1@openroof.com',     '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Diego Acosta',      '+595981000008', 'TENANT')
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
-- 7. PROPIEDADES (10 propiedades variadas)
-- JOINs directos para resolver FKs — evita subqueries en VALUES
-- ============================================================
INSERT INTO properties (
    title, description, address, price, property_type, status, visibility, category,
    availability, bedrooms, bathrooms, built_area, surface_area, parking_spaces,
    floors_count, construction_year, lat, lng, highlighted,
    owner_id, agent_id, location_id, created_at, updated_at, version
)
SELECT
    p.title, p.description, p.address, p.price, p.property_type, p.status, p.visibility, p.category,
    p.availability, p.bedrooms, p.bathrooms, p.built_area, p.surface_area, p.parking_spaces,
    p.floors_count, p.construction_year, p.lat, p.lng, p.highlighted,
    u.id AS owner_id, ap.id AS agent_id, l.id AS location_id,
    NOW(), NOW(), 0
FROM (VALUES
    ('Casa moderna en Villa Morra',
     'Hermosa casa de 3 plantas con acabados de lujo, amplio jardín y piscina.',
     'Av. Mariscal López 1234, Villa Morra',
     285000.00::numeric, 'HOUSE', 'PUBLISHED', 'PUBLIC', 'SALE', 'IMMEDIATE',
     4, 3, 320.00::numeric, 450.00::numeric, 2, 3, 2020,
     -25.2867::double precision, -57.5800::double precision, true,
     'propietario1@openroof.com', 'PY-AG-2018-001', 'Villa Morra'),

    ('Departamento amoblado en Carmelitas',
     'Moderno departamento de 2 dormitorios, completamente amoblado. Edificio con seguridad 24hs, gimnasio y área social.',
     'Calle Senador Long 456, Carmelitas',
     1200.00::numeric, 'APARTMENT', 'PUBLISHED', 'PUBLIC', 'RENT', 'IMMEDIATE',
     2, 1, 85.00::numeric, 85.00::numeric, 1, 1, 2022,
     -25.2810::double precision, -57.5750::double precision, false,
     'propietario1@openroof.com', 'PY-AG-2018-001', 'Carmelitas'),

    ('Terreno amplio en Luque',
     'Excelente terreno de 1200 m² ideal para proyecto habitacional. Zona en crecimiento, acceso asfaltado.',
     'Ruta Luque - San Bernardino Km 5',
     65000.00::numeric, 'LAND', 'PUBLISHED', 'PUBLIC', 'SALE', 'IMMEDIATE',
     0, 0, 0.00::numeric, 1200.00::numeric, 0, 0, NULL,
     -25.2700::double precision, -57.4800::double precision, false,
     'propietario2@openroof.com', 'PY-AG-2021-042', 'Luque'),

    ('Oficina corporativa en el Centro',
     'Oficina de 150 m² en edificio corporativo con recepción, sala de reuniones y estacionamiento.',
     'Calle Palma 789, Centro',
     2500.00::numeric, 'OFFICE', 'PUBLISHED', 'PUBLIC', 'RENT', 'IN_30_DAYS',
     0, 2, 150.00::numeric, 150.00::numeric, 3, 1, 2019,
     -25.2820::double precision, -57.6350::double precision, false,
     'propietario1@openroof.com', 'PY-AG-2018-001', 'Centro'),

    ('Casa familiar en Lambaré',
     'Acogedora casa de 3 dormitorios con patio grande, ideal para familia. Barrio tranquilo y seguro.',
     'Barrio San Isidro, Lambaré',
     95000.00::numeric, 'HOUSE', 'PUBLISHED', 'PUBLIC', 'SALE', 'IMMEDIATE',
     3, 2, 180.00::numeric, 350.00::numeric, 1, 1, 2015,
     -25.3400::double precision, -57.6200::double precision, false,
     'propietario2@openroof.com', 'PY-AG-2021-042', 'Lambaré'),

    ('Penthouse en Manorá',
     'Espectacular penthouse de 200 m² con vista panorámica. 3 suites, jacuzzi, terraza privada.',
     'Av. Santa Teresa 321, Manorá',
     350000.00::numeric, 'APARTMENT', 'PUBLISHED', 'PUBLIC', 'SALE', 'IN_30_DAYS',
     3, 3, 200.00::numeric, 220.00::numeric, 2, 1, 2023,
     -25.2900::double precision, -57.5850::double precision, true,
     'propietario1@openroof.com', 'PY-AG-2018-001', 'Manorá'),

    ('Depósito industrial - Fernando de la Mora',
     'Depósito de 500 m² con portón para camiones, oficina administrativa y baño. Ideal para logística.',
     'Ruta 2 Mcal. Estigarribia Km 12',
     3500.00::numeric, 'WAREHOUSE', 'PUBLISHED', 'PUBLIC', 'RENT', 'IMMEDIATE',
     0, 1, 500.00::numeric, 600.00::numeric, 5, 1, 2010,
     -25.3350::double precision, -57.5500::double precision, false,
     'propietario2@openroof.com', 'PY-AG-2021-042', 'Fernando de la Mora'),

    ('Casa con local comercial - San Lorenzo',
     'Propiedad mixta: casa de 3 dormitorios arriba y local comercial abajo. Sobre avenida principal.',
     'Av. Libertad 567, San Lorenzo',
     120000.00::numeric, 'HOUSE', 'PUBLISHED', 'PUBLIC', 'SALE_OR_RENT', 'TO_NEGOTIATE',
     3, 2, 250.00::numeric, 300.00::numeric, 1, 2, 2012,
     -25.3370::double precision, -57.5090::double precision, false,
     'propietario1@openroof.com', 'PY-AG-2018-001', 'San Lorenzo'),

    ('Casa nueva en Barrio Herrera',
     'Casa a estrenar de 4 dormitorios en suite, living comedor amplio, cocina con isla. Piscina y quincho.',
     'Calle Dr. Morra 890, Barrio Herrera',
     195000.00::numeric, 'HOUSE', 'PENDING', 'PRIVATE', 'SALE', 'IN_60_DAYS',
     4, 4, 280.00::numeric, 400.00::numeric, 2, 2, 2025,
     -25.2750::double precision, -57.5900::double precision, false,
     'propietario2@openroof.com', 'PY-AG-2021-042', 'Barrio Herrera'),

    ('Estancia ganadera - 50 hectáreas',
     'Campo ganadero con casa patronal, corral, aguada natural. 50 hectáreas de pasturas. Título perfecto.',
     'Compañía Isla Hugua, Encarnación',
     180000.00::numeric, 'FARM', 'PUBLISHED', 'PUBLIC', 'SALE', 'IMMEDIATE',
     3, 1, 200.00::numeric, 500000.00::numeric, 0, 1, 1995,
     -27.3300::double precision, -55.8660::double precision, false,
     'propietario2@openroof.com', 'PY-AG-2021-042', 'Encarnación')

) AS p(
    title, description, address, price, property_type, status, visibility, category,
    availability, bedrooms, bathrooms, built_area, surface_area, parking_spaces,
    floors_count, construction_year, lat, lng, highlighted,
    owner_email, license_number, location_name
)
JOIN users           u  ON u.email          = p.owner_email
JOIN agent_profiles  ap ON ap.license_number = p.license_number
JOIN locations       l  ON l.name            = p.location_name
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE title = 'Casa moderna en Villa Morra');

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
UNION ALL SELECT 'properties',        COUNT(*) FROM properties
UNION ALL SELECT 'favorites',         COUNT(*) FROM favorites
UNION ALL SELECT 'agent_reviews',     COUNT(*) FROM agent_reviews
UNION ALL SELECT 'messages',          COUNT(*) FROM messages
UNION ALL SELECT 'visits',            COUNT(*) FROM visits
UNION ALL SELECT 'offers',            COUNT(*) FROM offers
ORDER BY 1;
