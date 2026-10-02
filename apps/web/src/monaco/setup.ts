import { loader } from '@monaco-editor/react';
import * as monaco from 'monaco-editor/editor';
import editorWorker from 'monaco-editor/editor/editor.worker.start?worker';
// Only what a Java editor for beginners needs: the whole of Monaco would bundle every language.
import 'monaco-editor/features/bracketMatching/register';
import 'monaco-editor/features/caretOperations/register';
import 'monaco-editor/features/clipboard/register';
import 'monaco-editor/features/comment/register';
import 'monaco-editor/features/contextmenu/register';
import 'monaco-editor/features/cursorUndo/register';
import 'monaco-editor/features/find/register';
import 'monaco-editor/features/indentation/register';
import 'monaco-editor/features/lineSelection/register';
import 'monaco-editor/features/linesOperations/register';
import 'monaco-editor/features/multicursor/register';
import 'monaco-editor/features/readOnlyMessage/register';
import 'monaco-editor/features/smartSelect/register';
import 'monaco-editor/features/toggleTabFocusMode/register';
import 'monaco-editor/features/tokenization/register';
import 'monaco-editor/features/wordHighlighter/register';
import 'monaco-editor/features/wordOperations/register';
import 'monaco-editor/languages/definitions/java/register';

// Monaco is bundled with the app, not loaded from a CDN: no requests to third parties.
self.MonacoEnvironment = {
  getWorker: () => new editorWorker(),
};
loader.config({ monaco });
