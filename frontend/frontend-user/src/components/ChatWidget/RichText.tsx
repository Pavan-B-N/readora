import { Fragment, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import styles from './RichText.module.css';

type Block = { type: 'list'; items: string[] } | { type: 'text'; lines: string[] };

/**
 * Lightweight formatter for assistant replies — handles the markdown subset the model is
 * instructed to use (see ChatClientConfig's system prompt): paragraphs, "- " bullet lists,
 * **bold**, and [text](url) links. Deliberately not a full markdown parser (no tables, headers
 * become plain text, etc.) — pulling in a markdown library for this narrow a subset isn't worth
 * the bundle cost.
 */
export function RichText({ text }: { text: string }) {
  const blocks: Block[] = [];

  for (const rawLine of text.split('\n')) {
    const line = rawLine.replace(/^#{1,6}\s+/, '');
    const bulletMatch = line.match(/^[-*]\s+(.*)$/);
    const last = blocks[blocks.length - 1];

    if (bulletMatch) {
      if (last?.type === 'list') {
        last.items.push(bulletMatch[1]);
      } else {
        blocks.push({ type: 'list', items: [bulletMatch[1]] });
      }
    } else if (last?.type === 'text') {
      last.lines.push(line);
    } else {
      blocks.push({ type: 'text', lines: [line] });
    }
  }

  return (
    <>
      {blocks.map((block, blockIndex) =>
        block.type === 'list' ? (
          <ul className={styles.list} key={blockIndex}>
            {block.items.map((item, i) => (
              <li key={i}>{renderInline(item)}</li>
            ))}
          </ul>
        ) : (
          <Fragment key={blockIndex}>
            {block.lines.map((line, i) => (
              <Fragment key={i}>
                {i > 0 && <br />}
                {renderInline(line)}
              </Fragment>
            ))}
          </Fragment>
        ),
      )}
    </>
  );
}

function renderInline(line: string): ReactNode[] {
  // Split on **bold** and [text](url), keeping the delimited matches in the result.
  const parts = line.split(/(\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))/g);

  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      // The model routinely bolds its own links — **[Title](/books/id)** — so the inner content
      // needs the same treatment recursively, or the link syntax shows up as literal bracket text.
      return <strong key={index}>{renderInline(part.slice(2, -2))}</strong>;
    }

    const linkMatch = part.match(/^\[([^\]]+)\]\(([^)]+)\)$/);
    if (linkMatch) {
      const [, label, url] = linkMatch;
      // The label can itself carry markdown — the model just as often writes [**Title**](url) as
      // **[Title](url)** — so it needs the same recursive treatment as the bold branch above.
      const content = renderInline(label);
      const bookPath = toBookRoutePath(url);
      return bookPath ? (
        <Link key={index} to={bookPath} className={styles.link}>
          {content}
        </Link>
      ) : (
        <a key={index} href={url} target="_blank" rel="noreferrer" className={styles.link}>
          {content}
        </a>
      );
    }

    return <Fragment key={index}>{part}</Fragment>;
  });
}

/**
 * The system prompt asks for a relative "/books/{id}" link, but the model doesn't always comply —
 * it sometimes fully-qualifies it with this app's own origin (e.g. "http://localhost:5173/books/
 * {id}"), which would otherwise fall through to an external-style <a target="_blank"> instead of
 * an in-app navigation. Recognizes a book link regardless of whether it's relative or absolute;
 * anything else is left as a genuine external link.
 */
function toBookRoutePath(url: string): string | null {
  if (url.startsWith('/')) {
    return url.startsWith('/books/') ? url : null;
  }
  try {
    const { pathname } = new URL(url);
    return pathname.startsWith('/books/') ? pathname : null;
  } catch {
    return null;
  }
}
