import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";
admin.initializeApp();
const deepseekKey = defineSecret("DEEPSEEK_API_KEY");
const owners = () => new Set((process.env.OWNER_EMAILS || "owner@example.com").split(",").map(x => x.trim()).filter(Boolean));
export const agent = onRequest({ secrets: [deepseekKey] }, async (req, res) => {
  if (req.method !== "POST") return res.status(405).json({ reply: "Method not allowed" });
  const { message, idToken, model = "flash", approved = false, action } = req.body || {};
  if (typeof message !== "string" || !message.trim()) return res.status(400).json({ reply: "Message is required" });
  if (action && !approved) return res.status(409).json({ reply: "Explicit approval is required for this action" });
  try {
    const decoded = await admin.auth().verifyIdToken(String(idToken || ""));
    if (!decoded.email || !owners().has(decoded.email)) return res.status(403).json({ reply: "Not authorized" });
    const modelName = model === "pro" ? "deepseek-reasoner" : "deepseek-chat";
    const response = await fetch("https://api.deepseek.com/chat/completions", { method: "POST", headers: { "Authorization": `Bearer ${deepseekKey.value()}`, "Content-Type": "application/json" }, body: JSON.stringify({ model: modelName, messages: [{ role: "user", content: message }], max_tokens: 512 }) });
    if (!response.ok) throw new Error(`DeepSeek ${response.status}`);
    const json = await response.json() as { choices?: Array<{ message?: { content?: string } }> };
    const reply = json.choices?.[0]?.message?.content?.trim();
    if (!reply) throw new Error("DeepSeek returned no reply");
    return res.json({ reply });
  } catch (error) { console.error("agent request failed", error); return res.status(401).json({ reply: "Agent request could not be completed" }); }
});
