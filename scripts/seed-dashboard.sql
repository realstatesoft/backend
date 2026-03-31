-- ============================================================
-- SEED DATA PARA DASHBOARDS (Agent + Owner)
-- Ejecutar DESPUÉS de seed-completo.sql
-- Puebla: agent_clients, visit_requests, contracts,
--         property_views, agent_agenda, notifications
-- ============================================================

BEGIN;

-- ============================================================
-- 1. AGENT CLIENTS (relación agente-cliente)
-- ============================================================
INSERT INTO agent_clients (agent_id, user_id, status, priority, client_type, notes,
                           visited_properties_count, offers_count, last_contact_date,
                           created_at, updated_at, version)
SELECT ap.id, u.id, t.status, t.priority, t.client_type, t.notes,
       t.visited, t.offers, t.last_contact, NOW(), NOW(), 0
FROM (VALUES
    ('PY-AG-2018-001', 'comprador1@openroof.com', 'ACTIVE', 'HIGH', 'BUYER',
     'Interesado en propiedades de lujo en Villa Morra y Manorá', 3, 1,
     (NOW() - INTERVAL '1 day')::timestamp),
    ('PY-AG-2018-001', 'comprador2@openroof.com', 'ACTIVE', 'MEDIUM', 'BUYER',
     'Busca departamento para inversión', 2, 1,
     (NOW() - INTERVAL '3 days')::timestamp),
    ('PY-AG-2018-001', 'inquilino1@openroof.com', 'ACTIVE', 'LOW', 'TENANT',
     'Inquilino actual del depto en Carmelitas', 1, 0,
     (NOW() - INTERVAL '7 days')::timestamp),
    ('PY-AG-2021-042', 'comprador2@openroof.com', 'ACTIVE', 'HIGH', 'BUYER',
     'Interesado en terreno en Luque para proyecto', 1, 1,
     (NOW() - INTERVAL '2 days')::timestamp),
    ('PY-AG-2021-042', 'comprador1@openroof.com', 'ACTIVE', 'MEDIUM', 'BUYER',
     'Cliente referido por agente1', 0, 0,
     (NOW() - INTERVAL '5 days')::timestamp)
) AS t(license_number, user_email, status, priority, client_type, notes, visited, offers, last_contact)
JOIN agent_profiles ap ON ap.license_number = t.license_number
JOIN users u ON u.email = t.user_email
WHERE NOT EXISTS (
    SELECT 1 FROM agent_clients ac
    WHERE ac.agent_id = ap.id AND ac.user_id = u.id
);

-- ============================================================
-- 2. VISIT REQUESTS (solicitudes de visita)
-- ============================================================
INSERT INTO visit_requests (property_id, buyer_id, agent_id, proposed_at, status,
                            buyer_name, buyer_email, buyer_phone, message,
                            created_at, updated_at, version)
SELECT p.id, u.id, ap.id, t.proposed_at, t.status,
       t.buyer_name, t.b_email, t.buyer_phone, t.message,
       t.created, t.created, 0
