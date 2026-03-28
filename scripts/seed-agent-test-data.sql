-- ============================================================
-- SEED DATA: Datos de prueba completos para el agente de test
-- Ejecutar manualmente contra la BD de desarrollo/test
--
-- Prerequisito: el seed 009 ya debe estar aplicado
--   (crea agent.test@openroof.com user_id=200, agent_profile_id=200
--    y juan.perez@test.com user_id=201 con su relación)
--
-- Resultado esperado en el dashboard de agent.test@openroof.com:
--   activeClients  = 3  (users 201, 203, 204)
--   totalSales     = 1  (property 300 → SOLD, agent_id=200)
--   scheduledVisits= 2  (visit_request 300 PENDING + 301 ACCEPTED)
--   commissions    = 7500  (3% de contrato 300 SIGNED amount=250000)
-- ============================================================

BEGIN;

-- ============================================================
-- 1. USUARIOS
-- ============================================================

-- Propietario de las propiedades de prueba
INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at, version)
SELECT 202, 'owner.test@openroof.com',
       '$2a$10$w3oa2wou4oTUPuafWGrR.ezomBs0tjgLZYL4Nd6ccvQ5mIiMDCYsS',
       'Propietario de Prueba', 'OWNER', now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 202 OR email = 'owner.test@openroof.com');

-- Cliente adicional: María García
INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at, version)
SELECT 203, 'maria.garcia@test.com',
       '$2a$10$w3oa2wou4oTUPuafWGrR.ezomBs0tjgLZYL4Nd6ccvQ5mIiMDCYsS',
       'María García', 'USER', now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 203 OR email = 'maria.garcia@test.com');

-- Cliente adicional: Carlos López
INSERT INTO users (id, email, password_hash, name, role, created_at, updated_at, version)
SELECT 204, 'carlos.lopez@test.com',
       '$2a$10$w3oa2wou4oTUPuafWGrR.ezomBs0tjgLZYL4Nd6ccvQ5mIiMDCYsS',
       'Carlos López', 'USER', now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 204 OR email = 'carlos.lopez@test.com');

-- ============================================================
-- 2. RELACIONES AGENTE-CLIENTE ADICIONALES (agent_id=200)
-- ============================================================

INSERT INTO agent_clients (
    agent_id, user_id, priority, status, client_type,
    preferred_contact_method, interactions_count, visited_properties_count, offers_count,
    min_budget, max_budget, min_bedrooms, max_bedrooms, min_bathrooms, max_bathrooms,
    occupation, annual_income, source_channel,
    preferred_property_types, preferred_areas, tags,
    is_searching_property, created_at, updated_at, version
)
SELECT
    200, 203, 'MEDIUM', 'ACTIVE', 'BUYER',
    'WHATSAPP', 6, 2, 1,
    150000, 250000, 2, 3, 1, 2,
    'Arquitecta', 65000, 'Instagram',
    '["Apartamento"]', '["Villa Morra", "Carmelitas"]',
    '["Primer Contacto", "Interesada en Alquiler"]',
    true, now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM agent_clients WHERE agent_id = 200 AND user_id = 203);

INSERT INTO agent_clients (
    agent_id, user_id, priority, status, client_type,
    preferred_contact_method, interactions_count, visited_properties_count, offers_count,
    min_budget, max_budget, min_bedrooms, max_bedrooms, min_bathrooms, max_bathrooms,
    occupation, annual_income, source_channel,
    preferred_property_types, preferred_areas, tags,
    is_searching_property, created_at, updated_at, version
)
SELECT
    200, 204, 'LOW', 'INACTIVE', 'SELLER',
    'PHONE', 2, 0, 0,
    300000, 500000, 3, 5, 2, 4,
    'Empresario', 120000, 'Referral',
    '["Casa", "Terreno"]', '["Luque", "San Lorenzo"]',
    '["Cliente Frío", "Recontactar en 3 meses"]',
    false, now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM agent_clients WHERE agent_id = 200 AND user_id = 204);

-- ============================================================
-- 3. UBICACIÓN DE PRUEBA
-- ============================================================

