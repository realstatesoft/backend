-- ============================================================
-- OpenRoof — SEED AGENTES ADICIONALES (20 agentes)
-- Ejecutar en: Supabase Dashboard → SQL Editor
--
-- IMPORTANTE: Ejecutar DESPUÉS de seed-completo.sql
-- Contraseña de todos: Test1234!
-- ============================================================

BEGIN;

-- ============================================================
-- 1. USUARIOS AGENTES ADICIONALES
-- ============================================================
INSERT INTO users (email, password_hash, name, phone, role, email_verified_at, created_at, updated_at, version)
SELECT email, password_hash, name, phone, role, NOW(), NOW(), NOW(), 0
FROM (VALUES
    ('agente3@openroof.com',  '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Pedro Sánchez',      '+595981100001', 'AGENT'),
    ('agente4@openroof.com',  '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Laura Martínez',     '+595981100002', 'AGENT'),
    ('agente5@openroof.com',  '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Jorge Rodríguez',    '+595981100003', 'AGENT'),
    ('agente6@openroof.com',  '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Gabriela López',     '+595981100004', 'AGENT'),
    ('agente7@openroof.com',  '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Fernando Díaz',      '+595981100005', 'AGENT'),
    ('agente8@openroof.com',  '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Patricia Rojas',     '+595981100006', 'AGENT'),
    ('agente9@openroof.com',  '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Andrés Moreno',      '+595981100007', 'AGENT'),
    ('agente10@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Claudia Vega',       '+595981100008', 'AGENT'),
    ('agente11@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Ricardo Torres',     '+595981100009', 'AGENT'),
    ('agente12@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Verónica Castro',    '+595981100010', 'AGENT'),
    ('agente13@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Martín Aguilar',     '+595981100011', 'AGENT'),
    ('agente14@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Carolina Peralta',   '+595981100012', 'AGENT'),
    ('agente15@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Sebastián Ruiz',     '+595981100013', 'AGENT'),
    ('agente16@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Natalia Giménez',    '+595981100014', 'AGENT'),
    ('agente17@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Gustavo Cabrera',    '+595981100015', 'AGENT'),
    ('agente18@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Valeria Núñez',      '+595981100016', 'AGENT'),
    ('agente19@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Alejandro Paredes',  '+595981100017', 'AGENT'),
    ('agente20@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Lorena Espínola',    '+595981100018', 'AGENT'),
    ('agente21@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Rodrigo Aquino',     '+595981100019', 'AGENT'),
    ('agente22@openroof.com', '$2b$10$8V/4wzSa89/M00VwiQVP7.Fb1K83rR601duFtfxCg5Zo6gZ3rSiOq', 'Mónica Fleitas',     '+595981100020', 'AGENT')
) AS t(email, password_hash, name, phone, role)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'agente3@openroof.com');

-- ============================================================
-- 2. AGENT PROFILES
-- ============================================================

-- Agente 3: Pedro Sánchez - Casas de lujo
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especialista en propiedades de lujo y residencias exclusivas en los mejores barrios de Asunción.',
    'Sánchez Premium Real Estate', 12, 'PY-AG-2022-003', 4.9, 45, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente3@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-003');

-- Agente 4: Laura Martínez - Departamentos y apartamentos
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Experta en departamentos modernos y torres residenciales. Tu mejor opción para encontrar el depto ideal.',
    'Martínez Apartments', 6, 'PY-AG-2022-004', 4.6, 32, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente4@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-004');

-- Agente 5: Jorge Rodríguez - Terrenos y lotes
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especializado en venta de terrenos, loteamientos y proyectos de urbanización en todo Paraguay.',
    'Rodríguez Tierras', 15, 'PY-AG-2022-005', 4.8, 67, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente5@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-005');

-- Agente 6: Gabriela López - Comercial y oficinas
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Asesora corporativa en locales comerciales, oficinas y espacios de coworking.',
    'López Commercial', 9, 'PY-AG-2022-006', 4.7, 28, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente6@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-006');

-- Agente 7: Fernando Díaz - Industrial y depósitos
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especialista en naves industriales, galpones y centros logísticos.',
    'Díaz Industrial', 11, 'PY-AG-2022-007', 4.5, 19, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente7@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-007');

-- Agente 8: Patricia Rojas - Rural y campos
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Experta en propiedades rurales, estancias ganaderas y campos agrícolas en todo el Chaco y Región Oriental.',
    'Rojas Campos Py', 18, 'PY-AG-2022-008', 4.9, 52, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente8@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-008');

-- Agente 9: Andrés Moreno - Residencial familiar
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Ayudo a familias a encontrar su hogar ideal. Especializado en casas familiares y barrios tranquilos.',
    'Moreno Family Homes', 7, 'PY-AG-2022-009', 4.8, 41, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente9@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-009');

-- Agente 10: Claudia Vega - Inversiones inmobiliarias
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especialista en oportunidades de inversión, rentabilidad y desarrollo inmobiliario.',
    'Vega Investments', 10, 'PY-AG-2022-010', 4.7, 36, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente10@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-010');