FROM (VALUES
    -- Solicitudes para agente1
    ('Casa moderna en Villa Morra', 'comprador1@openroof.com', 'PY-AG-2018-001',
     (NOW() + INTERVAL '2 days')::timestamp, 'ACCEPTED',
     'Luis Ramírez', 'comprador1@openroof.com', '+595981123456',
     'Me gustaría visitar la casa este fin de semana',
     (NOW() - INTERVAL '5 days')::timestamp),

    ('Departamento amoblado en Carmelitas', 'comprador2@openroof.com', 'PY-AG-2018-001',
     (NOW() + INTERVAL '4 days')::timestamp, 'PENDING',
     'Sofía Villalba', 'comprador2@openroof.com', '+595982654321',
     'Quisiera ver el departamento, ¿está disponible el martes?',
     (NOW() - INTERVAL '2 days')::timestamp),

    ('Penthouse en Manorá', 'comprador1@openroof.com', 'PY-AG-2018-001',
     (NOW() - INTERVAL '10 days')::timestamp, 'ACCEPTED',
     'Luis Ramírez', 'comprador1@openroof.com', '+595981123456',
     'Visita al penthouse para segunda revisión',
     (NOW() - INTERVAL '15 days')::timestamp),

    ('Oficina corporativa en el Centro', 'inquilino1@openroof.com', 'PY-AG-2018-001',
     (NOW() - INTERVAL '20 days')::timestamp, 'ACCEPTED',
     'Diego Acosta', 'inquilino1@openroof.com', '+595984111222',
     'Primera visita a la oficina',
     (NOW() - INTERVAL '25 days')::timestamp),

    -- Solicitudes para agente2
    ('Terreno amplio en Luque', 'comprador2@openroof.com', 'PY-AG-2021-042',
     (NOW() + INTERVAL '3 days')::timestamp, 'PENDING',
     'Sofía Villalba', 'comprador2@openroof.com', '+595982654321',
     'Quiero ver el terreno y medir para proyecto',
     (NOW() - INTERVAL '1 day')::timestamp),

    ('Casa familiar en Lambaré', 'comprador1@openroof.com', 'PY-AG-2021-042',
     (NOW() + INTERVAL '5 days')::timestamp, 'PENDING',
     'Luis Ramírez', 'comprador1@openroof.com', '+595981123456',
     'Interesado en la casa para mi familia',
     (NOW() - INTERVAL '1 day')::timestamp),

    ('Terreno amplio en Luque', 'comprador1@openroof.com', 'PY-AG-2021-042',
     (NOW() - INTERVAL '12 days')::timestamp, 'REJECTED',
     'Luis Ramírez', 'comprador1@openroof.com', '+595981123456',
     'Quería visitar pero cambié de opinión',
     (NOW() - INTERVAL '14 days')::timestamp)

) AS t(prop_title, buyer_email, license_number, proposed_at, status,
       buyer_name, b_email, buyer_phone, message, created)
JOIN properties p ON p.title = t.prop_title
JOIN users u ON u.email = t.buyer_email
JOIN agent_profiles ap ON ap.license_number = t.license_number
WHERE NOT EXISTS (
    SELECT 1 FROM visit_requests vr
    WHERE vr.property_id = p.id AND vr.buyer_id = u.id AND vr.agent_id = ap.id
);

-- ============================================================
-- 3. CONTRACTS (ventas y alquileres)
-- ============================================================
INSERT INTO contracts (property_id, buyer_id, seller_id, contract_type, status,
                       amount, start_date, end_date, terms, created_at, updated_at, version)
SELECT p.id, buyer.id, seller.id, t.contract_type, t.status,
       t.amount, t.start_date, t.end_date, t.terms, t.created, t.created, 0
FROM (VALUES
    -- Venta cerrada: Casa Villa Morra
    ('Casa moderna en Villa Morra', 'comprador1@openroof.com', 'propietario1@openroof.com',
     'SALE', 'SIGNED', 285000.00::numeric,
     (CURRENT_DATE - INTERVAL '45 days')::date, NULL::date,
     'Venta de inmueble al contado. Escritura transferida.',
     (NOW() - INTERVAL '45 days')::timestamp),

    -- Venta cerrada: Penthouse Manorá
    ('Penthouse en Manorá', 'comprador2@openroof.com', 'propietario1@openroof.com',
     'SALE', 'SIGNED', 350000.00::numeric,
     (CURRENT_DATE - INTERVAL '20 days')::date, NULL::date,
     'Venta financiada 70/30. Primera cuota depositada.',
     (NOW() - INTERVAL '20 days')::timestamp),

    -- Alquiler activo: Depto Carmelitas
    ('Departamento amoblado en Carmelitas', 'inquilino1@openroof.com', 'propietario1@openroof.com',
     'RENT', 'SIGNED', 1200.00::numeric,
     (CURRENT_DATE - INTERVAL '90 days')::date,
     (CURRENT_DATE + INTERVAL '275 days')::date,
     'Contrato de alquiler por 12 meses. Depósito: 2 meses.',
     (NOW() - INTERVAL '90 days')::timestamp),

    -- Alquiler activo: Oficina Centro
    ('Oficina corporativa en el Centro', 'inquilino1@openroof.com', 'propietario1@openroof.com',
     'RENT', 'SIGNED', 2500.00::numeric,
     (CURRENT_DATE - INTERVAL '60 days')::date,
     (CURRENT_DATE + INTERVAL '305 days')::date,
     'Contrato de alquiler comercial por 12 meses.',
     (NOW() - INTERVAL '60 days')::timestamp),

    -- Contrato en borrador: Terreno Luque
    ('Terreno amplio en Luque', 'comprador2@openroof.com', 'propietario2@openroof.com',
     'SALE', 'DRAFT', 60000.00::numeric,
     NULL::date, NULL::date,
     'Oferta de compra en evaluación. Financiamiento bancario.',
     (NOW() - INTERVAL '3 days')::timestamp),

    -- Contrato enviado: Casa Lambaré
    ('Casa familiar en Lambaré', 'comprador1@openroof.com', 'propietario2@openroof.com',
     'SALE', 'SENT', 92000.00::numeric,
     NULL::date, NULL::date,
     'Propuesta de compra enviada al propietario.',
     (NOW() - INTERVAL '2 days')::timestamp)

) AS t(prop_title, buyer_email, seller_email, contract_type, status,
       amount, start_date, end_date, terms, created)