INSERT INTO locations (id, name, city, department, country, created_at, updated_at, version)
SELECT 500, 'Las Mercedes, Asunción', 'Asunción', 'Central', 'Paraguay', now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM locations WHERE id = 500);

-- ============================================================
-- 4. PROPIEDADES (owner_id=202, agent_id=200)
-- ============================================================

-- Property 300: SOLD → contribuye a totalSales del dashboard
INSERT INTO properties (
    id, owner_id, agent_id, location_id,
    title, description, category, property_type, address,
    price, bedrooms, bathrooms, full_bathrooms, half_bathrooms,
    surface_area, built_area, parking_spaces, floors_count,
    status, visibility, availability,
    highlighted, view_count, favorite_count,
    created_at, updated_at, version
)
SELECT
    300, 202, 200, 500,
    'Casa residencial en Las Mercedes',
    'Casa de 3 dormitorios con jardín y garaje en zona exclusiva.',
    'SALE', 'HOUSE', 'Las Mercedes 450, Asunción',
    250000.00, 3, 2.0, 2, 0,
    600.00, 220.00, 2, 1,
    'SOLD', 'PUBLIC', 'IMMEDIATE',
    false, 150, 12,
    now() - interval '60 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE id = 300);

-- Property 301: PUBLISHED, APARTMENT → para visita pendiente y contrato DRAFT
INSERT INTO properties (
    id, owner_id, agent_id, location_id,
    title, description, category, property_type, address,
    price, bedrooms, bathrooms, full_bathrooms, half_bathrooms,
    surface_area, built_area, parking_spaces, floors_count,
    status, visibility, availability,
    highlighted, view_count, favorite_count,
    created_at, updated_at, version
)
SELECT
    301, 202, 200, 500,
    'Apartamento moderno en Las Mercedes',
    'Apartamento de 2 dormitorios con vista panorámica, piscina y gimnasio.',
    'SALE', 'APARTMENT', 'Av. Las Mercedes 1200 Piso 5, Asunción',
    180000.00, 2, 2.0, 2, 0,
    120.00, 110.00, 1, 1,
    'PUBLISHED', 'PUBLIC', 'IMMEDIATE',
    true, 320, 28,
    now() - interval '30 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE id = 301);

-- Property 302: PUBLISHED, HOUSE → para visita aceptada y contrato PARTIALLY_SIGNED
INSERT INTO properties (
    id, owner_id, agent_id, location_id,
    title, description, category, property_type, address,
    price, bedrooms, bathrooms, full_bathrooms, half_bathrooms,
    surface_area, built_area, parking_spaces, floors_count,
    status, visibility, availability,
    highlighted, view_count, favorite_count,
    created_at, updated_at, version
)
SELECT
    302, 202, 200, 500,
    'Casa familiar con jardín amplio',
    'Casa de 4 dormitorios con jardín de 400m², ideal para familias.',
    'RENT', 'HOUSE', 'Calle Palmas 75, Asunción',
    320000.00, 4, 3.0, 2, 1,
    800.00, 350.00, 2, 2,
    'PUBLISHED', 'PUBLIC', 'TO_NEGOTIATE',
    false, 95, 7,
    now() - interval '15 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE id = 302);

-- ============================================================
-- 5. ASIGNACIONES DE PROPIEDADES (agent_id=200, assigned_by=202)
-- ============================================================

INSERT INTO property_assignments (id, property_id, agent_id, assigned_by, status, assigned_at, created_at, updated_at, version)
SELECT 300, 300, 200, 202, 'ACCEPTED', now() - interval '60 days', now() - interval '60 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM property_assignments WHERE id = 300);

INSERT INTO property_assignments (id, property_id, agent_id, assigned_by, status, assigned_at, created_at, updated_at, version)
SELECT 301, 301, 200, 202, 'ACCEPTED', now() - interval '30 days', now() - interval '30 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM property_assignments WHERE id = 301);

INSERT INTO property_assignments (id, property_id, agent_id, assigned_by, status, assigned_at, created_at, updated_at, version)
SELECT 302, 302, 200, 202, 'ACCEPTED', now() - interval '15 days', now() - interval '15 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM property_assignments WHERE id = 302);

