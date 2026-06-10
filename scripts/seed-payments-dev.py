#!/usr/bin/env python3
"""
seed-payments-dev.py — Genera datos de prueba de PAGOS contra la API dev de OpenRoof.

A diferencia de los seeds SQL, este script pasa por la API real: ejercita
validaciones, transiciones de estado, idempotencia y deja rastro de auditoría,
exactamente como lo hará la app móvil.

Modos:
  seed   - crea pagos de plataforma (PENDING/APPROVED/REJECTED) y pagos de cuotas
  edge   - casos límite: validaciones 400, idempotencia, doble pago, montos inválidos
  load   - ráfaga concurrente de lecturas+escrituras para prueba de demanda
  audit  - verifica que las acciones queden en /admin/audit-logs
  all    - seed + edge + audit

Uso:
  export OPENROOF_BASE_URL="https://<tu-dev>.com/api"   # default http://localhost:8080/api
  export OPENROOF_ADMIN_EMAIL="admin@openroof.com"
  export OPENROOF_ADMIN_PASSWORD="Test1234!"
  python3 seed-payments-dev.py seed --count 200
  python3 seed-payments-dev.py edge
  python3 seed-payments-dev.py load --count 300 --workers 20
  python3 seed-payments-dev.py all

Requisitos: python3 + requests  (pip install requests)
"""

import argparse
import concurrent.futures
import os
import random
import sys
import time
import uuid

import requests

BASE_URL = os.environ.get("OPENROOF_BASE_URL", "http://localhost:8080/api").rstrip("/")
ADMIN_EMAIL = os.environ.get("OPENROOF_ADMIN_EMAIL", "admin@openroof.com")
ADMIN_PASSWORD = os.environ.get("OPENROOF_ADMIN_PASSWORD", "Test1234!")
DEFAULT_PASSWORD = os.environ.get("OPENROOF_USER_PASSWORD", "Test1234!")

# Usuarios sembrados en dev (ver frontend/CUENTAS_DE_PRUEBA.md)
USER_POOL = [
    "inquilino1@openroof.com",
    "comprador1@openroof.com",
    "comprador2@openroof.com",
    "propietario1@openroof.com",
    "agente1@openroof.com",
    "agente2@openroof.com",
]

# Datos realistas (Paraguay)
CONCEPTS_RESERVATION = [
    "Reserva depto. 2 dorm. Barrio Jara - Asunción",
    "Reserva casa Lambaré, cuota inicial",
    "Reserva monoambiente Villa Morra",
    "Reserva dúplex San Lorenzo",
    "Reserva depto. amoblado Recoleta",
]
CONCEPTS_HIGHLIGHT = [
    "Destaque propiedad {pid} por {dias} días",
    "Promoción destacada prop. {pid} ({dias} días)",
]
AMOUNTS_RESERVATION = [500_000, 800_000, 1_000_000, 1_500_000, 2_000_000]
AMOUNTS_HIGHLIGHT = [100_000, 150_000, 250_000]
HIGHLIGHT_DAYS = [7, 15, 30]
PAYMENT_NOTES = [
    "Transferencia Ueno Bank, comprobante #{n}",
    "Transferencia Itaú, op. #{n}",
    "Pago en efectivo, recibo manual #{n}",
    "Giros Tigo Money ref. #{n}",
]

S = requests.Session()
S.headers.update({"Content-Type": "application/json"})

stats = {"ok": 0, "fail": 0, "expected_4xx": 0}


def log(msg, ok=True):
    print(("  ✔ " if ok else "  ✘ ") + msg)


def login(email, password):
    r = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": password}, timeout=30)
    if r.status_code != 200:
        raise RuntimeError(f"Login falló para {email}: {r.status_code} {r.text[:200]}")
    return r.json()["data"]["accessToken"]


def auth_headers(token, idem=None):
    h = {"Authorization": f"Bearer {token}"}
    if idem:
        h["Idempotency-Key"] = idem
    return h


