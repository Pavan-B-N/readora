import { useState, type KeyboardEvent } from 'react';
import { X } from 'lucide-react';
import styles from './TocBuilder.module.css';

interface TocBuilderProps {
  value: string[];
  onChange: (topics: string[]) => void;
}


/** Tag-style input for building a list of table-of-contents topics — type and press Enter or comma to add, Backspace to remove the last. */
export function TocBuilder({ value, onChange }: TocBuilderProps) {
  const [draft, setDraft] = useState('');

  const addTopic = () => {
    const next = draft.trim();
    if (!next || value.includes(next)) {
      setDraft('');
      return;
    }
    onChange([...value, next]);
    setDraft('');
  };

  const removeTopic = (index: number) => onChange(value.filter((_, i) => i !== index));

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      addTopic();
    } else if (event.key === 'Backspace' && !draft && value.length > 0) {
      removeTopic(value.length - 1);
    }
  };

  return (
    <div className={styles.topics}>
      {value.map((topic, index) => (
        <span className={styles.topic} key={topic}>
          {topic}
          <button
            type="button"
            className={styles.topicRemove}
            onClick={() => removeTopic(index)}
            aria-label={`Remove ${topic}`}
          >
            <X size={11} />
          </button>
        </span>
      ))}
      <input
        className={styles.topicInput}
        placeholder="Add topic, press Enter"
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={onKeyDown}
        onBlur={addTopic}
      />
    </div>
  );
}

/** Converts a flat topic list to the JSON string the backend stores, or null when empty. */
export function topicsToJson(topics: string[]): string | null {
  if (topics.length === 0) return null;
  return JSON.stringify({ Topics: topics });
}

/** Parses the backend's stored topics JSON (an array, or an object of arrays) back into a flat topic list. */
export function jsonToTopics(json: string | null): string[] {
  if (!json?.trim()) return [];

  try {
    const parsed = JSON.parse(json);
    if (Array.isArray(parsed)) return parsed.map(String);
    if (typeof parsed !== 'object' || parsed === null) return [];

    const all: string[] = [];
    for (const topics of Object.values(parsed)) {
      if (Array.isArray(topics)) all.push(...topics.map(String));
    }
    return all;
  } catch {
    return [];
  }
}