JOIN properties p ON p.title = t.prop_title
JOIN users buyer ON buyer.email = t.buyer_email
JOIN users seller ON seller.email = t.seller_email
WHERE NOT EXISTS (
    SELECT 1 FROM contracts c
    WHERE c.property_id = p.id AND c.buyer_id = buyer.id AND c.contract_type = t.contract_type
);

-- ============================================================
-- 4. PROPERTY VIEWS (vistas de propiedades)
-- ============================================================
INSERT INTO property_views (property_id, user_id, ip_address, user_agent, created_at, updated_at, version)
SELECT p.id, u.id, t.ip, t.ua, t.viewed_at, t.viewed_at, 0
FROM (VALUES
    -- Varias vistas para cada propiedad
    ('Casa moderna en Villa Morra', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '30 days')::timestamp),
    ('Casa moderna en Villa Morra', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '25 days')::timestamp),
    ('Casa moderna en Villa Morra', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '28 days')::timestamp),
    ('Casa moderna en Villa Morra', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '20 days')::timestamp),
    ('Casa moderna en Villa Morra', 'inquilino1@openroof.com', '190.128.30.3', 'Safari/17',   (NOW() - INTERVAL '22 days')::timestamp),

    ('Departamento amoblado en Carmelitas', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '15 days')::timestamp),
    ('Departamento amoblado en Carmelitas', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '14 days')::timestamp),
    ('Departamento amoblado en Carmelitas', 'inquilino1@openroof.com', '190.128.30.3', 'Safari/17',   (NOW() - INTERVAL '12 days')::timestamp),

    ('Terreno amplio en Luque', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '10 days')::timestamp),
    ('Terreno amplio en Luque', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '8 days')::timestamp),
    ('Terreno amplio en Luque', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '9 days')::timestamp),
    ('Terreno amplio en Luque', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '5 days')::timestamp),

    ('Oficina corporativa en el Centro', 'inquilino1@openroof.com', '190.128.30.3', 'Safari/17',  (NOW() - INTERVAL '18 days')::timestamp),
    ('Oficina corporativa en el Centro', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '16 days')::timestamp),

    ('Penthouse en Manorá', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '35 days')::timestamp),
    ('Penthouse en Manorá', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '30 days')::timestamp),
    ('Penthouse en Manorá', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '32 days')::timestamp),
    ('Penthouse en Manorá', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '28 days')::timestamp),
    ('Penthouse en Manorá', 'inquilino1@openroof.com', '190.128.30.3', 'Safari/17',   (NOW() - INTERVAL '27 days')::timestamp),

    ('Casa familiar en Lambaré', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '7 days')::timestamp),
    ('Casa familiar en Lambaré', 'comprador2@openroof.com', '190.128.20.2', 'Chrome/120',  (NOW() - INTERVAL '6 days')::timestamp),
    ('Casa familiar en Lambaré', 'comprador1@openroof.com', '190.128.10.1', 'Mozilla/5.0', (NOW() - INTERVAL '3 days')::timestamp)

) AS t(prop_title, user_email, ip, ua, viewed_at)
JOIN properties p ON p.title = t.prop_title
JOIN users u ON u.email = t.user_email
WHERE NOT EXISTS (
    SELECT 1 FROM property_views pv
    WHERE pv.property_id = p.id AND pv.user_id = u.id AND pv.created_at = t.viewed_at
);

