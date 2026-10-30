// Concede un REGALO a un jugador escribiéndolo en su bandeja (users/{uid}/gifts/{autoId}) con el
// Admin SDK (ignora las reglas de seguridad, por eso puede escribir en la bandeja de otro). El
// jugador lo ve en el botón 🎁 y lo reclama desde la app. Los clientes NO pueden crear regalos.
//
// Requiere tools/release/service-account.json (el mismo de publish-version/upload-apk), del proyecto
// Firebase correcto (tcg-live-clone-net-4823).
//
// El destinatario se identifica por ID de amigo (12 dígitos) o por email de su cuenta Google. Debe
// haber abierto la app al menos una vez estando en línea para figurar en el directorio `users`
// (email: además, haber iniciado sesión con Google al menos una vez tras la v0.2.7).
//
// Uso:
//   node tools/release/grant-gift.mjs --friend 075438786726 --cristales 50000
//   node tools/release/grant-gift.mjs --email pmmt99@gmail.com --cristales 50000 --title "Regalo" --message "¡Disfruta!"
//   (monedas/fichas opcionales: --monedas 1000 --fichas 50)
//   Retirar (ajuste admin, v0.2.18+; el saldo no baja de 0): --cristales -500  ·  --cristales todo

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const here = dirname(fileURLToPath(import.meta.url));

function arg(name) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : undefined;
}
function intArg(name) {
  const v = arg(name);
  if (v === undefined) return 0;
  if (v === "todo") return -2_000_000_000; // "retirar todo": el cliente lo recorta a 0
  const n = Number(v);
  if (!Number.isInteger(n) || Math.abs(n) > 2_000_000_000) {
    console.error(`--${name} debe ser un entero (negativo = retirar) o "todo".`);
    process.exit(1);
  }
  return n;
}

const friend = arg("friend")?.replace(/\D/g, "");
const email = arg("email")?.trim().toLowerCase();
const cristales = intArg("cristales");
const monedas = intArg("monedas");
const fichas = intArg("fichas");
const title = arg("title") ?? "Regalo";
const message = arg("message") ?? "";

if (!friend && !email) {
  console.error("Falta destinatario: usa --friend <12 dígitos> o --email <correo>.");
  process.exit(1);
}
if (!cristales && !monedas && !fichas) {
  console.error("Falta recompensa: usa --cristales / --monedas / --fichas (al menos una > 0).");
  process.exit(1);
}

let serviceAccount;
try {
  serviceAccount = JSON.parse(readFileSync(join(here, "service-account.json"), "utf8"));
} catch {
  console.error(
    "Falta tools/release/service-account.json. Descárgalo de la consola de Firebase " +
      "(Configuración → Cuentas de servicio → Generar nueva clave privada) y guárdalo ahí.",
  );
  process.exit(1);
}

initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

try {
  // Resuelve el uid del destinatario en el directorio `users`.
  const field = friend ? "friendCode" : "email";
  const value = friend || email;
  const snap = await db.collection("users").where(field, "==", value).limit(1).get();
  if (snap.empty) {
    console.error(
      `No se encontró ningún jugador con ${field}="${value}". ` +
        "Debe haber abierto la app en línea al menos una vez (y, por email, haber iniciado sesión con Google).",
    );
    process.exit(1);
  }
  const uid = snap.docs[0].id;
  const uname = snap.docs[0].get("username") || "(sin nombre)";

  await db.collection("users").doc(uid).collection("gifts").add({
    cristales,
    monedas,
    fichas,
    title,
    message,
    ts: FieldValue.serverTimestamp(),
  });

  const fmt = (n, label) =>
    n <= -2_000_000_000 ? `−todos los ${label}` : n && `${n > 0 ? "+" : "−"}${Math.abs(n).toLocaleString("es")} ${label}`;
  const rewards = [fmt(cristales, "cristales"), fmt(monedas, "monedas"), fmt(fichas, "fichas")]
    .filter(Boolean).join(" · ");
  console.log(`✔ Regalo enviado a ${uname} (${field}=${value}, uid=${uid}): ${rewards}.`);
  console.log("  Lo verá en el botón 🎁 la próxima vez que abra la app en línea.");
  process.exit(0);
} catch (e) {
  const msg = e?.errors?.[0]?.message ?? e?.message ?? String(e);
  console.error(`✗ No se pudo conceder el regalo: ${msg}`);
  process.exit(1);
}