-- Agente 11: Ricardo Torres - Encarnación
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'El experto local en Encarnación e Itapúa. Casas, terrenos y propiedades turísticas.',
    'Torres Inmobiliaria Encarnación', 14, 'PY-AG-2022-011', 4.8, 58, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente11@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-011');

-- Agente 12: Verónica Castro - Ciudad del Este
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especialista en propiedades comerciales y residenciales en Ciudad del Este y Alto Paraná.',
    'Castro CDE Properties', 8, 'PY-AG-2022-012', 4.6, 29, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente12@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-012');

-- Agente 13: Martín Aguilar - Casas nuevas y construcción
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Trabajo con desarrolladores y constructoras. Especializado en casas a estrenar y preventa.',
    'Aguilar Desarrollos', 6, 'PY-AG-2022-013', 4.5, 22, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente13@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-013');

-- Agente 14: Carolina Peralta - Alquileres
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Experta en administración de alquileres y búsqueda de inquilinos calificados.',
    'Peralta Rentals', 5, 'PY-AG-2022-014', 4.7, 47, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente14@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-014');

-- Agente 15: Sebastián Ruiz - Lujo y exclusivos
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Propiedades exclusivas para clientes exigentes. Mansiones, penthouses y residencias de élite.',
    'Ruiz Luxury Homes', 13, 'PY-AG-2022-015', 5.0, 18, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente15@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-015');

-- Agente 16: Natalia Giménez - Departamentos céntricos
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especialista en departamentos en el centro de Asunción. Monoambientes, lofts y studios.',
    'Giménez Urbano', 4, 'PY-AG-2022-016', 4.4, 15, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente16@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-016');

-- Agente 17: Gustavo Cabrera - Gran Asunción
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Conocedor de Lambaré, San Lorenzo, Luque y Fernando de la Mora. Casas y terrenos accesibles.',
    'Cabrera Gran Asunción', 9, 'PY-AG-2022-017', 4.6, 38, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente17@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-017');

-- Agente 18: Valeria Núñez - Propiedades turísticas
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especialista en propiedades para turismo y Airbnb. Encarnación, San Bernardino y zonas turísticas.',
    'Núñez Tourism Properties', 7, 'PY-AG-2022-018', 4.8, 31, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente18@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-018');

-- Agente 19: Alejandro Paredes - Chaco
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Experto en campos del Chaco paraguayo. Ganadería, agricultura y tierras vírgenes.',
    'Paredes Chaco Lands', 20, 'PY-AG-2022-019', 4.9, 24, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente19@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-019');

-- Agente 20: Lorena Espínola - Primera vivienda
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Ayudo a jóvenes y familias a comprar su primera casa. Financiamiento y opciones accesibles.',
    'Espínola First Home', 5, 'PY-AG-2022-020', 4.7, 42, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente20@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-020');

-- Agente 21: Rodrigo Aquino - Barrios cerrados
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Especialista en condominios y barrios cerrados. Seguridad y calidad de vida para tu familia.',
    'Aquino Country Living', 8, 'PY-AG-2022-021', 4.8, 35, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente21@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-021');

-- Agente 22: Mónica Fleitas - Locales comerciales
INSERT INTO agent_profiles (user_id, bio, company_name, experience_years, license_number, avg_rating, total_reviews, created_at, updated_at, version)
SELECT u.id, 'Experta en locales para emprendimientos, franquicias y negocios. Shopping, galerías y calles comerciales.',
    'Fleitas Commercial', 11, 'PY-AG-2022-022', 4.6, 27, NOW(), NOW(), 0
FROM users u WHERE u.email = 'agente22@openroof.com'
AND NOT EXISTS (SELECT 1 FROM agent_profiles WHERE license_number = 'PY-AG-2022-022');

-- ============================================================
-- 3. AGENT SPECIALTIES (asegurar que existan todas)
-- ============================================================
INSERT INTO agent_specialties (name, created_at, updated_at, version)
SELECT name, NOW(), NOW(), 0
FROM (VALUES
    ('residential'),
    ('land'),
    ('commercial'),
    ('alquileres'),
    ('turismo'),
    ('primera-vivienda'),
    ('barrios-cerrados')
) AS t(name)
WHERE NOT EXISTS (SELECT 1 FROM agent_specialties WHERE agent_specialties.name = t.name);

-- ============================================================
-- 4. AGENT TO SPECIALTIES (asignar especialidades)
-- ============================================================