-- ============================================================
-- 5. AGENT AGENDA (eventos de agenda para agentes)
-- ============================================================
INSERT INTO agent_agenda (user_id, agent_id, visit_id, event_type, title, description,
                          starts_at, ends_at, location, notes,
                          created_at, updated_at, version)
SELECT ap.user_id, ap.id, v.id, t.event_type, t.title, t.description,
       t.starts_at, t.ends_at, t.location, t.notes,
       NOW(), NOW(), 0
FROM (VALUES
    -- Eventos para agente1 (Carlos Mendoza)
    ('PY-AG-2018-001', NULL, 'VISIT',
     'Visita Casa Villa Morra',
     'Mostrar casa a Luis Ramírez - segunda visita con esposa',
     (NOW() + INTERVAL '2 days')::timestamp,
     (NOW() + INTERVAL '2 days' + INTERVAL '1 hour')::timestamp,
     'Av. Mariscal López 1234, Villa Morra',
     'Llevar planos y documentación de la propiedad'),

    ('PY-AG-2018-001', NULL, 'VISIT',
     'Visita Depto Carmelitas',
     'Primera visita de Sofía Villalba al departamento',
     (NOW() + INTERVAL '4 days')::timestamp,
     (NOW() + INTERVAL '4 days' + INTERVAL '45 minutes')::timestamp,
     'Calle Senador Long 456, Carmelitas',
     'Cliente interesada en inversión'),

    ('PY-AG-2018-001', NULL, 'MEETING',
     'Reunión con propietario Fernández',
     'Revisión de contrato de la oficina en el Centro',
     (NOW() + INTERVAL '1 day')::timestamp,
     (NOW() + INTERVAL '1 day' + INTERVAL '1 hour 30 minutes')::timestamp,
     'Oficina OpenRoof - Sala de reuniones',
     'Traer contrato actualizado'),

    ('PY-AG-2018-001', NULL, 'MEETING',
     'Cierre venta Penthouse Manorá',
     'Firma de documentos finales con comprador',
     (NOW() + INTERVAL '3 days')::timestamp,
     (NOW() + INTERVAL '3 days' + INTERVAL '2 hours')::timestamp,
     'Notaría González & Asociados',
     'Verificar que todos los documentos estén listos'),

    ('PY-AG-2018-001', NULL, 'BLOCKED',
     'Capacitación inmobiliaria',
     'Curso de actualización en normativas de compraventa',
     (NOW() + INTERVAL '5 days')::timestamp,
     (NOW() + INTERVAL '5 days' + INTERVAL '4 hours')::timestamp,
     'Centro de convenciones Mariscal',
     NULL),

    ('PY-AG-2018-001', NULL, 'OTHER',
     'Sesión de fotos propiedades',
     'Sesión fotográfica profesional para 3 propiedades',
     (NOW() + INTERVAL '6 days')::timestamp,
     (NOW() + INTERVAL '6 days' + INTERVAL '3 hours')::timestamp,
     'Varias ubicaciones',
     'Coordinar con fotógrafo Juan'),

    -- Eventos para agente2 (María González)
    ('PY-AG-2021-042', NULL, 'VISIT',
     'Visita Terreno Luque',
     'Mostrar terreno a Sofía Villalba - medición del lote',
     (NOW() + INTERVAL '3 days')::timestamp,
     (NOW() + INTERVAL '3 days' + INTERVAL '1 hour 30 minutes')::timestamp,
     'Ruta Luque - San Bernardino Km 5',
     'Llevar cinta métrica y documentación catastral'),

    ('PY-AG-2021-042', NULL, 'VISIT',
     'Visita Casa Lambaré',
     'Mostrar casa a Luis Ramírez y familia',
     (NOW() + INTERVAL '5 days')::timestamp,
     (NOW() + INTERVAL '5 days' + INTERVAL '1 hour')::timestamp,
     'Barrio San Isidro, Lambaré',
     'Primera visita - cliente interesado para familia'),

    ('PY-AG-2021-042', NULL, 'MEETING',
     'Reunión propietaria Ana Benítez',
     'Discutir estrategia de precios para sus propiedades',
     (NOW() + INTERVAL '1 day')::timestamp,
     (NOW() + INTERVAL '1 day' + INTERVAL '1 hour')::timestamp,
     'Café de la Estación, Luque',
     'Preparar análisis comparativo de mercado'),

    ('PY-AG-2021-042', NULL, 'OTHER',
     'Publicación nuevas propiedades',
     'Subir fotos y datos de 2 propiedades nuevas al sistema',
     (NOW() + INTERVAL '2 days')::timestamp,
     (NOW() + INTERVAL '2 days' + INTERVAL '2 hours')::timestamp,
     'Oficina OpenRoof',
     NULL)

) AS t(license_number, visit_placeholder, event_type, title, description,
       starts_at, ends_at, location, notes)
