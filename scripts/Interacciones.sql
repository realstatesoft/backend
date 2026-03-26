-- ============================================================
-- OpenRoof — SEED DE INTERACCIONES CRM
-- Ejecutar DESPUÉS de:
--   1) seed-completo.sql
--   2) seed-agentes.sql (opcional)
--   3) la migración de interacciones CRM ya aplicada
--
-- Carga de una sola vez:
-- - agent_clients
-- - client_interactions
-- - datos para probar timeline, filtros, outcome y source
-- - deja last_contact_at e interactions_count consistentes
-- ============================================================

BEGIN;

-- ============================================================
-- 0. PRE-CHECKS
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'client_interactions'
          AND column_name = 'outcome'
    ) THEN
        RAISE EXCEPTION 'Falta la migracion de interacciones: column client_interactions.outcome no existe';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'client_interactions'
          AND column_name = 'source'
    ) THEN
        RAISE EXCEPTION 'Falta la migracion de interacciones: column client_interactions.source no existe';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'client_interactions'
          AND column_name = 'occurred_at'
    ) THEN
        RAISE EXCEPTION 'Falta la migracion de interacciones: column client_interactions.occurred_at no existe';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'agent_clients'
          AND column_name = 'last_contact_at'
    ) THEN
        RAISE EXCEPTION 'Falta la migracion de interacciones: column agent_clients.last_contact_at no existe';
    END IF;
END $$;

