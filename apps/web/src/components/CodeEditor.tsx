import Editor from '@monaco-editor/react';
import type * as Monaco from 'monaco-editor/editor';
import { useEffect, useRef } from 'react';
import '../monaco/setup';
import type { Theme } from '../theme/theme';

interface Props {
  path: string;
  value: string;
  readOnly: boolean;
  theme: Theme;
  onChange: (value: string) => void;
  onRun: () => void;
}

/** The Monaco editor, loaded on demand: syntax highlighting only; errors show up on running. */
export default function CodeEditor({ path, value, readOnly, theme, onChange, onRun }: Props) {
  const run = useRef(onRun);
  useEffect(() => {
    run.current = onRun;
  }, [onRun]);

  const mount = (editor: Monaco.editor.IStandaloneCodeEditor, monaco: typeof Monaco) => {
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => {
      run.current();
    });
  };

  return (
    <Editor
      height="100%"
      language="java"
      theme={theme === 'dark' ? 'vs-dark' : 'vs'}
      path={path}
      value={value}
      onChange={(next) => {
        onChange(next ?? '');
      }}
      onMount={mount}
      options={{
        readOnly,
        fontFamily: getComputedStyle(document.documentElement).getPropertyValue('--font-code'),
        fontSize: 14,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        automaticLayout: true,
        tabSize: 2,
        ariaLabel: path,
      }}
    />
  );
}
