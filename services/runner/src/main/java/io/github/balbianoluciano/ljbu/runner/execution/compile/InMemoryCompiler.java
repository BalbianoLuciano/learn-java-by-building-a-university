package io.github.balbianoluciano.ljbu.runner.execution.compile;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import io.github.balbianoluciano.ljbu.runner.execution.SourceFile;
import io.github.balbianoluciano.ljbu.runner.execution.compile.SourceFacts.ClassFacts;
import io.github.balbianoluciano.ljbu.runner.execution.compile.SourceFacts.MethodFacts;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.Diagnostic;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Compiles learner code in memory (docs/SECURITY.md §3, layer 2): nothing is read from or written
 * to disk, and annotation processors never run.
 */
public final class InMemoryCompiler {

  public static final String PACKAGE_NOT_ALLOWED = "ljbu.err.package.not.allowed";
  public static final String COMPILATION_TIMED_OUT = "ljbu.err.compilation.timed.out";

  private static final List<String> OPTIONS =
      List.of("--release", "25", "-proc:none", "-Xlint:none", "-implicit:none", "-g");
  private static final long TIMEOUT_SECONDS = 10;

  private final ExecutorService workers =
      Executors.newCachedThreadPool(
          task -> {
            Thread thread = new Thread(task, "ljbu-compiler");
            thread.setDaemon(true);
            return thread;
          });

  public CompilationResult compile(List<SourceFile> files) {
    Future<CompilationResult> compilation = workers.submit(() -> compileNow(files));
    try {
      return compilation.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      compilation.cancel(true);
      return new CompilationResult.Failure(
          List.of(
              new Diagnostic(
                  files.getFirst().path(), 1, 1, COMPILATION_TIMED_OUT, "compilation timed out")));
    } catch (InterruptedException e) {
      compilation.cancel(true);
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while compiling", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("The compiler failed", e.getCause());
    }
  }

