import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useProgress } from '../state/progress';
import { aliasing, fakeApi, modules, passedResult, SOLVED_MAIN } from './fixtures';
import { renderApp } from './render';

vi.mock('../scene/SceneView', () => ({ default: () => <div role="img" aria-label="maqueta" /> }));
vi.mock('../components/CodeEditor', () => ({
  default: (props: {
    path: string;
    value: string;
    onChange: (value: string) => void;
    onRun: () => void;
  }) => (
    <textarea
      aria-label={props.path}
      value={props.value}
      onChange={(event) => {
        props.onChange(event.target.value);
      }}
      onKeyDown={(event) => {
        if (event.ctrlKey && event.key === 'Enter') {
          props.onRun();
        }
      }}
    />
  ),
}));

/** The acceptance of M4: challenge 1.3 from start to end, with the keyboard only. */
describe('completing challenge 1.3 with the keyboard', () => {
  it('goes from the home page to the passed result', async () => {
    fakeApi({ '/modules': modules, '/challenges/m1-03': aliasing, '/runs': passedResult });
    const user = userEvent.setup();
    renderApp('/');

    // Home → module → challenge, following links with Tab and Enter.
    await focusAndActivate(user, 'link', /Clases, objetos y referencias/);
    await focusAndActivate(user, 'link', /La facultad donde estudiás/);
    const editor = await screen.findByLabelText('Main.java');

    // Write the solution and run it with Ctrl+Enter from the editor.
    await tabTo(user, editor);
    await user.clear(editor);
    await user.paste(SOLVED_MAIN);
    await user.keyboard('{Control>}{Enter}{/Control}');

    expect(await screen.findByText(/¡Obra terminada!/)).toBeInTheDocument();
    expect(useProgress.getState().challenges['m1-03']?.status).toBe('completed');

    // Read the log and walk the timeline without the mouse, on the same screen as the code.
    await focusAndActivate(user, 'button', 'Línea 7 de Main.java');
    expect(screen.getByRole('button', { name: 'Línea 7 de Main.java' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    await tabTo(user, screen.getByRole('group', { name: 'Línea de tiempo' }));
    await user.keyboard('{ArrowRight}{ArrowRight}');
    expect(screen.getByText(/Paso 2 \/ 4/)).toBeInTheDocument();
  });
});

async function focusAndActivate(
  user: ReturnType<typeof userEvent.setup>,
  role: 'link' | 'button',
  name: string | RegExp,
) {
  const target = await screen.findByRole(role, { name });
  await tabTo(user, target);
  await user.keyboard('{Enter}');
}

/** Presses Tab until the element has focus; fails if it is not reachable. */
async function tabTo(user: ReturnType<typeof userEvent.setup>, target: HTMLElement) {
  for (let presses = 0; presses < 40; presses++) {
    if (document.activeElement === target) {
      return;
    }
    await user.tab();
  }
  throw new Error(
    'Not reachable with Tab: ' + (target.getAttribute('aria-label') ?? target.textContent),
  );
}
