import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router';
import { App } from '../App';

/** The whole app, starting at a route. */
export function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  );
}

export function renderAt(path: string, element: ReactElement) {
  return render(<MemoryRouter initialEntries={[path]}>{element}</MemoryRouter>);
}
