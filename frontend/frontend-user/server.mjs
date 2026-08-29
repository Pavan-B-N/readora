// Production static server for the built SPA (`dist/`), with one addition: when a known
// social/chat crawler requests a book detail page, it gets a small server-rendered HTML
// document with real Open Graph tags instead of the SPA shell. Crawlers for link-preview
// features (WhatsApp, Slack, Twitter/X, Discord, iMessage, ...) never execute JavaScript, so
// they can't see the title/description the SPA sets client-side (see BookDetailPage's
// document.title effect) — they only ever read whatever HTML the server returns directly.
// Everything else (real browsers, every other route) is served the normal SPA untouched.
import 'dotenv/config';
import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const app = express();

const PORT = process.env.PORT || 4173;
const API_BASE_URL = process.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const SITE_URL = process.env.SITE_URL ?? `http://localhost:${PORT}`;

const BOT_USER_AGENT =
  /facebookexternalhit|Facebot|Twitterbot|Slackbot|LinkedInBot|WhatsApp|TelegramBot|Discordbot|Pinterest|redditbot|SkypeUriPreview|Googlebot|bingbot|Applebot|vkShare/i;

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

app.get('/books/:id', async (req, res, next) => {
  if (!BOT_USER_AGENT.test(req.get('user-agent') ?? '')) return next();

  try {
    const apiResponse = await fetch(`${API_BASE_URL}/api/v1/books/${req.params.id}`);
    if (!apiResponse.ok) return next();
    const book = await apiResponse.json();

    const title = book.title;
    const authorNames = (book.authors ?? []).map((a) => a.name).join(', ');
    const description = (book.description || `${title}${authorNames ? ` by ${authorNames}` : ''} — available now on Readora.`).slice(0, 200);
    const image = book.images?.[0] || `${SITE_URL}/pwa-512x512.png`;
    const url = `${SITE_URL}/books/${book.id}`;

    res.set('Content-Type', 'text/html').send(`<!doctype html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<title>${escapeHtml(title)} — Readora</title>
<meta name="description" content="${escapeHtml(description)}" />
<meta property="og:type" content="book" />
<meta property="og:site_name" content="Readora" />
<meta property="og:title" content="${escapeHtml(title)}" />
<meta property="og:description" content="${escapeHtml(description)}" />
<meta property="og:image" content="${escapeHtml(image)}" />
<meta property="og:url" content="${escapeHtml(url)}" />
<meta name="twitter:card" content="summary_large_image" />
<meta name="twitter:title" content="${escapeHtml(title)}" />
<meta name="twitter:description" content="${escapeHtml(description)}" />
<meta name="twitter:image" content="${escapeHtml(image)}" />
</head>
<body></body>
</html>`);
  } catch {
    next();
  }
});

app.use(express.static(path.join(__dirname, 'dist')));

// Catch-all SPA fallback — deliberately routeless middleware, not a `'*'` path pattern:
// Express 5's path-to-regexp no longer accepts a bare wildcard there.
app.use((req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`frontend-user serving http://localhost:${PORT} (OG previews enabled for /books/:id)`);
});