def req(method, path, token=None, expect=None, idem=None, **kwargs):
    """Petición con contabilidad de resultados. expect = status esperado (o lista)."""
    url = f"{BASE_URL}{path}"
    r = requests.request(method, url, headers=auth_headers(token, idem) if token else {}, timeout=60, **kwargs)
    expected = expect if isinstance(expect, (list, tuple)) else [expect or 200]
    if r.status_code in expected:
        stats["ok" if r.status_code < 400 else "expected_4xx"] += 1
    else:
        stats["fail"] += 1
        log(f"{method} {path} → {r.status_code} (esperaba {expected}) {r.text[:160]}", ok=False)
    return r


# ---------------------------------------------------------------- seed

def seed_platform_payments(count, admin_token, tokens, plan_ids, property_ids):
    """Pagos de plataforma en mezcla de estados: ~50% APPROVED, 20% REJECTED, 30% PENDING."""
    print(f"\n== Seed: {count} pagos de plataforma ==")
    created = []
    for i in range(count):
        email, token = random.choice(list(tokens.items()))
        kind = random.choices(["RESERVATION", "PROPERTY_HIGHLIGHT", "SUBSCRIPTION"], weights=[5, 3, 2])[0]
        if kind == "RESERVATION":
            body = {
                "type": "RESERVATION",
                "amount": random.choice(AMOUNTS_RESERVATION),
                "concept": random.choice(CONCEPTS_RESERVATION),
            }
        elif kind == "PROPERTY_HIGHLIGHT" and property_ids:
            pid, dias = random.choice(property_ids), random.choice(HIGHLIGHT_DAYS)
            body = {
                "type": "PROPERTY_HIGHLIGHT",
                "amount": random.choice(AMOUNTS_HIGHLIGHT),
                "concept": random.choice(CONCEPTS_HIGHLIGHT).format(pid=pid, dias=dias),
                "metadata": {"propertyId": pid, "highlightDays": dias},
            }
        elif kind == "SUBSCRIPTION" and plan_ids:
            plan = random.choice(plan_ids)
            body = {
                "type": "SUBSCRIPTION",
                "amount": plan["price"],
                "concept": f"Suscripción {plan['name']}",
                "metadata": {"subscriptionPlanId": plan["id"]},
            }
        else:
            body = {"type": "OTHER", "amount": 50_000, "concept": "Pago varios QA"}
        r = req("POST", "/payments", token, expect=201, json=body)
        if r.status_code == 201:
            created.append(r.json()["data"]["id"])

    random.shuffle(created)
    n_appr, n_rej = int(len(created) * 0.5), int(len(created) * 0.2)
    for pid in created[:n_appr]:
        # approve puede dar 400 legítimo (p.ej. plan desactivado): lo aceptamos
        req("POST", f"/payments/{pid}/approve", admin_token, expect=[200, 400])
    for pid in created[n_appr:n_appr + n_rej]:
        req("POST", f"/payments/{pid}/reject", admin_token, expect=200)
    print(f"   creados={len(created)} approved≈{n_appr} rejected≈{n_rej} pending≈{len(created)-n_appr-n_rej}")
    return created


def get_pending_installments(token):
    """Descubre leases ACTIVE del usuario (vista tenant) y devuelve sus cuotas pagables."""
    r = req("GET", "/tenant/lease", token, expect=200, params={"size": 20})
    if r.status_code != 200:
        return []
    leases = ((r.json().get("data") or {}).get("content")) or []
    out = []
    for lease in leases:
        r2 = req("GET", "/rentals/installments", token, expect=200, params={"leaseId": lease["id"]})
        if r2.status_code == 200:
            out += [i for i in (r2.json().get("data") or [])
                    if i.get("status") in ("PENDING", "PARTIAL", "OVERDUE")]
    return out


def seed_installment_payments(tokens, per_user=10):
    """Paga cuotas reales de los leases de cada usuario: completas y parciales."""
    print("\n== Seed: pagos de cuotas de alquiler ==")
    total = 0
    for email, token in tokens.items():
        installments = get_pending_installments(token)[:per_user]
        for inst in installments:
            paid = inst.get("payments") or []
            paid_amount = sum(p["amount"] for p in paid)
            balance = float(inst["totalAmount"]) - paid_amount
            if balance <= 0:
                continue
            partial = random.random() < 0.4 and balance > 100_000
            amount = round(balance * 0.5) if partial else balance
            body = {
                "amount": amount,
                "method": random.choice(["TRANSFER", "CASH", "CARD", "CHECK"]),
                "notes": random.choice(PAYMENT_NOTES).format(n=random.randint(10000, 99999)),
            }
            r2 = req("POST", f"/rentals/installments/{inst['id']}/payments",
                     token, expect=201, idem=str(uuid.uuid4()), json=body)
            if r2.status_code == 201:
                total += 1
    print(f"   pagos de cuota registrados: {total}")


