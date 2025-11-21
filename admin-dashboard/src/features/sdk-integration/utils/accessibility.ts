/**
 * Accessibility utilities for SDK integration wizard.
 */

/**
 * Announces a message to screen readers.
 */
export function announceToScreenReader(message: string, priority: 'polite' | 'assertive' = 'polite'): void {
  const announcement = document.createElement('div');
  announcement.setAttribute('role', 'status');
  announcement.setAttribute('aria-live', priority);
  announcement.setAttribute('aria-atomic', 'true');
  announcement.className = 'sr-only';
  announcement.textContent = message;
  
  document.body.appendChild(announcement);
  
  // Remove after announcement is read
  setTimeout(() => {
    document.body.removeChild(announcement);
  }, 1000);
}

/**
 * Announces step change to screen readers.
 */
export function announceStepChange(stepNumber: number, stepName: string): void {
  announceToScreenReader(`Step ${stepNumber}: ${stepName}`, 'polite');
}

/**
 * Announces generation completion.
 */
export function announceGenerationComplete(stepName: string): void {
  announceToScreenReader(`${stepName} generation completed successfully`, 'polite');
}

/**
 * Announces error to screen readers.
 */
export function announceError(message: string): void {
  announceToScreenReader(`Error: ${message}`, 'assertive');
}

/**
 * Gets ARIA label for step button.
 */
export function getStepAriaLabel(stepNumber: number, stepName: string, isActive: boolean, isCompleted: boolean): string {
  let label = `Step ${stepNumber}: ${stepName}`;
  if (isActive) {
    label += ', current step';
  } else if (isCompleted) {
    label += ', completed';
  } else {
    label += ', not started';
  }
  return label;
}

/**
 * Focuses element with smooth scroll.
 */
export function focusElement(elementId: string, options?: { behavior?: ScrollBehavior; block?: ScrollLogicalPosition }): void {
  const element = document.getElementById(elementId);
  if (element) {
    element.focus({ preventScroll: false });
    element.scrollIntoView({
      behavior: options?.behavior || 'smooth',
      block: options?.block || 'center',
    });
  }
}

