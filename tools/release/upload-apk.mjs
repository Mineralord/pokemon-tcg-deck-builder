// Sube la APK release a Google Drive REEMPLAZANDO el contenido del MISMO archivo (mismo fileId →
// el enlace no cambia nunca). Lo ejecuta Claude como parte del release; requiere que la cuenta de
// servicio (service-account.json) tenga acceso de EDITOR al archivo de Drive.
//
// Requisitos UNA sola vez:
//   1) tools/release/service-account.json (misma clave que para Firestore).
//   2) En Drive, compartir el archivo APK con el email de la cuenta de servicio (campo client_email
//      del JSON) como "Editor". Y, aparte, dejarlo como "Cualquiera con el enlace" para que tus
//      amigos puedan DESCARGARLO.
//   3) API de Drive habilitada en el proyecto de Google Cloud.
//
// Uso:  node tools/release/upload-apk.mjs [--apk <ruta>]

import { readFileSync, createReadStream, existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join, resolve } from "node:path";
import { google } from "googleapis";

const here = dirname(fileURLToPath(import.meta.url));

function arg(name) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

const config = JSON.parse(readFileSync(join(here, "release.config.json"), "utf8"));
const fileId = config.driveFileId;
if (!fileId) {
  console.error("Falta driveFileId en release.config.json.");
  process.exit(1);
}
const apkPath = resolve(here, arg("apk") ?? config.apkPath);
if (!existsSync(apkPath)) {
  console.error(`No existe la APK: ${apkPath}. Genera primero el release (assembleRelease).`);
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

const auth = new google.auth.GoogleAuth({
  credentials: serviceAccount,
  scopes: ["https://www.googleapis.com/auth/drive"],
});
const drive = google.drive({ version: "v3", auth });

try {
  await drive.files.update({
    fileId,
    media: {
      mimeType: "application/vnd.android.package-archive",
      body: createReadStream(apkPath),
    },
    // No conservar la revisión nueva "para siempre" → Drive puede podarla luego; además abajo
    // borramos explícitamente las revisiones viejas para no acumular APKs antiguas.
    keepRevisionForever: false,
    supportsAllDrives: true,
  });
  console.log(`✔ APK subida a Drive (fileId ${fileId}). El enlace de descarga no cambia.`);

  // Borra las revisiones ANTIGUAS (las APKs viejas que Drive guarda como historial), dejando solo
  // la más reciente (head). Así la carpeta no acumula versiones pasadas.
  const revs = await drive.revisions.list({ fileId, fields: "revisions(id)" });
  const list = revs.data.revisions ?? [];
  const old = list.slice(0, -1); // todas menos la última
  for (const r of old) {
    await drive.revisions.delete({ fileId, revisionId: r.id }).catch(() => {});
  }
  if (old.length > 0) console.log(`✔ Borradas ${old.length} revisión(es) antigua(s) en Drive.`);
  process.exit(0);
} catch (e) {
  const msg = e?.errors?.[0]?.message ?? e?.message ?? String(e);
  console.error(`✗ No se pudo subir a Drive: ${msg}`);
  console.error(
    "Comprueba que compartiste el archivo con el email de la cuenta de servicio " +
      `(${serviceAccount.client_email}) como Editor y que la API de Drive está habilitada.`,
  );
  process.exit(1);
}
