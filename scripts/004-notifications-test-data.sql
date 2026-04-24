-- ============================================================
-- OpenRoof — SEED 004 NOTIFICATIONS (datos de prueba realistas)
-- Ejecutar DESPUÉS de:
--   1. scripts/seed-completo.sql
--   2. scripts/seed-agents.sql (opcional)
-- ============================================================

BEGIN;

-- ============================================================
-- 1. NOTIFICACIONES PARA ADMIN
-- ============================================================
INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'SYSTEM',
    'Panel administrativo actualizado',
    'Se registraron nuevas actividades del sistema durante las últimas 24 horas.',
    jsonb_build_object(
        'section', 'admin-dashboard',
        'priority', 'medium'
    ),
    '/admin',
    NOW() - INTERVAL '5 hours',
    NOW() - INTERVAL '6 hours',
    NOW() - INTERVAL '5 hours',
    0
FROM users u
WHERE u.email = 'admin@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Panel administrativo actualizado'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'ALERT',
    'Nueva revisión pendiente',
    'Una propiedad reportada quedó pendiente de revisión administrativa.',
    jsonb_build_object(
        'propertyTitle', 'Penthouse en Manorá',
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Penthouse en Manorá' LIMIT 1)
    ),
    '/properties/me',
    NULL,
    NOW() - INTERVAL '90 minutes',
    NOW() - INTERVAL '90 minutes',
    0
FROM users u
WHERE u.email = 'admin@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Nueva revisión pendiente'
        AND n.deleted_at IS NULL
  );

-- ============================================================
-- 2. NOTIFICACIONES PARA AGENTE 1
-- ============================================================
INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'VISIT',
    'Nueva solicitud de visita',
    'Luis Ramírez solicitó visitar la propiedad Casa moderna en Villa Morra.',
    jsonb_build_object(
        'buyerEmail', 'comprador1@openroof.com',
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Casa moderna en Villa Morra' LIMIT 1),
        'propertyTitle', 'Casa moderna en Villa Morra'
    ),
    '/visit-requests',
    NULL,
    NOW() - INTERVAL '45 minutes',
    NOW() - INTERVAL '45 minutes',
    0
FROM users u
WHERE u.email = 'agente1@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Nueva solicitud de visita'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'OFFER',
    'Oferta recibida para Penthouse en Manorá',
    'Sofía Villalba envió una oferta preliminar para el penthouse publicado.',
    jsonb_build_object(
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Penthouse en Manorá' LIMIT 1),
        'propertyTitle', 'Penthouse en Manorá',
        'buyerEmail', 'comprador2@openroof.com',
        'amount', 340000
    ),
    '/properties/me',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day 30 minutes',
    NOW() - INTERVAL '1 day',
    0
FROM users u
WHERE u.email = 'agente1@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Oferta recibida para Penthouse en Manorá'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'CONTRACT',
    'Contrato listo para revisión',
    'El contrato de alquiler de Oficina corporativa en el Centro ya está listo para revisar.',
    jsonb_build_object(
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Oficina corporativa en el Centro' LIMIT 1),
        'propertyTitle', 'Oficina corporativa en el Centro',
        'stage', 'draft_ready'
    ),
    '/contracts',
    NULL,
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '3 hours',
    0
FROM users u
WHERE u.email = 'agente1@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Contrato listo para revisión'
        AND n.deleted_at IS NULL
  );

-- ============================================================
-- 3. NOTIFICACIONES PARA AGENTE 2
-- ============================================================
INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'ALERT',
    'Propiedad destacada perdió visibilidad',
    'Terreno amplio en Luque requiere actualización de datos para mantenerse destacado.',
    jsonb_build_object(
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Terreno amplio en Luque' LIMIT 1),
        'propertyTitle', 'Terreno amplio en Luque',
        'reason', 'missing_update'
    ),
    '/properties/me',
    NULL,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '2 hours',
    0