JOIN agent_profiles ap ON ap.license_number = t.license_number
LEFT JOIN visits v ON FALSE -- visit_id = NULL for all (visits table uses different flow)
WHERE NOT EXISTS (
    SELECT 1 FROM agent_agenda aa
    WHERE aa.user_id = ap.user_id AND aa.agent_id = ap.id AND aa.title = t.title AND aa.starts_at = t.starts_at
);

-- ============================================================
-- 6. NOTIFICATIONS
-- ============================================================
INSERT INTO notifications (user_id, type, title, message, action_url, data,
                           created_at, updated_at, version)
SELECT u.id, t.ntype, t.title, t.message, t.action_url, t.data::jsonb,
       t.created, t.created, 0
FROM (VALUES
    -- Notificaciones para agente1
    ('agente1@openroof.com', 'VISIT',
     'Nueva solicitud de visita',
     'Luis Ramírez quiere visitar Casa moderna en Villa Morra',
     '/dashboard/appointments',
     '{"propertyTitle":"Casa moderna en Villa Morra"}',
     (NOW() - INTERVAL '5 days')::timestamp),

    ('agente1@openroof.com', 'OFFER',
     'Nueva oferta recibida',
     'Oferta de USD 270.000 por Casa moderna en Villa Morra',
     '/dashboard/sales',
     '{"amount":270000,"currency":"USD"}',
     (NOW() - INTERVAL '4 days')::timestamp),

    ('agente1@openroof.com', 'MESSAGE',
     'Nuevo mensaje',
     'Sofía Villalba te envió un mensaje sobre Depto Carmelitas',
     '/dashboard/messages',
     '{"senderId":6}',
     (NOW() - INTERVAL '3 hours')::timestamp),

    ('agente1@openroof.com', 'CONTRACT',
     'Contrato firmado',
     'El contrato de venta del Penthouse en Manorá fue firmado',
     '/dashboard/sales',
     '{"contractType":"SALE"}',
     (NOW() - INTERVAL '20 days')::timestamp),

    ('agente1@openroof.com', 'SYSTEM',
     'Bienvenido a OpenRoof',
     'Tu perfil de agente ha sido verificado exitosamente',
     '/dashboard',
     '{}',
     (NOW() - INTERVAL '60 days')::timestamp),

    -- Notificaciones para agente2
    ('agente2@openroof.com', 'VISIT',
     'Nueva solicitud de visita',
     'Sofía Villalba quiere visitar Terreno amplio en Luque',
     '/dashboard/appointments',
     '{"propertyTitle":"Terreno amplio en Luque"}',
     (NOW() - INTERVAL '1 day')::timestamp),

    ('agente2@openroof.com', 'OFFER',
     'Nueva oferta recibida',
     'Oferta de USD 60.000 por terreno en Luque',
     '/dashboard/sales',
     '{"amount":60000,"currency":"USD"}',
     (NOW() - INTERVAL '2 days')::timestamp),

    -- Notificaciones para propietario1
    ('propietario1@openroof.com', 'CONTRACT',
     'Venta concretada',
     'La venta de Casa moderna en Villa Morra fue completada',
     '/owner/properties',
     '{"propertyTitle":"Casa moderna en Villa Morra"}',
     (NOW() - INTERVAL '45 days')::timestamp),

    ('propietario1@openroof.com', 'VISIT',
     'Visita programada',
     'Un comprador visitará tu departamento en Carmelitas',
     '/owner/visits',
     '{}',
     (NOW() - INTERVAL '2 days')::timestamp),

    ('propietario1@openroof.com', 'REVIEW',
     'Nueva reseña de agente',
     'Tu agente Carlos Mendoza recibió una reseña de 5 estrellas',
     '/agents',
     '{"rating":5}',
     (NOW() - INTERVAL '10 days')::timestamp),

    -- Notificaciones para propietario2
    ('propietario2@openroof.com', 'OFFER',
     'Oferta por tu propiedad',
     'Recibiste una oferta de USD 60.000 por el terreno en Luque',
     '/owner/properties',
     '{"amount":60000}',
     (NOW() - INTERVAL '2 days')::timestamp),

    ('propietario2@openroof.com', 'VISIT',
     'Solicitud de visita',
     'Luis Ramírez quiere visitar Casa familiar en Lambaré',
     '/owner/visits',
     '{}',
     (NOW() - INTERVAL '1 day')::timestamp),

    -- Notificaciones para compradores
    ('comprador1@openroof.com', 'CONTRACT',
     'Compra completada',
     'Felicidades! La compra de Casa moderna en Villa Morra fue completada',
     '/properties',
     '{"propertyTitle":"Casa moderna en Villa Morra"}',
     (NOW() - INTERVAL '45 days')::timestamp),

    ('comprador2@openroof.com', 'VISIT',
     'Visita confirmada',
     'Tu visita al Terreno amplio en Luque fue agendada',
     '/properties',
     '{}',
     (NOW() - INTERVAL '1 day')::timestamp)

) AS t(user_email, ntype, title, message, action_url, data, created)
JOIN users u ON u.email = t.user_email
WHERE NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.user_id = u.id AND n.type = t.ntype AND n.title = t.title AND n.created_at = t.created
);

