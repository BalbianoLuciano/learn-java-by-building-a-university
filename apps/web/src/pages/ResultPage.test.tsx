import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'vitest-axe';
import { useResults } from '../state/results';
import {
  aliasing,
  FACULTAD,
  fakeApi,
  incompleteResult,
  modules,
  passedResult,
  SOLVED_MAIN,
} from '../test/fixtures';
import { renderApp } from '../test/render';

vi.mock('../components/CodeEditor', () => ({ default: () => <textarea aria-label="editor" /> }));
// WebGL does not exist in jsdom: the scene is covered by its own unit tests and by hand.
vi.mock('../scene/SceneView', () => ({
  default: ({ label }: { label: string }) => <div role="img" aria-label={label} />,
}));

const SOURCES = { 'Main.java': SOLVED_MAIN, 'FacultadRegional.java': FACULTAD };

describe('ResultPage', () => {
  it('goes back to the editor when there is no result to show', async () => {
    fakeApi({ '/modules': modules, '/challenges/m1-03': aliasing });

    renderApp('/desafios/m1-03/resultado');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'La facultad donde estudiás' }),
    ).toBeInTheDocument();
  });

  it('shows the outcome, the pieces and the log of the last run', async () => {
    useResults.getState().setRun('m1-03', { result: incompleteResult, sources: SOURCES });

    renderApp('/desafios/m1-03/resultado');

    expect(screen.getByText(/Obra en construcción \(0\/2\)/)).toBeInTheDocument();
    expect(
      await screen.findByRole('img', {
        name: 'Maqueta del resultado: Obra en construcción (0/2). 1 piezas. La bitácora describe cada una.',
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Resistencia regional-faculty/ }),
    ).toBeInTheDocument();
    const log = within(screen.getByRole('complementary'));
    const entries = log.getAllByRole('listitem');
    expect(entries).toHaveLength(2);
    expect(entries[0]).toHaveTextContent('Creaste una segunda facultad');
    expect(entries[0]).toHaveTextContent(
      'Pista: ¿Qué tenés que poner a la derecha del = para no crear otra facultad?',
    );
  });

  it('shows the line a log entry points to when its chip is chosen', async () => {
    useResults.getState().setRun('m1-03', { result: passedResult, sources: SOURCES });
    const user = userEvent.setup();
    renderApp('/desafios/m1-03/resultado');

    await user.click(screen.getByRole('button', { name: 'Línea 8 de Main.java' }));

    const code = screen.getByRole('figure');
    expect(code).toHaveTextContent('Main.java');
    const highlighted = code.querySelector('[data-selected="true"]');
    expect(highlighted).toHaveTextContent('miFacultad.provincia = "Chaco";');
    expect(screen.getByRole('button', { name: 'Línea 8 de Main.java' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('walks the execution with the arrow keys', async () => {
    useResults.getState().setRun('m1-03', { result: passedResult, sources: SOURCES });
    const user = userEvent.setup();
    renderApp('/desafios/m1-03/resultado');
    const timeline = screen.getByRole('group', { name: 'Línea de tiempo' });

    timeline.focus();
    await user.keyboard('{ArrowRight}{ArrowRight}{ArrowRight}');

    expect(
      screen.getByText(/Paso 3 \/ 4 · Main\.java:7 · La variable miFacultad recibió un valor/),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /miFacultad variable-sign/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByRole('figure').querySelector('[data-selected="true"]')).toHaveTextContent(
      'miFacultad = resistencia',
    );
  });

  it('has no accessibility violations', async () => {
    useResults.getState().setRun('m1-03', { result: incompleteResult, sources: SOURCES });
    const { container } = renderApp('/desafios/m1-03/resultado');

    expect(await axe(container)).toHaveNoViolations();
  });
});