-- ============================================================
-- 1. AGENT_CLIENTS PARA PROBAR INTERACCIONES
-- ============================================================
WITH data AS (
    SELECT *
    FROM (VALUES
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'ACTIVE', 'HIGH', 'BUYER',
            '["comprador","vip","seguimiento"]'::jsonb,
            'WHATSAPP',
            250000.00::numeric, 320000.00::numeric,
            3, 5,
            2, 4,
            'SINGLE',
            '1991-04-15'::date,
            'Arquitecto',
            180000000.00::numeric,
            'Av. Aviadores del Chaco 1500, Asuncion',
            'FACEBOOK',
            '["HOUSE","APARTMENT"]'::jsonb,
            '["Villa Morra","Manora","Carmelitas"]'::jsonb,
            '["Piscina","Garage","Quincho"]'::jsonb,
            'Cliente muy interesado en propiedades premium. Prefiere contacto rapido por WhatsApp.',
            true,
            1,
            0
        ),
        (
            'PY-AG-2018-001', 'comprador2@openroof.com',
            'ACTIVE', 'MEDIUM', 'BUYER',
            '["comprador","inversionista"]'::jsonb,
            'EMAIL',
            180000.00::numeric, 380000.00::numeric,
            2, 4,
            2, 3,
            'MARRIED',
            '1988-09-02'::date,
            'Contadora',
            145000000.00::numeric,
            'Barrio Herrera, Asuncion',
            'INSTAGRAM',
            '["APARTMENT","OFFICE"]'::jsonb,
            '["Centro","Villa Morra"]'::jsonb,
            '["Terraza","Seguridad","Garage"]'::jsonb,
            'Busca opciones como inversion y renta.',
            true,
            0,
            1
        ),
        (
            'PY-AG-2018-001', 'inquilino1@openroof.com',
            'ACTIVE', 'MEDIUM', 'TENANT',
            '["inquilino","alquiler"]'::jsonb,
            'CALL',
            0::numeric, 0::numeric,
            1, 3,
            1, 2,
            'SINGLE',
            '1995-01-11'::date,
            'Disenador UX',
            72000000.00::numeric,
            'Lambare, Central',
            'REFERRAL',
            '["APARTMENT"]'::jsonb,
            '["Carmelitas","Manora","Centro"]'::jsonb,
            '["Mascotas","Balcon","Aire acondicionado"]'::jsonb,
            'Ya alquila una propiedad y puede renovar o mudarse.',
            false,
            1,
            0
        ),
        (
            'PY-AG-2021-042', 'propietario2@openroof.com',
            'ACTIVE', 'HIGH', 'OWNER',
            '["propietario","venta"]'::jsonb,
            'EMAIL',
            0::numeric, 0::numeric,
            0, 0,
            0, 0,
            'MARRIED',
            '1984-07-23'::date,
            'Empresaria',
            260000000.00::numeric,
            'Luque, Central',
            'GOOGLE',
            '[]'::jsonb,
            '[]'::jsonb,
            '[]'::jsonb,
            'Quiere vender rapido el terreno de Luque.',
            false,
            0,
            0
        ),
        (
            'PY-AG-2021-042', 'comprador1@openroof.com',
            'ACTIVE', 'LOW', 'BUYER',
            '["comprador","cross-sell"]'::jsonb,
            'WHATSAPP',
            90000.00::numeric, 180000.00::numeric,
            2, 3,
            1, 2,
            'SINGLE',
            '1992-12-03'::date,
            'Ingeniero civil',
            110000000.00::numeric,
            'San Lorenzo, Central',
            'TIKTOK',
            '["LAND","HOUSE"]'::jsonb,
            '["Luque","San Lorenzo","Lambare"]'::jsonb,
            '["Patio","Garage"]'::jsonb,
            'Tambien evalua terrenos con financiamiento.',
            true,
            0,
            0
        )
    ) AS t(
        license_number, user_email,
        status, priority, client_type,
        tags, preferred_contact_method,
        min_budget, max_budget,
        min_bedrooms, max_bedrooms,
        min_bathrooms, max_bathrooms,
        marital_status, birth_date, occupation, annual_income, address, source_channel,
        preferred_property_types, preferred_areas, desired_features,
        notes, is_searching_property, visited_properties_count, offers_count
    )
)
INSERT INTO agent_clients (
    agent_id,
    user_id,
    status,
    priority,
    client_type,
    tags,
    visited_properties_count,
    offers_count,
    min_budget,
    max_budget,
    min_bedrooms,
    max_bedrooms,
    min_bathrooms,
    max_bathrooms,
    preferred_contact_method,
    marital_status,
    birth_date,
    occupation,
    annual_income,
    address,
    source_channel,
    interactions_count,
    preferred_property_types,
    preferred_areas,
    desired_features,
    notes,
    is_searching_property,
    created_at,
    updated_at,
    version
)
SELECT
    ap.id,
    u.id,
    d.status,
    d.priority,
    d.client_type,
    d.tags,
    d.visited_properties_count,
    d.offers_count,
    NULLIF(d.min_budget, 0),
    NULLIF(d.max_budget, 0),
    NULLIF(d.min_bedrooms, 0),
    NULLIF(d.max_bedrooms, 0),
    NULLIF(d.min_bathrooms, 0),
    NULLIF(d.max_bathrooms, 0),
    d.preferred_contact_method,
    d.marital_status,
    d.birth_date,
    d.occupation,
    d.annual_income,
    d.address,
    d.source_channel,
    0,
    d.preferred_property_types,
    d.preferred_areas,
    d.desired_features,
    d.notes,
    d.is_searching_property,
    NOW(),
    NOW(),
    0
FROM data d
JOIN agent_profiles ap ON ap.license_number = d.license_number
JOIN users u ON u.email = d.user_email
WHERE NOT EXISTS (
    SELECT 1
    FROM agent_clients ac
    WHERE ac.agent_id = ap.id
      AND ac.user_id = u.id
      AND ac.deleted_at IS NULL
);