-- ============================================================
-- 6. CONTRATOS (seller_id=200 = user id del agente)
-- ============================================================

-- Contrato 300: SIGNED → suma a totalSold y comisiones (3% de 250000 = 7500)
INSERT INTO contracts (id, property_id, buyer_id, seller_id, contract_type, status, amount, start_date, end_date, terms, created_at, updated_at, version)
SELECT 300, 300, 201, 200, 'SALE', 'SIGNED', 250000.00,
       '2026-01-15', '2026-02-15',
       'Pago al contado. Entrega inmediata de llaves. Libre de gravámenes.',
       now() - interval '45 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE id = 300);

-- Contrato 301: DRAFT → aparece como contrato activo
INSERT INTO contracts (id, property_id, buyer_id, seller_id, contract_type, status, amount, terms, created_at, updated_at, version)
SELECT 301, 301, 203, 200, 'SALE', 'DRAFT', 180000.00,
       'Financiado al 30%. Sujeto a aprobación bancaria.',
       now() - interval '10 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE id = 301);

-- Contrato 302: PARTIALLY_SIGNED → aparece como contrato activo
INSERT INTO contracts (id, property_id, buyer_id, seller_id, contract_type, status, amount, start_date, end_date, terms, created_at, updated_at, version)
SELECT 302, 302, 204, 200, 'RENT', 'PARTIALLY_SIGNED', 320000.00,
       '2026-04-01', '2027-04-01',
       'Alquiler anual. Depósito equivalente a 2 meses. Revisión de precio cada 6 meses.',
       now() - interval '5 days', now(), 1
WHERE NOT EXISTS (SELECT 1 FROM contracts WHERE id = 302);

-- ============================================================
-- 7. SOLICITUDES DE VISITA (agent_id=200 = AgentProfile id)
-- ============================================================

-- Visita 300: PENDING → contribuye a scheduledVisits
INSERT INTO visit_requests (
    id, property_id, buyer_id, agent_id,
    proposed_at, status,
    buyer_name, buyer_email, buyer_phone, message,
    created_at, updated_at, version
)
SELECT
    300, 301, 201, 200,
    now() + interval '7 days', 'PENDING',
    'Juan Pérez', 'juan.perez@test.com', '+595981123456',
    'Me interesa conocer el apartamento, especialmente las amenidades del edificio.',
    now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM visit_requests WHERE property_id = 301 AND buyer_id = 201 AND agent_id = 200);

-- Visita 301: ACCEPTED → contribuye a scheduledVisits
INSERT INTO visit_requests (
    id, property_id, buyer_id, agent_id,
    proposed_at, status,
    buyer_name, buyer_email, buyer_phone, message,
    created_at, updated_at, version
)
SELECT
    301, 302, 203, 200,
    now() + interval '14 days', 'ACCEPTED',
    'María García', 'maria.garcia@test.com', '+595982654321',
    'Quiero ver el jardín y la distribución de los dormitorios.',
    now(), now(), 1
WHERE NOT EXISTS (SELECT 1 FROM visit_requests WHERE property_id = 302 AND buyer_id = 203 AND agent_id = 200);

COMMIT;

-- ============================================================
-- VERIFICACIÓN
-- ============================================================
SELECT 'users creados'           AS check, COUNT(*) FROM users          WHERE id IN (202, 203, 204)
UNION ALL
SELECT 'agent_clients del ag200', COUNT(*) FROM agent_clients           WHERE agent_id = 200
UNION ALL
SELECT 'propiedades del ag200',   COUNT(*) FROM properties              WHERE agent_id = 200
UNION ALL
SELECT 'assignments ACCEPTED',    COUNT(*) FROM property_assignments    WHERE agent_id = 200 AND status = 'ACCEPTED'
UNION ALL
SELECT 'contratos del ag200',     COUNT(*) FROM contracts               WHERE seller_id = 200
UNION ALL
SELECT 'visitas pendientes/acep', COUNT(*) FROM visit_requests          WHERE agent_id = 200 AND status IN ('PENDING','ACCEPTED');