# ---------------------------------------------------------------- edge

def edge_cases(admin_token, tokens):
    """Cada caso declara el status esperado: si la API responde otra cosa, cuenta como fallo."""
    print("\n== Edge cases (la app móvil debe manejar todos estos) ==")
    email, token = next(iter(tokens.items()))

    # 401 sin token
    req("POST", "/payments", None, expect=[401, 403],
        json={"type": "RESERVATION", "amount": 1000, "concept": "x"})
    log("401/403 sin token")

    # validaciones 400
    for body, desc in [
        ({"type": "RESERVATION", "amount": -5, "concept": "monto negativo"}, "amount negativo"),
        ({"type": "RESERVATION", "amount": 100.999, "concept": "3 decimales"}, "3 decimales"),
        ({"type": "RESERVATION", "amount": 1000, "concept": ""}, "concept vacío"),
        ({"amount": 1000, "concept": "sin type"}, "type faltante"),
        ({"type": "PROPERTY_HIGHLIGHT", "amount": 1000, "concept": "sin metadata"}, "highlight sin metadata"),
        ({"type": "SUBSCRIPTION", "amount": 1000, "concept": "sin plan"}, "subscription sin planId"),
    ]:
        req("POST", "/payments", token, expect=400, json=body)
        log(f"400 {desc}")

    # 404 y 403
    req("GET", "/payments/99999999", token, expect=404)
    log("404 pago inexistente")
    req("GET", "/payments", token, expect=403)
    log("403 listado admin con usuario normal")

    # transición inválida: rechazar un pago ya rechazado
    r = req("POST", "/payments", token, expect=201,
            json={"type": "RESERVATION", "amount": 750_000, "concept": "Reserva edge transición"})
    if r.status_code == 201:
        pid = r.json()["data"]["id"]
        req("POST", f"/payments/{pid}/reject", admin_token, expect=200)
        req("POST", f"/payments/{pid}/reject", admin_token, expect=400)
        log("400 doble reject (transición inválida)")
        req("POST", f"/payments/{pid}/approve", admin_token, expect=400)
        log("400 approve sobre REJECTED")

    # idempotencia en cuotas: mismo Idempotency-Key dos veces ⇒ mismo pago, sin duplicar
    pendientes = []
    for em, tk in tokens.items():
        pendientes = get_pending_installments(tk)
        if pendientes:
            token = tk
            break
    if pendientes:
        inst = pendientes[0]
        idem = str(uuid.uuid4())
        body = {"amount": 1000, "method": "CASH", "notes": "test idempotencia"}
        r1 = req("POST", f"/rentals/installments/{inst['id']}/payments", token, expect=201, idem=idem, json=body)
        r2 = req("POST", f"/rentals/installments/{inst['id']}/payments", token, expect=201, idem=idem, json=body)
        if r1.status_code == r2.status_code == 201:
            same = r1.json()["data"]["id"] == r2.json()["data"]["id"]
            log(f"idempotencia: mismo payment id en retry → {same}", ok=same)
            if not same:
                stats["fail"] += 1
        # sin Idempotency-Key ⇒ 400
        req("POST", f"/rentals/installments/{inst['id']}/payments", token, expect=400, json=body)
        log("400 sin Idempotency-Key")
        # monto mayor al saldo ⇒ 400
        req("POST", f"/rentals/installments/{inst['id']}/payments", token, expect=400,
            idem=str(uuid.uuid4()), json={"amount": 999_999_999, "method": "CASH"})
        log("400 monto excede saldo")
    else:
        print("   (sin cuotas pendientes para este usuario; correr seed SQL de leases primero)")


# ---------------------------------------------------------------- load