-- ============================================================
-- 2. INTERACCIONES CRM
-- Carga manuales y de sistema
-- Tipos: CALL, EMAIL, WHATSAPP, VISIT, MEETING, NOTE
-- Sources: MANUAL, SYSTEM
-- ============================================================
WITH interaction_seed AS (
    SELECT *
    FROM (VALUES
        -- Carlos Mendoza + comprador1
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'CALL', 'Llamada de presentacion',
            'Se realizo llamada inicial para entender necesidad y presupuesto.',
            'CONTACTED', 'MANUAL',
            (NOW() - INTERVAL '12 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'NOTE', NULL,
            'Prefiere propiedades en Villa Morra o Manora. No quiere propiedades para refaccion.',
            'INFO_CAPTURED', 'MANUAL',
            (NOW() - INTERVAL '11 days 20 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'WHATSAPP', 'Envio de opciones premium',
            'Se compartieron 4 propiedades con piscina y quincho por WhatsApp.',
            'SENT', 'SYSTEM',
            (NOW() - INTERVAL '10 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'EMAIL', 'Brochure de propiedades',
            'Se envio brochure PDF con comparativa de precios y ubicaciones.',
            'SENT', 'MANUAL',
            (NOW() - INTERVAL '9 days 6 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'MEETING', 'Reunion en oficina',
            'Se reviso capacidad financiera y tiempos estimados de compra.',
            'QUALIFIED', 'MANUAL',
            (NOW() - INTERVAL '8 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'VISIT', 'Visita confirmada',
            'Se confirmo visita a Casa moderna en Villa Morra.',
            'CONFIRMED', 'SYSTEM',
            (NOW() - INTERVAL '7 days 2 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'VISIT', 'Visita realizada',
            'El cliente asistio puntualmente y mostro alto interes en la propiedad.',
            'COMPLETED', 'MANUAL',
            (NOW() - INTERVAL '6 days 18 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'CALL', 'Seguimiento post visita',
            'Pidio una segunda visita con su esposa antes de presentar oferta.',
            'FOLLOW_UP_SCHEDULED', 'MANUAL',
            (NOW() - INTERVAL '6 days 12 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'NOTE', NULL,
            'Cliente muestra urgencia moderada. Le preocupa el costo de mantenimiento.',
            'INFO_CAPTURED', 'MANUAL',
            (NOW() - INTERVAL '5 days 23 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador1@openroof.com',
            'WHATSAPP', 'Mensaje de recordatorio',
            'Recordatorio automatico enviado para segunda visita.',
            'SENT', 'SYSTEM',
            (NOW() - INTERVAL '5 days')::timestamp
        ),

        -- Carlos Mendoza + comprador2
        (
            'PY-AG-2018-001', 'comprador2@openroof.com',
            'EMAIL', 'Primer contacto',
            'Se recibio consulta por opciones de inversion y rentabilidad estimada.',
            'CONTACTED', 'MANUAL',
            (NOW() - INTERVAL '14 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador2@openroof.com',
            'MEETING', 'Reunion de inversion',
            'Se analizo compra de oficina y departamento para renta.',
            'QUALIFIED', 'MANUAL',
            (NOW() - INTERVAL '13 days 3 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador2@openroof.com',
            'NOTE', NULL,
            'Busca retorno anual minimo del 7%. Prefiere zonas de alta demanda.',
            'INFO_CAPTURED', 'MANUAL',
            (NOW() - INTERVAL '13 days 2 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador2@openroof.com',
            'EMAIL', 'Propuesta de inversion',
            'Se enviaron 3 escenarios de compra con renta estimada.',
            'PROPOSAL_SHARED', 'MANUAL',
            (NOW() - INTERVAL '11 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador2@openroof.com',
            'CALL', 'Llamada de seguimiento financiero',
            'Cliente quiere revisar financiacion antes de avanzar.',
            'PENDING_FINANCING', 'MANUAL',
            (NOW() - INTERVAL '9 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'comprador2@openroof.com',
            'WHATSAPP', 'Mensaje enviado',
            'Se envio resumen ejecutivo con links a propiedades comparables.',
            'SENT', 'SYSTEM',
            (NOW() - INTERVAL '8 days 12 hours')::timestamp
        ),

        -- Carlos Mendoza + inquilino1
        (
            'PY-AG-2018-001', 'inquilino1@openroof.com',
            'CALL', 'Consulta de renovacion',
            'El cliente evalua renovar alquiler o mudarse a un departamento con balcon.',
            'CONTACTED', 'MANUAL',
            (NOW() - INTERVAL '15 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'inquilino1@openroof.com',
            'NOTE', NULL,
            'Acepta mascotas pequenas y necesita cochera.',
            'INFO_CAPTURED', 'MANUAL',
            (NOW() - INTERVAL '14 days 20 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'inquilino1@openroof.com',
            'VISIT', 'Visita confirmada',
            'Se confirmo visita al Departamento amoblado en Carmelitas.',
            'CONFIRMED', 'SYSTEM',
            (NOW() - INTERVAL '7 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'inquilino1@openroof.com',
            'VISIT', 'Visita realizada',
            'Le gusto la propiedad y pidio condiciones del contrato.',
            'COMPLETED', 'MANUAL',
            (NOW() - INTERVAL '6 days 20 hours')::timestamp
        ),
        (
            'PY-AG-2018-001', 'inquilino1@openroof.com',
            'EMAIL', 'Condiciones de alquiler',
            'Se enviaron condiciones, expensas y politica de mascotas.',
            'PROPOSAL_SHARED', 'MANUAL',
            (NOW() - INTERVAL '6 days')::timestamp
        ),
        (
            'PY-AG-2018-001', 'inquilino1@openroof.com',
            'WHATSAPP', 'Mensaje enviado',
            'Se envio checklist de documentos requeridos.',
            'SENT', 'SYSTEM',
            (NOW() - INTERVAL '5 days 18 hours')::timestamp
        ),

        -- Maria Gonzalez + propietario2
        (
            'PY-AG-2021-042', 'propietario2@openroof.com',
            'MEETING', 'Reunion de captacion',
            'Se revisaron objetivos de venta del terreno y precio de salida.',
            'QUALIFIED', 'MANUAL',
            (NOW() - INTERVAL '18 days')::timestamp
        ),
        (
            'PY-AG-2021-042', 'propietario2@openroof.com',
            'NOTE', NULL,
            'Quiere vender en menos de 90 dias. Esta abierta a contraofertas razonables.',
            'INFO_CAPTURED', 'MANUAL',
            (NOW() - INTERVAL '17 days 22 hours')::timestamp
        ),
        (
            'PY-AG-2021-042', 'propietario2@openroof.com',
            'CALL', 'Validacion de precio',
            'Se recomendo precio competitivo para acelerar cierre.',
            'PRICE_ALIGNED', 'MANUAL',
            (NOW() - INTERVAL '16 days')::timestamp
        ),
        (
            'PY-AG-2021-042', 'propietario2@openroof.com',
            'EMAIL', 'Reporte de mercado',
            'Se envio comparativo de terrenos similares en Luque.',
            'SENT', 'MANUAL',
            (NOW() - INTERVAL '14 days')::timestamp
        ),

        -- Maria Gonzalez + comprador1
        (
            'PY-AG-2021-042', 'comprador1@openroof.com',
            'WHATSAPP', 'Consulta por terreno',
            'Se inicio conversacion sobre Terreno amplio en Luque.',
            'CONTACTED', 'MANUAL',
            (NOW() - INTERVAL '4 days')::timestamp
        ),
        (
            'PY-AG-2021-042', 'comprador1@openroof.com',
            'VISIT', 'Visita confirmada',
            'Se confirmo visita al Terreno amplio en Luque.',
            'CONFIRMED', 'SYSTEM',
            (NOW() - INTERVAL '3 days 6 hours')::timestamp
        ),
        (
            'PY-AG-2021-042', 'comprador1@openroof.com',
            'VISIT', 'Visita reprogramada',
            'El cliente solicito mover la visita por lluvia.',
            'RESCHEDULED', 'MANUAL',
            (NOW() - INTERVAL '2 days 18 hours')::timestamp
        ),
        (
            'PY-AG-2021-042', 'comprador1@openroof.com',
            'CALL', 'Seguimiento luego de reprogramacion',
            'Sigue interesado, pero quiere evaluar acceso y servicios de la zona.',
            'FOLLOW_UP_SCHEDULED', 'MANUAL',
            (NOW() - INTERVAL '1 day 20 hours')::timestamp
        ),
        (
            'PY-AG-2021-042', 'comprador1@openroof.com',
            'NOTE', NULL,
            'Muy sensible al precio. Posible comprador si hay margen de negociacion.',
            'INFO_CAPTURED', 'MANUAL',
            (NOW() - INTERVAL '1 day 10 hours')::timestamp
        )
    ) AS t(
        license_number, user_email,
        type, subject, note, outcome, source, occurred_at
    )
)
INSERT INTO client_interactions (
    agent_id,
    agent_client_id,
    type,
    subject,
    note,
    outcome,
    source,
    occurred_at,
    created_at,
    updated_at,
    version
)
SELECT
    ap.id,
    ac.id,
    i.type,
    i.subject,
    i.note,
    i.outcome,
    i.source,
    i.occurred_at,
    i.occurred_at,
    i.occurred_at,
    0
FROM interaction_seed i
JOIN agent_profiles ap ON ap.license_number = i.license_number
JOIN users u ON u.email = i.user_email
JOIN agent_clients ac
    ON ac.agent_id = ap.id
   AND ac.user_id = u.id
   AND ac.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM client_interactions ci
    WHERE ci.agent_client_id = ac.id
      AND ci.type = i.type
      AND COALESCE(ci.subject, '') = COALESCE(i.subject, '')
      AND COALESCE(ci.note, '') = COALESCE(i.note, '')
      AND COALESCE(ci.outcome, '') = COALESCE(i.outcome, '')
      AND ci.occurred_at = i.occurred_at
      AND ci.deleted_at IS NULL
);

-- ============================================================
-- 3. RECONCILIAR interactions_count Y last_contact_at
-- ============================================================
UPDATE agent_clients ac
SET
    interactions_count = stats.interactions_count,
    last_contact_at = stats.last_contact_at,
    updated_at = NOW()
FROM (
    SELECT
        ci.agent_client_id,
        COUNT(*)::int AS interactions_count,
        MAX(ci.occurred_at) AS last_contact_at
    FROM client_interactions ci
    WHERE ci.deleted_at IS NULL
      AND ci.agent_client_id IS NOT NULL
    GROUP BY ci.agent_client_id
) stats
WHERE ac.id = stats.agent_client_id;

-- Para agent_clients sin interacciones
UPDATE agent_clients
SET
    interactions_count = 0,
    last_contact_at = NULL,
    updated_at = NOW()
WHERE id IN (
    SELECT ac.id
    FROM agent_clients ac
    LEFT JOIN client_interactions ci
      ON ci.agent_client_id = ac.id
     AND ci.deleted_at IS NULL
    GROUP BY ac.id
    HAVING COUNT(ci.id) = 0
);

COMMIT;

-- ============================================================
-- VERIFICACION RAPIDA
-- ============================================================

-- Resumen de agent_clients CRM
SELECT
    ac.id AS agent_client_id,
    ap.license_number,
    u.email AS client_email,
    ac.client_type,
    ac.priority,
    ac.interactions_count,
    ac.last_contact_at
FROM agent_clients ac
JOIN agent_profiles ap ON ap.id = ac.agent_id
JOIN users u ON u.id = ac.user_id
WHERE ap.license_number IN ('PY-AG-2018-001', 'PY-AG-2021-042')
ORDER BY ap.license_number, u.email;

-- Resumen de interacciones por cliente CRM
SELECT
    ac.id AS agent_client_id,
    ap.license_number,
    u.email AS client_email,
    ci.type,
    ci.source,
    ci.outcome,
    ci.occurred_at
FROM client_interactions ci
JOIN agent_clients ac ON ac.id = ci.agent_client_id
JOIN agent_profiles ap ON ap.id = ci.agent_id
JOIN users u ON u.id = ac.user_id
WHERE ci.deleted_at IS NULL
ORDER BY ac.id, ci.occurred_at DESC;

-- Conteo por tipo
SELECT
    type,
    source,
    COUNT(*) AS total
FROM client_interactions
WHERE deleted_at IS NULL
GROUP BY type, source
ORDER BY type, source;



UPDATE agent_clients
SET client_type = 'INDIVIDUAL'
WHERE client_type IN ('BUYER', 'OWNER', 'TENANT');

UPDATE agent_clients
SET preferred_contact_method = 'PHONE'
WHERE preferred_contact_method = 'CALL';
