// Fusiona los documentos DUPLICADOS del directorio `users`: un mismo jugador (mismo friendCode) con
// varios uids, uno por cada sesión anónima de Firebase que usó antes de vincularse con Google (0.2.22+).
//
// Por cada friendCode con más de un documento se conserva el visto más recientemente (`lastSeen`,
// el que usa hoy) y, de cada documento antiguo:
//   - regalos sin reclamar      → se mueven al conservado
//   - amigos                    → se re-enlazan con el conservado (en ambos lados)
//   - solicitudes recibidas     → se mueven (y la saliente del emisor apunta al conservado)
//   - solicitudes enviadas      → se mueven (y la entrante del destino apunta al conservado)
//   - invitaciones a combate    → se descartan (son efímeras)
//   - el documento antiguo      → se borra con todas sus subcolecciones
//
// Por defecto es una SIMULACIÓN (no escribe nada). Para aplicar: --apply
//   node tools/release/dedupe-users.mjs            (simula y muestra el plan)
//   node tools/release/dedupe-users.mjs --apply    (aplica)

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const here = dirname(fileURLToPath(import.meta.url));
const apply = process.argv.includes("--apply");
// --descartar-regalo <giftId>: no mueve ese regalo (p. ej. uno reenviado que ya se reclamó en otro uid).
const dropGifts = new Set(process.argv.flatMap((a, i, all) => (a === "--descartar-regalo" ? [all[i + 1]] : [])));

initializeApp({ credential: cert(JSON.parse(readFileSync(join(here, "service-account.json"), "utf8"))) });
const db = getFirestore();
const users = db.collection("users");
const seen = (d) => d.get("lastSeen")?.toMillis?.() ?? 0;

const all = await users.get();
const groups = new Map();
for (const d of all.docs) {
  const code = d.get("friendCode");
  if (!code) continue;
  if (!groups.has(code)) groups.set(code, []);
  groups.get(code).push(d);
}

let merged = 0;
for (const [code, docs] of groups) {
  if (docs.length < 2) continue;
  docs.sort((a, b) => seen(b) - seen(a));
  const [keep, ...stale] = docs;
  const keepRef = users.doc(keep.id);
  const keepName = keep.get("username") ?? "";
  console.log(`\n${code} "${keepName}": conservo ${keep.id} (${new Date(seen(keep)).toISOString()}), fusiono ${stale.length}`);

  for (const s of stale) {
    const sRef = users.doc(s.id);
    const [gifts, friends, incoming, outgoing] = await Promise.all(
      ["gifts", "friends", "incomingRequests", "outgoingRequests"].map((c) => sRef.collection(c).get()),
    );
    console.log(`  - ${s.id}: ${gifts.size} regalos, ${friends.size} amigos, ${incoming.size} recibidas, ${outgoing.size} enviadas`);
    if (!apply) continue;

    const batch = db.batch();
    for (const g of gifts.docs) if (!dropGifts.has(g.id)) batch.set(keepRef.collection("gifts").doc(g.id), g.data());
    const isMe = (id) => id === keep.id || docs.some((d) => d.id === id);
    for (const f of friends.docs) {
      batch.delete(users.doc(f.id).collection("friends").doc(s.id));
      if (isMe(f.id)) continue;
      batch.set(keepRef.collection("friends").doc(f.id), f.data(), { merge: true });
      batch.set(
        users.doc(f.id).collection("friends").doc(keep.id),
        { username: keepName, friendCode: code, since: FieldValue.serverTimestamp() },
        { merge: true },
      );
    }
    for (const r of incoming.docs) {
      batch.delete(users.doc(r.id).collection("outgoingRequests").doc(s.id));
      if (isMe(r.id)) continue;
      batch.set(keepRef.collection("incomingRequests").doc(r.id), r.data(), { merge: true });
      batch.set(
        users.doc(r.id).collection("outgoingRequests").doc(keep.id),
        { username: keepName, friendCode: code, ts: FieldValue.serverTimestamp() },
        { merge: true },
      );
    }
    for (const r of outgoing.docs) {
      batch.delete(users.doc(r.id).collection("incomingRequests").doc(s.id));
      if (isMe(r.id)) continue;
      batch.set(keepRef.collection("outgoingRequests").doc(r.id), r.data(), { merge: true });
      batch.set(
        users.doc(r.id).collection("incomingRequests").doc(keep.id),
        { username: keepName, friendCode: code, ts: FieldValue.serverTimestamp() },
        { merge: true },
      );
    }
    await batch.commit();
    await db.recursiveDelete(sRef);
  }
  merged++;
}

console.log(`\n${merged} jugador(es) con duplicados${apply ? " fusionados" : " (SIMULACIÓN: usa --apply para aplicar)"}.`);
process.exit(0);
