import { useState, type KeyboardEvent } from 'react';
import { GripVertical, Plus, Trash2, X, ChevronUp, ChevronDown } from 'lucide-react';
import { Tooltip } from '@/components/Tooltip';
import styles from './TocBuilder.module.css';

export interface TocSection {
  name: string;
  topics: string[];
}

interface TocBuilderProps {
  value: TocSection[];
  onChange: (sections: TocSection[]) => void;
}

/**
 * Visual editor for a book's table of contents. The backend stores this as a JSON string, but
 * an admin shouldn't have to write (or be able to break) JSON — they add sections and topics,
 * and serialization happens on save.
 */
export function TocBuilder({ value, onChange }: TocBuilderProps) {
  const [drafts, setDrafts] = useState<Record<number, string>>({});

  const updateSection = (index: number, patch: Partial<TocSection>) => {
    onChange(value.map((section, i) => (i === index ? { ...section, ...patch } : section)));
  };

  const addSection = () => onChange([...value, { name: '', topics: [] }]);

  const removeSection = (index: number) => onChange(value.filter((_, i) => i !== index));

  const moveSection = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= value.length) return;
    const next = [...value];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  };

  const addTopic = (sectionIndex: number) => {
    const draft = (drafts[sectionIndex] ?? '').trim();
    if (!draft) return;
    updateSection(sectionIndex, { topics: [...value[sectionIndex].topics, draft] });
    setDrafts({ ...drafts, [sectionIndex]: '' });
  };

  const removeTopic = (sectionIndex: number, topicIndex: number) => {
    updateSection(sectionIndex, {
      topics: value[sectionIndex].topics.filter((_, i) => i !== topicIndex),
    });
  };

  const onTopicKeyDown = (event: KeyboardEvent<HTMLInputElement>, sectionIndex: number) => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      addTopic(sectionIndex);
    } else if (event.key === 'Backspace' && !drafts[sectionIndex] && value[sectionIndex].topics.length > 0) {
      removeTopic(sectionIndex, value[sectionIndex].topics.length - 1);
    }
  };

  return (
    <div className={styles.wrapper}>
      {value.length === 0 && (
        <div className={styles.empty}>
          No contents yet. Add a section like “Getting Started”, then list the topics it covers.
          <br />
          Leave this empty for fiction.
        </div>
      )}

      {value.map((section, sectionIndex) => (
        <div className={styles.section} key={sectionIndex}>
          <div className={styles.sectionHead}>
            <span className={styles.grip}>
              <GripVertical size={14} />
            </span>
            <span className={styles.sectionIndex}>{sectionIndex + 1}</span>
            <input
              className={styles.sectionName}
              placeholder="Section name — e.g. Getting Started"
              value={section.name}
              onChange={(e) => updateSection(sectionIndex, { name: e.target.value })}
            />
            <Tooltip label="Move up">
              <button
                type="button"
                className={styles.iconButton}
                disabled={sectionIndex === 0}
                onClick={() => moveSection(sectionIndex, -1)}
                aria-label="Move section up"
              >
                <ChevronUp size={14} />
              </button>
            </Tooltip>
            <Tooltip label="Move down">
              <button
                type="button"
                className={styles.iconButton}
                disabled={sectionIndex === value.length - 1}
                onClick={() => moveSection(sectionIndex, 1)}
                aria-label="Move section down"
              >
                <ChevronDown size={14} />
              </button>
            </Tooltip>
            <Tooltip label="Delete section">
              <button
                type="button"
                className={[styles.iconButton, styles.deleteButton].join(' ')}
                onClick={() => removeSection(sectionIndex)}
                aria-label="Delete section"
              >
                <Trash2 size={14} />
              </button>
            </Tooltip>
          </div>

          <div className={styles.topics}>
            {section.topics.map((topic, topicIndex) => (
              <span className={styles.topic} key={topicIndex}>
                {topic}
                <button
                  type="button"
                  className={styles.topicRemove}
                  onClick={() => removeTopic(sectionIndex, topicIndex)}
                  aria-label={`Remove ${topic}`}
                >
                  <X size={11} />
                </button>
              </span>
            ))}
            <input
              className={styles.topicInput}
              placeholder="Add topic, press Enter"
              value={drafts[sectionIndex] ?? ''}
              onChange={(e) => setDrafts({ ...drafts, [sectionIndex]: e.target.value })}
              onKeyDown={(e) => onTopicKeyDown(e, sectionIndex)}
              onBlur={() => addTopic(sectionIndex)}
            />
          </div>
        </div>
      ))}

      <button type="button" className={styles.addSection} onClick={addSection}>
        <Plus size={15} />
        Add section
      </button>
    </div>
  );
}

/** Converts builder state to the JSON string the backend stores, or null when empty. */
export function tocSectionsToJson(sections: TocSection[]): string | null {
  const meaningful = sections.filter((s) => s.name.trim() && s.topics.length > 0);
  if (meaningful.length === 0) return null;

  const object: Record<string, string[]> = {};
  for (const section of meaningful) {
    object[section.name.trim()] = section.topics;
  }
  return JSON.stringify(object);
}

/** Parses the stored JSON string back into builder state, tolerating malformed legacy data. */
export function jsonToTocSections(json: string | null): TocSection[] {
  if (!json?.trim()) return [];

  try {
    const parsed = JSON.parse(json);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) return [];

    return Object.entries(parsed).map(([name, topics]) => ({
      name,
      topics: Array.isArray(topics) ? topics.map(String) : [],
    }));
  } catch {
    return [];
  }
}
