import { Fragment } from 'react';

/**
 * Minimal inline formatter for assistant replies — renders **bold** and strips stray markdown
 * headers. Deliberately not a full markdown parser: the model only ever emits light inline
 * formatting here, and pulling in a markdown library for that isn't worth the bundle cost.
 */
export function RichText({ text }: { text: string }) {
  const lines = text.split('\n');

  return (
    <>
      {lines.map((line, lineIndex) => (
        <Fragment key={lineIndex}>
          {lineIndex > 0 && <br />}
          {renderInline(line.replace(/^#{1,6}\s+/, ''))}
        </Fragment>
      ))}
    </>
  );
}

function renderInline(line: string) {
  // Split on **bold** while keeping the delimiters' contents.
  const parts = line.split(/(\*\*[^*]+\*\*)/g);

  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    return <Fragment key={index}>{part}</Fragment>;
  });
}