  private static CompilationResult compileNow(List<SourceFile> files) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
    try (StandardJavaFileManager standard =
        compiler.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8)) {
      standard.setLocation(StandardLocation.CLASS_PATH, List.of());
      MemoryFileManager fileManager = new MemoryFileManager(standard);
      List<SourceObject> units = files.stream().map(SourceObject::new).toList();
      JavacTask task =
          (JavacTask)
              compiler.getTask(Writer.nullWriter(), fileManager, collector, OPTIONS, null, units);
      task.setLocale(Locale.ROOT);

      List<CompilationUnitTree> trees = new ArrayList<>();
      task.parse().forEach(trees::add);
      List<Diagnostic> packages = packageDeclarations(trees, Trees.instance(task));
      if (!packages.isEmpty()) {
        return new CompilationResult.Failure(packages);
      }
      List<Diagnostic> errors = errors(collector, files);
      if (errors.isEmpty()) {
        task.analyze();
        errors = errors(collector, files);
      }
      if (!errors.isEmpty()) {
        return new CompilationResult.Failure(errors);
      }
      SourceFacts facts = new FactsCollector(task).collect(trees);
      task.generate();
      errors = errors(collector, files);
      if (!errors.isEmpty()) {
        return new CompilationResult.Failure(errors);
      }
      return new CompilationResult.Success(fileManager.classes(), facts);
    }
  }

  /** Learner code lives in the unnamed package (docs/specs/challenge-format.md). */
  private static List<Diagnostic> packageDeclarations(List<CompilationUnitTree> trees, Trees api) {
    List<Diagnostic> found = new ArrayList<>();
    for (CompilationUnitTree unit : trees) {
      if (unit.getPackage() != null) {
        long start = api.getSourcePositions().getStartPosition(unit, unit.getPackage());
        int line = start < 0 ? 1 : (int) unit.getLineMap().getLineNumber(start);
        found.add(
            new Diagnostic(
                fileName(unit.getSourceFile()),
                line,
                1,
                PACKAGE_NOT_ALLOWED,
                "package declarations are not allowed"));
      }
    }
    return found;
  }

  private static List<Diagnostic> errors(
      DiagnosticCollector<JavaFileObject> collector, List<SourceFile> files) {
    List<Diagnostic> errors = new ArrayList<>();
    for (javax.tools.Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
      if (diagnostic.getKind() != javax.tools.Diagnostic.Kind.ERROR) {
        continue;
      }
      String file =
          diagnostic.getSource() == null
              ? files.getFirst().path()
              : fileName(diagnostic.getSource());
      errors.add(
          new Diagnostic(
              file,
              (int) Math.max(1, diagnostic.getLineNumber()),
              (int) Math.max(1, diagnostic.getColumnNumber()),
              diagnostic.getCode(),
              diagnostic.getMessage(Locale.ROOT)));
    }
    return errors;
  }

  private static String fileName(FileObject file) {
    String path = file.toUri().getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private static final class SourceObject extends SimpleJavaFileObject {
    private final String content;

    SourceObject(SourceFile file) {
      super(URI.create("string:///" + file.path()), Kind.SOURCE);
      this.content = file.content();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return content;
    }
  }

  private static final class ClassObject extends SimpleJavaFileObject {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    ClassObject(String className) {
      super(URI.create("bytes:///" + className.replace('.', '/') + ".class"), Kind.CLASS);
    }

    @Override
    public OutputStream openOutputStream() {
      return bytes;
    }
  }

  private static final class MemoryFileManager
      extends ForwardingJavaFileManager<StandardJavaFileManager> {
    private final Map<String, ClassObject> outputs = new LinkedHashMap<>();

    MemoryFileManager(StandardJavaFileManager delegate) {
      super(delegate);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(
        JavaFileManager.Location location,
        String className,
        JavaFileObject.Kind kind,
        FileObject sibling) {
      return outputs.computeIfAbsent(className, ClassObject::new);
    }

    Map<String, byte[]> classes() {
      Map<String, byte[]> classes = new LinkedHashMap<>();
      outputs.forEach((name, object) -> classes.put(name, object.bytes.toByteArray()));
      return classes;
    }
  }

  /** Reads declaration lines and {@code @Override} from the attributed syntax trees. */
  private static final class FactsCollector {
    private final Trees trees;
    private final SourcePositions positions;
    private final Elements elements;
    private final Types types;
    private final Map<String, ClassFacts> classes = new LinkedHashMap<>();

    FactsCollector(JavacTask task) {
      this.trees = Trees.instance(task);
      this.positions = trees.getSourcePositions();
      this.elements = task.getElements();
      this.types = task.getTypes();
    }

    SourceFacts collect(List<CompilationUnitTree> units) {
      for (CompilationUnitTree unit : units) {
        new TreePathScanner<Void, Void>() {
          @Override
          public Void visitClass(ClassTree node, Void unused) {
            if (trees.getElement(getCurrentPath()) instanceof TypeElement type) {
              record(unit, getCurrentPath(), node, type);
            }
            return super.visitClass(node, unused);
          }
        }.scan(unit, null);
      }
      return new SourceFacts(classes);
    }

    private void record(CompilationUnitTree unit, TreePath path, ClassTree node, TypeElement type) {
      int classLine = declarationLine(unit, node, node.getModifiers(), 1);
      Map<String, Integer> fieldLines = new LinkedHashMap<>();
      Map<String, MethodFacts> methods = new LinkedHashMap<>();
      for (Tree member : node.getMembers()) {
        if (member instanceof VariableTree field) {
          fieldLines.put(
              field.getName().toString(),
              declarationLine(unit, field, field.getModifiers(), classLine));
        } else if (member instanceof MethodTree method
            && trees.getElement(new TreePath(path, member)) instanceof ExecutableElement element) {
          methods.put(
              element.getSimpleName() + descriptor(element, type),
              new MethodFacts(
                  declarationLine(unit, method, method.getModifiers(), classLine),
                  element.getAnnotation(Override.class) != null));
        }
      }
      classes.put(
          elements.getBinaryName(type).toString(),
          new ClassFacts(fileName(unit.getSourceFile()), classLine, fieldLines, methods));
    }

    /** The line where the declaration starts, after its annotations. */
    private int declarationLine(
        CompilationUnitTree unit, Tree tree, ModifiersTree modifiers, int fallback) {
      long position = positions.getStartPosition(unit, tree);
      for (AnnotationTree annotation : modifiers.getAnnotations()) {
        position = Math.max(position, positions.getEndPosition(unit, annotation));
      }
      if (position < 0) {
        return fallback;
      }
      try {
        CharSequence source = unit.getSourceFile().getCharContent(true);
        while (position < source.length()
            && Character.isWhitespace(source.charAt((int) position))) {
          position++;
        }
      } catch (IOException e) {
        return fallback;
      }
      return (int) unit.getLineMap().getLineNumber(position);
    }

    private String descriptor(ExecutableElement method, TypeElement owner) {
      StringBuilder descriptor = new StringBuilder("(");
      if (method.getKind() == ElementKind.CONSTRUCTOR && owner.getKind() == ElementKind.ENUM) {
        // javac prepends the name and the ordinal to every enum constructor.
        descriptor.append("Ljava/lang/String;I");
      }
      for (VariableElement parameter : method.getParameters()) {
        descriptor.append(descriptor(types.erasure(parameter.asType())));
      }
      return descriptor
          .append(')')
          .append(descriptor(types.erasure(method.getReturnType())))
          .toString();
    }

    private String descriptor(TypeMirror type) {
      return switch (type.getKind()) {
        case BOOLEAN -> "Z";
        case BYTE -> "B";
        case SHORT -> "S";
        case INT -> "I";
        case LONG -> "J";
        case CHAR -> "C";
        case FLOAT -> "F";
        case DOUBLE -> "D";
        case VOID -> "V";
        case ARRAY -> "[" + descriptor(((ArrayType) type).getComponentType());
        case DECLARED ->
            "L"
                + elements
                    .getBinaryName((TypeElement) ((DeclaredType) type).asElement())
                    .toString()
                    .replace('.', '/')
                + ";";
        default -> "Ljava/lang/Object;";
      };
    }
  }
}