FROM users u
WHERE u.email = 'agente2@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Propiedad destacada perdió visibilidad'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'REVIEW',
    'Nuevo comentario recibido',
    'Recibiste una nueva reseña por tu atención en una operación reciente.',
    jsonb_build_object(
        'rating', 5,
        'source', 'post-visit-review'
    ),
    '/profile',
    NOW() - INTERVAL '10 hours',
    NOW() - INTERVAL '12 hours',
    NOW() - INTERVAL '10 hours',
    0
FROM users u
WHERE u.email = 'agente2@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Nuevo comentario recibido'
        AND n.deleted_at IS NULL
  );

-- ============================================================
-- 4. NOTIFICACIONES PARA PROPIETARIOS Y COMPRADORES
-- ============================================================
INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'ALERT',
    'Tu propiedad fue publicada',
    'Casa moderna en Villa Morra ya está visible para potenciales compradores.',
    jsonb_build_object(
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Casa moderna en Villa Morra' LIMIT 1),
        'propertyTitle', 'Casa moderna en Villa Morra',
        'status', 'PUBLISHED'
    ),
    '/properties/me',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days 1 hour',
    NOW() - INTERVAL '2 days',
    0
FROM users u
WHERE u.email = 'propietario1@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Tu propiedad fue publicada'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'SYSTEM',
    'Perfil de búsqueda guardado',
    'Tu preferencia de búsqueda para terrenos en Luque quedó guardada y activa.',
    jsonb_build_object(
        'location', 'Luque',
        'category', 'SALE',
        'notificationsEnabled', true
    ),
    '/properties',
    NULL,
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '4 hours',
    0
FROM users u
WHERE u.email = 'propietario2@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Perfil de búsqueda guardado'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'VISIT',
    'Horario de visita confirmado',
    'Tu visita para Casa moderna en Villa Morra fue confirmada por el agente.',
    jsonb_build_object(
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Casa moderna en Villa Morra' LIMIT 1),
        'propertyTitle', 'Casa moderna en Villa Morra',
        'scheduledAt', to_char(NOW() + INTERVAL '1 day', 'YYYY-MM-DD"T"HH24:MI:SS')
    ),
    '/visit-requests',
    NULL,
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '30 minutes',
    0
FROM users u
WHERE u.email = 'comprador1@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Horario de visita confirmado'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'OFFER',
    'Tu oferta fue recibida',
    'El agente recibió tu propuesta por el Penthouse en Manorá y la revisará pronto.',
    jsonb_build_object(
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Penthouse en Manorá' LIMIT 1),
        'propertyTitle', 'Penthouse en Manorá',
        'status', 'RECEIVED'
    ),
    '/properties',
    NOW() - INTERVAL '8 hours',
    NOW() - INTERVAL '9 hours',
    NOW() - INTERVAL '8 hours',
    0
FROM users u
WHERE u.email = 'comprador2@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Tu oferta fue recibida'
        AND n.deleted_at IS NULL
  );

INSERT INTO notifications (
    user_id, type, title, message, data, action_url, read_at,
    created_at, updated_at, version
)
SELECT
    u.id,
    'CONTRACT',
    'Renovación de alquiler disponible',
    'Ya podés revisar la propuesta de renovación de tu contrato actual.',
    jsonb_build_object(
        'propertyId', (SELECT p.id FROM properties p WHERE p.title = 'Departamento amoblado en Carmelitas' LIMIT 1),
        'propertyTitle', 'Departamento amoblado en Carmelitas',
        'status', 'READY_FOR_REVIEW'
    ),
    '/contracts',
    NULL,
    NOW() - INTERVAL '70 minutes',
    NOW() - INTERVAL '70 minutes',
    0
FROM users u
WHERE u.email = 'inquilino1@openroof.com'
  AND NOT EXISTS (
      SELECT 1 FROM notifications n
      WHERE n.user_id = u.id
        AND n.title = 'Renovación de alquiler disponible'
        AND n.deleted_at IS NULL
  );

COMMIT;
