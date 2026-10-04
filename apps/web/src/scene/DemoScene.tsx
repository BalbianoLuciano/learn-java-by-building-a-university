import { Suspense, lazy, useEffect, useMemo, useState } from 'react';
import { demoResult } from './demo';
import { useReducedMotion } from './motion';

const SceneView = lazy(() => import('./SceneView'));

/** The model of the landing page, replaying its program in a loop (decorative). */
export function DemoScene() {
  const result = useMemo(() => demoResult(), []);
  const reducedMotion = useReducedMotion();
  const [step, setStep] = useState<number | undefined>(reducedMotion ? undefined : 0);
  useEffect(() => {
    if (reducedMotion) {
      return;
    }
    const timer = window.setInterval(() => {
      setStep((current) => {
        const next = (current ?? -1) + 1;
        return next >= result.timeline.length + 6 ? 0 : next;
      });
    }, 900);
    return () => {
      window.clearInterval(timer);
    };
  }, [reducedMotion, result.timeline.length]);
  const current = step !== undefined && step < result.timeline.length ? step : undefined;
  return (
    <div aria-hidden style={{ position: 'absolute', inset: 0 }}>
      <Suspense fallback={null}>
        <SceneView
          result={result}
          selection={{ step: current }}
          onSelect={() => undefined}
          label=""
          interactive={false}
        />
      </Suspense>
    </div>
  );
}
