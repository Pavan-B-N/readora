import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react';
import { X, ChevronDown } from 'lucide-react';
import { FieldWrapper } from '@/components/Input';
import styles from './Combobox.module.css';

export interface ComboboxOption {
  value: string;
  label: string;
  meta?: string;
}

const MAX_SUGGESTIONS = 5;

interface BaseProps {
  options: ComboboxOption[];
  label?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  placeholder?: string;
  disabled?: boolean;
}

interface SingleProps extends BaseProps {
  multiple?: false;
  value: string | null;
  onChange: (value: string | null) => void;
}

interface MultiProps extends BaseProps {
  multiple: true;
  value: string[];
  onChange: (value: string[]) => void;
}

type ComboboxProps = SingleProps | MultiProps;

/**
 * Type-to-search select showing the top 5 matches. Replaces long native <select> lists —
 * with hundreds of authors, scrolling a dropdown isn't usable.
 */
export function Combobox(props: ComboboxProps) {
  const { options, label, hint, error, required, placeholder, disabled } = props;
  const isMulti = props.multiple === true;

  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const selectedValues = useMemo(
    () => (isMulti ? (props.value as string[]) : props.value ? [props.value as string] : []),
    [isMulti, props.value],
  );

  const suggestions = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    const pool = normalized
      ? options.filter((o) => o.label.toLowerCase().includes(normalized))
      : options;
    return pool.slice(0, MAX_SUGGESTIONS);
  }, [options, query]);

  const totalMatches = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return normalized ? options.filter((o) => o.label.toLowerCase().includes(normalized)).length : options.length;
  }, [options, query]);

  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setOpen(false);
        setQuery('');
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const commit = (option: ComboboxOption) => {
    if (isMulti) {
      const current = props.value as string[];
      const next = current.includes(option.value)
        ? current.filter((v) => v !== option.value)
        : [...current, option.value];
      (props.onChange as (v: string[]) => void)(next);
      setQuery('');
      inputRef.current?.focus();
    } else {
      (props.onChange as (v: string | null) => void)(option.value);
      setQuery('');
      setOpen(false);
    }
  };

  const removeValue = (value: string) => {
    if (isMulti) {
      (props.onChange as (v: string[]) => void)((props.value as string[]).filter((v) => v !== value));
    } else {
      (props.onChange as (v: string | null) => void)(null);
    }
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setOpen(true);
      setActiveIndex((i) => Math.min(i + 1, suggestions.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveIndex((i) => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const option = suggestions[activeIndex];
      if (option) commit(option);
    } else if (event.key === 'Escape') {
      setOpen(false);
      setQuery('');
    } else if (event.key === 'Backspace' && !query && selectedValues.length > 0) {
      removeValue(selectedValues[selectedValues.length - 1]);
    }
  };

  const labelFor = (value: string) => options.find((o) => o.value === value)?.label ?? value;
  const singleSelectedLabel = !isMulti && selectedValues[0] ? labelFor(selectedValues[0]) : '';

  return (
    <FieldWrapper label={label} hint={hint} error={error} required={required}>
      <div className={styles.wrapper} ref={wrapperRef}>
        <div
          className={[styles.control, open && styles.controlOpen, error && styles.controlInvalid]
            .filter(Boolean)
            .join(' ')}
          onClick={() => {
            if (disabled) return;
            setOpen(true);
            inputRef.current?.focus();
          }}
        >
          {isMulti &&
            selectedValues.map((value) => (
              <span className={styles.chip} key={value}>
                {labelFor(value)}
                <button
                  type="button"
                  className={styles.chipRemove}
                  aria-label={`Remove ${labelFor(value)}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    removeValue(value);
                  }}
                >
                  <X size={12} />
                </button>
              </span>
            ))}

          <input
            ref={inputRef}
            className={styles.search}
            disabled={disabled}
            value={open ? query : isMulti ? query : singleSelectedLabel}
            placeholder={
              isMulti
                ? selectedValues.length > 0
                  ? 'Add another…'
                  : (placeholder ?? 'Search…')
                : (placeholder ?? 'Search…')
            }
            onChange={(e) => {
              setQuery(e.target.value);
              setOpen(true);
            }}
            onFocus={() => setOpen(true)}
            onKeyDown={onKeyDown}
          />

          {!isMulti && selectedValues.length > 0 ? (
            <button
              type="button"
              className={styles.clear}
              aria-label="Clear selection"
              onClick={(e) => {
                e.stopPropagation();
                removeValue(selectedValues[0]);
              }}
            >
              <X size={14} />
            </button>
          ) : (
            <ChevronDown size={14} className={styles.clear} />
          )}
        </div>

        {open && (
          <div className={styles.menu}>
            {suggestions.length === 0 ? (
              <div className={styles.empty}>No matches for “{query}”</div>
            ) : (
              <>
                {suggestions.map((option, index) => {
                  const selected = selectedValues.includes(option.value);
                  return (
                    <button
                      type="button"
                      key={option.value}
                      className={[
                        styles.option,
                        index === activeIndex && styles.optionActive,
                        selected && styles.optionSelected,
                      ]
                        .filter(Boolean)
                        .join(' ')}
                      onMouseEnter={() => setActiveIndex(index)}
                      onClick={() => commit(option)}
                    >
                      <span>{option.label}</span>
                      {option.meta && <span className={styles.optionMeta}>{option.meta}</span>}
                      {selected && <span className={styles.optionMeta}>Selected</span>}
                    </button>
                  );
                })}
                {totalMatches > MAX_SUGGESTIONS && (
                  <div className={styles.footer}>
                    Showing top {MAX_SUGGESTIONS} of {totalMatches} — keep typing to narrow
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </FieldWrapper>
  );
}