def load_test(tokens, count, workers):
    """Ráfaga mixta 80% lecturas / 20% escrituras. Mide latencias y errores."""
    print(f"\n== Load: {count} requests, {workers} workers ==")
    token_list = list(tokens.values())
    latencies, errors = [], []

    def one(i):
        token = random.choice(token_list)
        t0 = time.time()
        try:
            if i % 5 == 0:
                r = requests.post(f"{BASE_URL}/payments",
                                  headers=auth_headers(token),
                                  json={"type": "RESERVATION",
                                        "amount": random.choice(AMOUNTS_RESERVATION),
                                        "concept": f"Reserva load-test #{i}"},
                                  timeout=60)
                ok = r.status_code == 201
            else:
                path = random.choice(["/payments/my", "/tenant/lease", "/tenant/payments"])
                r = requests.get(f"{BASE_URL}{path}", headers=auth_headers(token), timeout=60)
                ok = r.status_code == 200
            if not ok:
                errors.append(r.status_code)
        except Exception as e:
            errors.append(str(e)[:60])
        latencies.append(time.time() - t0)

    t0 = time.time()
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as ex:
        list(ex.map(one, range(count)))
    dur = time.time() - t0
    latencies.sort()
    p = lambda q: latencies[int(len(latencies) * q)] * 1000 if latencies else 0
    print(f"   {count} reqs en {dur:.1f}s → {count/dur:.1f} req/s")
    print(f"   latencia ms: p50={p(0.5):.0f} p95={p(0.95):.0f} p99={p(0.99):.0f}")
    print(f"   errores: {len(errors)}" + (f" → muestra: {errors[:5]}" if errors else ""))


# ---------------------------------------------------------------- audit

def audit_check(admin_token):
    print("\n== Auditoría ==")
    r = req("GET", "/admin/audit-logs/entity-options", admin_token, expect=200)
    if r.status_code == 200:
        print(f"   entidades auditables: {r.json().get('data')}")
    r = req("GET", "/admin/audit-logs", admin_token, expect=200, params={"page": 0, "size": 10})
    if r.status_code == 200:
        data = r.json().get("data") or {}
        total = (data.get("page") or {}).get("totalElements", data.get("totalElements", "?"))
        print(f"   audit-logs accesible, totalElements={total}")
        print("   ⇒ revisar manualmente que los pagos recién creados figuren con su usuario y acción.")


# ---------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("mode", choices=["seed", "edge", "load", "audit", "all"])
    ap.add_argument("--count", type=int, default=100, help="pagos a crear (seed) o requests (load)")
    ap.add_argument("--workers", type=int, default=10, help="concurrencia en modo load")
    ap.add_argument("--property-ids", default="404,402,401,33",
                    help="IDs de propiedades existentes para pagos PROPERTY_HIGHLIGHT")
    args = ap.parse_args()

    print(f"Base URL: {BASE_URL}")
    admin_token = login(ADMIN_EMAIL, ADMIN_PASSWORD)
    print(f"Login ADMIN ok ({ADMIN_EMAIL})")

    tokens = {}
    for email in USER_POOL:
        try:
            tokens[email] = login(email, DEFAULT_PASSWORD)
        except RuntimeError as e:
            print(f"  (omitido {email}: {e})")
        time.sleep(0.4)  # rate limit en /auth
    if not tokens:
        sys.exit("Ningún usuario de prueba pudo loguearse; revisar seeds de usuarios.")
    print(f"Usuarios logueados: {len(tokens)}")

    plan_ids = []
    r = requests.get(f"{BASE_URL}/subscription-plans", timeout=30)
    if r.status_code == 200:
        content = (r.json().get("data") or {}).get("content") or []
        plan_ids = [{"id": p["id"], "name": p["name"], "price": p["price"]} for p in content]
    property_ids = [int(x) for x in args.property_ids.split(",") if x.strip()]

    if args.mode in ("seed", "all"):
        seed_platform_payments(args.count, admin_token, tokens, plan_ids, property_ids)
        seed_installment_payments(tokens)
    if args.mode in ("edge", "all"):
        edge_cases(admin_token, tokens)
    if args.mode == "load":
        load_test(tokens, args.count, args.workers)
    if args.mode in ("audit", "all"):
        audit_check(admin_token)

    print(f"\nResumen: ok={stats['ok']} esperados-4xx={stats['expected_4xx']} FALLOS={stats['fail']}")
    sys.exit(1 if stats["fail"] else 0)


if __name__ == "__main__":
    main()
