import { Check } from 'lucide-react';
import { Fragment } from 'react';
import styles from './Stepper.module.css';

export interface Step {
  label: string;
  description?: string;
}

interface StepperProps {
  steps: Step[];
  current: number;
  furthestReached: number;
  onStepClick?: (index: number) => void;
}

/** Horizontal step indicator. Completed steps are clickable so you can go back and edit. */
export function Stepper({ steps, current, furthestReached, onStepClick }: StepperProps) {
  return (
    <nav className={styles.stepper} aria-label="Progress">
      {steps.map((step, index) => {
        const complete = index < current;
        const active = index === current;
        const reachable = index <= furthestReached;

        return (
          <Fragment key={step.label}>
            {index > 0 && (
              <span className={[styles.connector, complete && styles.connectorDone].filter(Boolean).join(' ')} />
            )}
            <button
              type="button"
              className={[styles.step, active && styles.stepActive, complete && styles.stepComplete]
                .filter(Boolean)
                .join(' ')}
              disabled={!reachable || !onStepClick}
              onClick={() => onStepClick?.(index)}
              aria-current={active ? 'step' : undefined}
            >
              <span className={styles.marker}>{complete ? <Check size={15} /> : index + 1}</span>
              <span className={styles.labels}>
                <span className={styles.stepLabel}>{step.label}</span>
                {step.description && <span className={styles.stepDesc}>{step.description}</span>}
              </span>
            </button>
          </Fragment>
        );
      })}
    </nav>
  );
}
