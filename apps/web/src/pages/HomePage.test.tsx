import { screen } from '@testing-library/react';
import { useProgress } from '../state/progress';
import { fakeApi, modules } from '../test/fixtures';
import { renderApp } from '../test/render';

// WebGL does not exist in jsdom: the miniatures are plain boxes here.
vi.mock('../scene/MiniModel', () => ({
  MiniModel: ({ className }: { className?: string }) => <span className={className} />,
}));

describe('HomePage', () => {
  it('lists the modules with the progress of the learner', async () => {
    fakeApi({ '/modules': modules });
    useProgress.getState().recordRun('m1-01', true);

    renderApp('/');

    expect(
      await screen.findByRole('link', { name: /Clases, objetos y referencias/ }),
    ).toHaveTextContent('1 de 3 desafíos');
  });

  it('explains when the api cannot be reached and offers to retry', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new TypeError('offline'))),
    );

    renderApp('/');

    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos hablar con el servidor');
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});

describe('ModulePage', () => {
  it('lists the challenges of a module with their status', async () => {
    fakeApi({ '/modules': modules });
    useProgress.getState().recordSolution('m1-02');
    useProgress.getState().recordRun('m1-02', true);

    renderApp('/modulos/m1');

    const links = await screen.findAllByRole('link', { name: /Desafío/ });
    expect(links).toHaveLength(3);
    expect(links[0]).toHaveTextContent('Pendiente');
    expect(links[1]).toHaveTextContent('Completado con solución');
    expect(links[1]).toHaveAttribute('href', '/desafios/m1-02');
  });
});