-- Agente 3: Pedro Sánchez - lujo, casas, residencial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-003' AND s.name IN ('lujo', 'casas', 'residencial')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 4: Laura Martínez - departamentos, apartamentos, residencial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-004' AND s.name IN ('departamentos', 'apartamentos', 'residencial')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 5: Jorge Rodríguez - terrenos, lotes, land
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-005' AND s.name IN ('terrenos', 'lotes', 'land')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 6: Gabriela López - comercial, oficinas, commercial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-006' AND s.name IN ('comercial', 'oficinas', 'commercial')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 7: Fernando Díaz - industrial, depósitos, bodegas
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-007' AND s.name IN ('industrial', 'depósitos', 'bodegas')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 8: Patricia Rojas - rural, campos, estancias, agrícola
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-008' AND s.name IN ('rural', 'campos', 'estancias', 'agrícola')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 9: Andrés Moreno - residencial, casas
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-009' AND s.name IN ('residencial', 'casas', 'residential')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 10: Claudia Vega - inversiones, comercial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-010' AND s.name IN ('inversiones', 'comercial')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 11: Ricardo Torres - residencial, casas, terrenos
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-011' AND s.name IN ('residencial', 'casas', 'terrenos', 'turismo')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 12: Verónica Castro - comercial, residencial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-012' AND s.name IN ('comercial', 'residencial', 'casas')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 13: Martín Aguilar - residencial, casas
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-013' AND s.name IN ('residencial', 'casas')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 14: Carolina Peralta - alquileres, departamentos, residencial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-014' AND s.name IN ('alquileres', 'departamentos', 'residencial', 'casas')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 15: Sebastián Ruiz - lujo, casas, residencial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-015' AND s.name IN ('lujo', 'casas', 'residencial', 'departamentos')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 16: Natalia Giménez - departamentos, apartamentos
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-016' AND s.name IN ('departamentos', 'apartamentos', 'residencial')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 17: Gustavo Cabrera - casas, terrenos, residencial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-017' AND s.name IN ('casas', 'terrenos', 'residencial')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 18: Valeria Núñez - turismo, alquileres, casas
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-018' AND s.name IN ('turismo', 'alquileres', 'casas', 'departamentos')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 19: Alejandro Paredes - rural, campos, agrícola
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-019' AND s.name IN ('rural', 'campos', 'agrícola', 'estancias')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 20: Lorena Espínola - primera-vivienda, casas, residencial
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-020' AND s.name IN ('primera-vivienda', 'casas', 'residencial', 'departamentos')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 21: Rodrigo Aquino - barrios-cerrados, casas, residencial, lujo
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-021' AND s.name IN ('barrios-cerrados', 'casas', 'residencial', 'lujo')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

-- Agente 22: Mónica Fleitas - comercial, oficinas
INSERT INTO agent_to_specialties (agent_id, specialty_id)
SELECT ap.id, s.id FROM agent_profiles ap CROSS JOIN agent_specialties s
WHERE ap.license_number = 'PY-AG-2022-022' AND s.name IN ('comercial', 'oficinas', 'commercial')
AND NOT EXISTS (SELECT 1 FROM agent_to_specialties ats WHERE ats.agent_id = ap.id AND ats.specialty_id = s.id);

COMMIT;

-- ============================================================
-- RESUMEN DE AGENTES CREADOS:
-- ============================================================
-- | #  | Nombre              | Especialidades                           | Rating |
-- |----|---------------------|------------------------------------------|--------|
-- | 3  | Pedro Sánchez       | lujo, casas, residencial                 | 4.9    |
-- | 4  | Laura Martínez      | departamentos, apartamentos              | 4.6    |
-- | 5  | Jorge Rodríguez     | terrenos, lotes                          | 4.8    |
-- | 6  | Gabriela López      | comercial, oficinas                      | 4.7    |
-- | 7  | Fernando Díaz       | industrial, depósitos, bodegas           | 4.5    |
-- | 8  | Patricia Rojas      | rural, campos, estancias, agrícola       | 4.9    |
-- | 9  | Andrés Moreno       | residencial, casas                       | 4.8    |
-- | 10 | Claudia Vega        | inversiones, comercial                   | 4.7    |
-- | 11 | Ricardo Torres      | residencial, casas, terrenos (Encarnación)| 4.8   |
-- | 12 | Verónica Castro     | comercial, residencial (CDE)             | 4.6    |
-- | 13 | Martín Aguilar      | residencial, casas (nuevas)              | 4.5    |
-- | 14 | Carolina Peralta    | alquileres, departamentos                | 4.7    |
-- | 15 | Sebastián Ruiz      | lujo, casas, departamentos               | 5.0    |
-- | 16 | Natalia Giménez     | departamentos, apartamentos              | 4.4    |
-- | 17 | Gustavo Cabrera     | casas, terrenos (Gran Asunción)          | 4.6    |
-- | 18 | Valeria Núñez       | turismo, alquileres                      | 4.8    |
-- | 19 | Alejandro Paredes   | rural, campos, agrícola (Chaco)          | 4.9    |
-- | 20 | Lorena Espínola     | primera-vivienda, casas                  | 4.7    |
-- | 21 | Rodrigo Aquino      | barrios-cerrados, casas, lujo            | 4.8    |
-- | 22 | Mónica Fleitas      | comercial, oficinas                      | 4.6    |
-- ============================================================
