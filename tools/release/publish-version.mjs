// Publica la ÚLTIMA versión descargable en Firestore (config/appVersion). Lo ejecuta Claude como
// paso 3 del flujo de release; el cliente lee ese doc al arrancar y muestra el aviso obligatorio.
//
// Requiere una clave de servicio (Admin SDK) en tools/release/service-account.json (gitignored):
// Consola de Firebase → Configuración del proyecto → Cuentas de servicio → "Generar nueva clave privada".
// El Admin SDK ignora las reglas de seguridad, por eso puede escribir aunque config sea de solo lectura.
//
// Uso:
//   node tools/release/publish-version.mjs --code 3 --name 0.3.0 --message "Novedades…"
//   node tools/release/publish-version.mjs --code 3 --name 0.3.0 --url "https://…"   (sobre-escribe el enlace)

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const here = dirname(fileURLToPath(import.meta.url));

function arg(name) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

const code = Number(arg("code"));
const name = arg("name");
const message = arg("message") ?? "";
if (!Number.isInteger(code) || code <= 0 || !name) {
  console.error("Faltan argumentos. Uso: --code <N> --name <x.y.z> [--message \"…\"] [--url \"…\"]");
  process.exit(1);
}

const config = JSON.parse(readFileSync(join(here, "release.config.json"), "utf8"));
const downloadUrl = arg("url") ?? config.downloadUrl;
if (!downloadUrl) {
  console.error("No hay downloadUrl (ni en release.config.json ni en --url).");
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

await db.doc("config/appVersion").set({
  latestVersionCode: code,
  versionName: name,
  downloadUrl,
  message,
  updatedAt: new Date().toISOString(),
});

console.log(`✔ Publicado: versionCode=${code} (${name}) → ${downloadUrl}`);
process.exit(0);
