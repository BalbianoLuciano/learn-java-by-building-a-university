import Editor from '@monaco-editor/react';
import type * as Monaco from 'monaco-editor/editor';
import { useEffect, useRef } from 'react';
import '../monaco/setup';
import type { Theme } from '../theme/theme';
import styles from './CodeEditor.module.css';

interface Props {
  path: string;
  value: string;
  readOnly: boolean;
  theme: Theme;
  /** The line the learner chose in the log, the timeline or the model. */
  highlightLine?: number;
  onChange: (value: string) => void;
  onRun: () => void;
}

/** The Monaco editor, loaded on demand: syntax highlighting only; errors show up on running. */
export default function CodeEditor({
  path,
  value,
  readOnly,
  theme,
  highlightLine,
  onChange,
  onRun,
}: Props) {
  const run = useRef(onRun);
  const editorRef = useRef<Monaco.editor.IStandaloneCodeEditor | null>(null);
  const decorations = useRef<Monaco.editor.IEditorDecorationsCollection | null>(null);
  useEffect(() => {
    run.current = onRun;
  }, [onRun]);

  useEffect(() => {
    const editor = editorRef.current;
    if (!editor) {
      return;
    }
    decorations.current?.clear();
    if (highlightLine === undefined) {
      return;
    }
    decorations.current = editor.createDecorationsCollection([
      {
        range: {
          startLineNumber: highlightLine,
          startColumn: 1,
          endLineNumber: highlightLine,
          endColumn: 1,
        },
        options: {
          isWholeLine: true,
          className: styles.highlight,
          linesDecorationsClassName: styles.gutter,
        },
      },
    ]);
    editor.revealLineInCenterIfOutsideViewport(highlightLine);
  }, [highlightLine, path]);

  const mount = (editor: Monaco.editor.IStandaloneCodeEditor, monaco: typeof Monaco) => {
    editorRef.current = editor;
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
