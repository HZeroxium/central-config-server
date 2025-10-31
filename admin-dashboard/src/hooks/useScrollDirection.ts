import { useState, useEffect, useRef } from "react";

interface UseScrollDirectionOptions {
  /**
   * Threshold in pixels before detecting scroll direction change
   * @default 10
   */
  threshold?: number;
  /**
   * Initial visibility state
   * @default true
   */
  initialVisible?: boolean;
}

interface UseScrollDirectionResult {
  /**
   * Whether the header should be visible
   */
  isVisible: boolean;
  /**
   * Current scroll direction ('up' | 'down')
   */
  direction: "up" | "down";
  /**
   * Current scroll Y position
   */
  scrollY: number;
}

/**
 * Hook to detect scroll direction and control header visibility
 * Header hides on scroll down, shows on scroll up
 */
export function useScrollDirection(
  options: UseScrollDirectionOptions = {}
): UseScrollDirectionResult {
  const { threshold = 10, initialVisible = true } = options;

  const [isVisible, setIsVisible] = useState(initialVisible);
  const [direction, setDirection] = useState<"up" | "down">("up");
  const [scrollY, setScrollY] = useState(0);

  const lastScrollY = useRef(0);
  const ticking = useRef(false);

  useEffect(() => {
    const handleScroll = () => {
      if (!ticking.current) {
        window.requestAnimationFrame(() => {
          const currentScrollY = window.scrollY;

          // Determine scroll direction
          if (currentScrollY > lastScrollY.current + threshold) {
            // Scrolling down
            setDirection("down");
            setIsVisible(false);
          } else if (currentScrollY < lastScrollY.current - threshold) {
            // Scrolling up
            setDirection("up");
            setIsVisible(true);
          }

          // Always show header at top of page
          if (currentScrollY <= threshold) {
            setIsVisible(true);
          }

          lastScrollY.current = currentScrollY;
          setScrollY(currentScrollY);
          ticking.current = false;
        });

        ticking.current = true;
      }
    };

    // Set initial scroll position
    lastScrollY.current = window.scrollY;
    setScrollY(window.scrollY);

    window.addEventListener("scroll", handleScroll, { passive: true });

    return () => {
      window.removeEventListener("scroll", handleScroll);
    };
  }, [threshold]);

  return { isVisible, direction, scrollY };
}
