import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useAttempts } from '../state/attempts';
import { useProgress } from '../state/progress';
import { useResults } from '../state/results';
import {
  aliasing,
  fakeApi,
  incompleteResult,
  modules,
  passedResult,
  SOLVED_MAIN,
} from '../test/fixtures';
import { renderApp } from '../test/render';

// Monaco does not run in jsdom: the editor becomes a textarea with the same contract.
vi.mock('../components/CodeEditor', () => ({
  default: (props: {
    path: string;
    value: string;
    readOnly: boolean;
    onChange: (value: string) => void;
    onRun: () => void;
  }) => (
    <textarea
      aria-label={props.path}
      value={props.value}
      readOnly={props.readOnly}
      onChange={(event) => {
        props.onChange(event.target.value);
      }}
    />
  ),
}));

describe('ChallengePage', () => {
  it('shows the brief, the goals and the starter code', async () => {
    fakeApi({ '/modules': modules, '/challenges/m1-03': aliasing });

    renderApp('/desafios/m1-03');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'La facultad donde estudiás' }),
    ).toBeInTheDocument();
    await screen.findByLabelText('Main.java');
    expect(screen.getByText(/Declará una variable/)).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(screen.getByRole('tab', { name: /Main\.java/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(screen.getByRole('tab', { name: /FacultadRegional\.java/ })).toHaveTextContent(
      'solo lectura',
    );
    expect(screen.getByLabelText('Main.java')).toHaveValue(aliasing.files[1]?.content);
  });

  it('keeps the code of the learner across reloads', async () => {
    fakeApi({ '/modules': modules, '/challenges/m1-03': aliasing });
    const user = userEvent.setup();
    renderApp('/desafios/m1-03');
    const editor = await screen.findByLabelText('Main.java');

    await user.clear(editor);
    await user.type(editor, 'nuevo');

    expect(useAttempts.getState().files['m1-03']?.['Main.java']).toBe('nuevo');
    expect(localStorage.getItem('ljbu.code.v1')).toContain('nuevo');
  });

  it('runs only the editable files and shows the result', async () => {
    const calls = fakeApi({
      '/modules': modules,
      '/challenges/m1-03': aliasing,
      '/runs': passedResult,
    });
    useAttempts.getState().setFile('m1-03', 'Main.java', SOLVED_MAIN);
    const user = userEvent.setup();
    renderApp('/desafios/m1-03');
    await screen.findByLabelText('Main.java');

    await user.click(screen.getByRole('button', { name: 'Ejecutar' }));

    expect(await screen.findByText(/¡Obra terminada!/)).toBeInTheDocument();
    const run = calls.find((call) => call.url.endsWith('/runs'));
    expect(JSON.parse(run?.init?.body as string)).toEqual({
      challengeId: 'm1-03',
      files: [{ path: 'Main.java', content: SOLVED_MAIN }],
    });
    expect(useProgress.getState().challenges['m1-03']?.status).toBe('completed');
    expect(useResults.getState().runs['m1-03']?.sources['FacultadRegional.java']).toBe(
      aliasing.files[0]?.content,
    );
  });

  it('ticks the goals that the last result passed', async () => {
    fakeApi({ '/modules': modules, '/challenges/m1-03': aliasing });
    useResults.getState().setRun('m1-03', {
      result: { ...incompleteResult, log: passedResult.log.slice(0, 1) },
      sources: {},
    });

    renderApp('/desafios/m1-03');

    const goals = await screen.findAllByRole('listitem');
    const ticked = goals.filter((item) => item.getAttribute('data-done') === 'true');
    expect(ticked).toHaveLength(1);
    expect(ticked[0]).toHaveTextContent('miFacultad apunta a la FR Resistencia');
  });

  it('reveals hints as the rules allow and the solution after confirming', async () => {
    fakeApi({
      '/modules': modules,
      '/challenges/m1-03': aliasing,
      '/challenges/m1-03/hints/1': { level: 1, text: 'Una variable guarda una referencia.' },
      '/challenges/m1-03/solution': {
        files: [{ path: 'Main.java', content: SOLVED_MAIN }],
        explanation: '# Solución\n\nTexto.',
      },
    });
    useProgress.setState({
      challenges: { 'm1-03': { status: 'pending', runs: 1, hintsSeen: 3, solutionSeen: false } },
    });
    const user = userEvent.setup();
    renderApp('/desafios/m1-03');
    await screen.findByLabelText('Main.java');

    await user.click(screen.getByRole('button', { name: 'Pedir pista' }));
    expect(screen.getByRole('button', { name: /Pista 3/ })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Pista 1' }));
    expect(await screen.findByText('Una variable guarda una referencia.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ver solución' }));
    await user.click(screen.getByRole('button', { name: 'Sí, mostrarla' }));

    expect(await screen.findByRole('heading', { name: 'Solución' })).toBeInTheDocument();
    expect(useProgress.getState().challenges['m1-03']?.solutionSeen).toBe(true);
  });

  it('explains a run the api refused', async () => {
    fakeApi({ '/modules': modules, '/challenges/m1-03': aliasing });
    vi.mocked(fetch).mockImplementationOnce(() =>
      Promise.resolve(new Response(JSON.stringify(aliasing), { status: 200 })),
    );
    const user = userEvent.setup();
    renderApp('/desafios/m1-03');
    await screen.findByLabelText('Main.java');
    vi.mocked(fetch).mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify({ status: 429, code: 'rate_limited' }), {
          status: 429,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
      ),
    );

    await user.click(screen.getByRole('button', { name: 'Ejecutar' }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Hiciste muchas ejecuciones seguidas');
    });
  });
});
