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

const highlighted = vi.fn();
vi.mock('../components/CodeEditor', () => ({
  default: ({ path, highlightLine }: { path: string; highlightLine?: number }) => {
    highlighted(path, highlightLine);
    return <textarea aria-label={path} />;
  },
}));
// WebGL does not exist in jsdom: the scene is covered by its own unit tests and by hand.
vi.mock('../scene/SceneView', () => ({
  default: ({ label }: { label: string }) => <div role="img" aria-label={label} />,
}));

const SOURCES = { 'Main.java': SOLVED_MAIN, 'FacultadRegional.java': FACULTAD };

/** The challenge screen after a run: model, log and timeline next to the code (DESIGN.md §A2). */
describe('Workbench with a result', () => {
  beforeEach(() => {
    fakeApi({ '/modules': modules, '/challenges/m1-03': aliasing });
    highlighted.mockClear();
  });

  it('redirects the old result route to the challenge', async () => {
    renderApp('/desafios/m1-03/resultado');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'La facultad donde estudiás' }),
    ).toBeInTheDocument();
    expect(screen.getByText(/Ejecutá el código para ver la maqueta/)).toBeInTheDocument();
  });

  it('shows the outcome, the model and the log of the last run next to the code', async () => {
    useResults.getState().setRun('m1-03', { result: incompleteResult, sources: SOURCES });

    renderApp('/desafios/m1-03');

    expect(await screen.findByText(/Obra en construcción \(0\/2\)/)).toBeInTheDocument();
    expect(
      await screen.findByRole('img', {
        name: 'Maqueta del resultado: Obra en construcción (0/2). 1 piezas. La bitácora describe cada una.',
      }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('Main.java')).toBeInTheDocument();
    const log = within(screen.getByRole('complementary'));
    const entries = log.getAllByRole('listitem');
    expect(entries).toHaveLength(2);
    expect(entries[0]).toHaveTextContent('Creaste una segunda facultad');
    expect(entries[0]).toHaveTextContent(
      'Pista: ¿Qué tenés que poner a la derecha del = para no crear otra facultad?',
    );
    // The brief stays folded once there is a result; its summary still shows the progress.
    expect(screen.getByText('0/2')).toBeInTheDocument();
  });

  it('highlights the line a log entry points to in the editor when its chip is chosen', async () => {
    useResults.getState().setRun('m1-03', { result: passedResult, sources: SOURCES });
    const user = userEvent.setup();
    renderApp('/desafios/m1-03');

    const chip = await screen.findByRole('button', { name: 'Línea 8 de Main.java' });
    expect(chip).toHaveTextContent('Main.java:8');
    await user.click(chip);

    expect(highlighted).toHaveBeenLastCalledWith('Main.java', 8);
    expect(chip).toHaveAttribute('aria-pressed', 'true');
  });

  it('walks the execution with the arrow keys', async () => {
    useResults.getState().setRun('m1-03', { result: passedResult, sources: SOURCES });
    const user = userEvent.setup();
    renderApp('/desafios/m1-03');
    const timeline = await screen.findByRole('group', { name: 'Línea de tiempo' });

    timeline.focus();
    await user.keyboard('{ArrowRight}{ArrowRight}{ArrowRight}');

    expect(
      screen.getByText(/Paso 3 \/ 4 · Main\.java:7 · La variable miFacultad recibió un valor/),
    ).toBeInTheDocument();
    expect(highlighted).toHaveBeenLastCalledWith('Main.java', 7);
  });

  it('opens the legend of the model', async () => {
    useResults.getState().setRun('m1-03', { result: passedResult, sources: SOURCES });
    const user = userEvent.setup();
    renderApp('/desafios/m1-03');

    await user.click(await screen.findByRole('button', { name: '¿Qué es cada forma?' }));

    const legend = screen.getByRole('dialog', { name: 'Cómo se ve Java en la maqueta' });
    expect(within(legend).getByText('Plano')).toBeInTheDocument();
    expect(within(legend).getByText('Ventanilla')).toBeInTheDocument();
  });

  it('has no accessibility violations', async () => {
    useResults.getState().setRun('m1-03', { result: incompleteResult, sources: SOURCES });
    const { container } = renderApp('/desafios/m1-03');
    await screen.findByText(/Obra en construcción/);

    expect(await axe(container)).toHaveNoViolations();
  });
});