-- ============================================================
-- 7. MENSAJES ADICIONALES (más conversaciones para dashboard)
-- ============================================================
-- (Complementa los 4 mensajes de seed-completo.sql)
INSERT INTO messages (content, sender_id, receiver_id, property_id, read_at,
                      created_at, updated_at, version)
SELECT t.content, s.id, r.id, p.id, t.read_at, t.sent_at, t.sent_at, 0
FROM (VALUES
    -- Conversación agente1 <-> propietario1 (sobre venta Penthouse)
    ('Roberto, tenemos una oferta firme por el penthouse. USD 350.000.',
     'agente1@openroof.com', 'propietario1@openroof.com', 'Penthouse en Manorá',
     (NOW() - INTERVAL '25 days')::timestamp, (NOW() - INTERVAL '24 days')::timestamp),

    ('Excelente Carlos! Acepto la oferta. Procedamos con el contrato.',
     'propietario1@openroof.com', 'agente1@openroof.com', 'Penthouse en Manorá',
     (NOW() - INTERVAL '24 days')::timestamp, (NOW() - INTERVAL '23 days')::timestamp),

    ('Perfecto, preparo los documentos y le aviso para la firma.',
     'agente1@openroof.com', 'propietario1@openroof.com', 'Penthouse en Manorá',
     (NOW() - INTERVAL '23 days')::timestamp, (NOW() - INTERVAL '22 days')::timestamp),

    -- Conversación agente2 <-> propietario2 (sobre terreno)
    ('Ana, hay mucho interés en tu terreno de Luque. Ya tenemos 2 visitas programadas.',
     'agente2@openroof.com', 'propietario2@openroof.com', 'Terreno amplio en Luque',
     (NOW() - INTERVAL '5 days')::timestamp, (NOW() - INTERVAL '4 days')::timestamp),

    ('Qué bueno María! Manteneme al tanto de cómo van las visitas.',
     'propietario2@openroof.com', 'agente2@openroof.com', 'Terreno amplio en Luque',
     (NOW() - INTERVAL '4 days')::timestamp, (NOW() - INTERVAL '3 days')::timestamp),

    ('Una de las compradores hizo una oferta de USD 60.000. ¿Qué te parece?',
     'agente2@openroof.com', 'propietario2@openroof.com', 'Terreno amplio en Luque',
     (NOW() - INTERVAL '2 days')::timestamp, NULL),

    -- Conversación agente1 <-> comprador2 (sobre Carmelitas)
    ('Buenos días Sofía, el departamento sí acepta mascotas pequeñas.',
     'agente1@openroof.com', 'comprador2@openroof.com', 'Departamento amoblado en Carmelitas',
     (NOW() - INTERVAL '2 hours')::timestamp, NULL),

    ('Genial! Entonces agendo una visita. ¿Puede ser el martes a las 15h?',
     'comprador2@openroof.com', 'agente1@openroof.com', 'Departamento amoblado en Carmelitas',
     (NOW() - INTERVAL '1 hour')::timestamp, NULL)

) AS t(content, sender_email, receiver_email, prop_title, sent_at, read_at)
JOIN users s ON s.email = t.sender_email
JOIN users r ON r.email = t.receiver_email
LEFT JOIN properties p ON p.title = t.prop_title
WHERE NOT EXISTS (
    SELECT 1 FROM messages
    WHERE content = t.content AND sender_id = s.id AND receiver_id = r.id
);

COMMIT;

-- ============================================================
-- VERIFICACIÓN DE DATOS DEL DASHBOARD
-- ============================================================
SELECT 'agent_clients'   AS tabla, COUNT(*) AS registros FROM agent_clients
UNION ALL SELECT 'visit_requests',  COUNT(*) FROM visit_requests
UNION ALL SELECT 'contracts',       COUNT(*) FROM contracts
UNION ALL SELECT 'property_views',  COUNT(*) FROM property_views
UNION ALL SELECT 'agent_agenda',    COUNT(*) FROM agent_agenda
UNION ALL SELECT 'notifications',   COUNT(*) FROM notifications
UNION ALL SELECT 'messages',        COUNT(*) FROM messages
UNION ALL SELECT 'offers',          COUNT(*) FROM offers
ORDER BY 1;

-- ============================================================
-- RESUMEN ESPERADO POR DASHBOARD:
--
-- AGENT DASHBOARD (agente1@openroof.com / Carlos Mendoza):
--   - Active Clients: 3 (comprador1, comprador2, inquilino1)
--   - Total Sales: 2 (Villa Morra SOLD + Penthouse SOLD)
--   - Scheduled Visits: 3 (2 PENDING + 1 ACCEPTED visit requests)
--   - Commissions: ~19,050 (3% de 285k + 350k contracts SIGNED)
--   - Agenda: 6 events (2 visits, 2 meetings, 1 blocked, 1 other)
--   - Messages: conversations with comprador1, comprador2, propietario1
--
-- AGENT DASHBOARD (agente2@openroof.com / María González):
--   - Active Clients: 2 (comprador2, comprador1)
--   - Total Sales: 0 (no SOLD properties)
--   - Scheduled Visits: 2 (2 PENDING visit requests)
--   - Commissions: 0
--   - Agenda: 4 events (2 visits, 1 meeting, 1 other)
--   - Messages: conversations with propietario2
--
-- OWNER DASHBOARD (propietario1@openroof.com / Roberto):
--   - My Properties: 4 (Villa Morra, Carmelitas, Oficina, Penthouse)
--   - Total Visits: 4 visit requests on his properties
--   - Inquiries: 1 offer (Villa Morra)
--   - Views: 14 property views across his 4 properties
--
-- OWNER DASHBOARD (propietario2@openroof.com / Ana):
--   - My Properties: 2 (Terreno, Casa Lambaré)
--   - Total Visits: 3 visit requests on her properties
--   - Inquiries: 1 offer (Terreno)
--   - Views: 8 property views across her 2 properties
-- ============================================================
