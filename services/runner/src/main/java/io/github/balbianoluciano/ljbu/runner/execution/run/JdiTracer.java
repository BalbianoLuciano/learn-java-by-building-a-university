package io.github.balbianoluciano.ljbu.runner.execution.run;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ModificationWatchpointRequest;
import com.sun.jdi.request.StepRequest;
import io.github.balbianoluciano.ljbu.runner.execution.ExecutionLimits;
import io.github.balbianoluciano.ljbu.runner.execution.trace.HeapObject;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Step;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.ExceededLimit;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.ExceptionInfo;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.Limits;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Trace.Status;
import io.github.balbianoluciano.ljbu.runner.execution.trace.Value;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Runs the learner's {@code Main} in a child JVM and records what it does through JDI: the program
 * is not modified, so the trace is what the JVM really did (ADR 0003). Only events in learner
 * classes are recorded.
 */
public final class JdiTracer {

  private static final List<String> JDK_CLASSES =
      List.of("java.*", "javax.*", "jdk.*", "sun.*", "com.sun.*");
  private static final String MAIN_SIGNATURE = "([Ljava/lang/String;)V";
  private static final long POLL_MS = 20;
  private static final int MAX_FRAMES_SEARCHED = 64;

  private final ExecutionLimits limits;
  private final Set<String> learnerClasses;
  private final ChildJvm child;
  private final VirtualMachine vm;
  private final long deadlineNanos;

  private final ObjectRegistry registry = new ObjectRegistry();
  private final ValueMapper mapper = new ValueMapper(registry);
  private final List<Step> steps = new ArrayList<>();
  private final List<ReferenceType> learnerTypes = new ArrayList<>();
  private final Deque<FrameShadow> frames = new ArrayDeque<>();
  private final StringBuilder stdout = new StringBuilder();
  private final CharsetDecoder decoder =
      StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPLACE)
          .onUnmappableCharacter(CodingErrorAction.REPLACE);
  private byte[] undecoded = new byte[0];
  private int outputBytes;

  private ThreadReference mainThread;
  private int mainDepth = -1;
  private Position current;
  private ExceptionInfo fatalException;

  /** Set when an exception may have popped frames without an exit event. */
  private boolean framesMayBeStale;

  private JdiTracer(
      ChildJvm child,
      VirtualMachine vm,
      Set<String> learnerClasses,
      ExecutionLimits limits,
      long deadlineNanos) {
    this.child = child;
    this.vm = vm;
    this.learnerClasses = learnerClasses;
    this.limits = limits;
    this.deadlineNanos = deadlineNanos;
  }

  /**
   * @param classDirectory directory with the compiled learner classes, the only class path entry
   * @param learnerClasses binary names of those classes
   */
  public static RunOutcome run(
      Path classDirectory, Set<String> learnerClasses, ExecutionLimits limits)
      throws IOException, InterruptedException {
    long deadline = System.nanoTime() + limits.timeoutMs() * 1_000_000L;
    try (ChildJvm child = ChildJvm.start(classDirectory, limits)) {
      VirtualMachine vm = attach(child.debugPort());
      try {
        return new JdiTracer(child, vm, learnerClasses, limits, deadline).trace();
      } finally {
        child.close();
        try {
          vm.dispose();
        } catch (RuntimeException e) {
          // Already disconnected.
        }
      }
    }
  }

  private static VirtualMachine attach(int port) throws IOException {
    AttachingConnector connector =
        Bootstrap.virtualMachineManager().attachingConnectors().stream()
            .filter(candidate -> candidate.name().equals("com.sun.jdi.SocketAttach"))
            .findFirst()
            .orElseThrow(() -> new IOException("JDI socket connector not available"));
    Map<String, Connector.Argument> arguments = connector.defaultArguments();
    arguments.get("hostname").setValue("127.0.0.1");
    arguments.get("port").setValue(Integer.toString(port));
    arguments.get("timeout").setValue("5000");
    try {
      return connector.attach(arguments);
    } catch (IllegalConnectorArgumentsException e) {
      throw new IOException("Cannot attach to the child JVM", e);
    }
  }

  // --- Event loop

  /** How the execution ended. */
  private record Finish(Status status, ExceededLimit exceeded) {}

  /** Thrown from any depth of event handling when a limit cuts the program. */
  private static final class LimitReached extends RuntimeException {
    private final ExceededLimit limit;

    LimitReached(ExceededLimit limit) {
      super(null, null, false, false);
      this.limit = limit;
    }
  }

  private RunOutcome trace() throws InterruptedException {
    for (String className : learnerClasses) {
      ClassPrepareRequest request = vm.eventRequestManager().createClassPrepareRequest();
      request.addClassFilter(className);
      request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
      request.enable();
    }

    Finish finish;
    try {
      finish = eventLoop();
    } catch (LimitReached reached) {
      finish = new Finish(Status.LIMIT_EXCEEDED, reached.limit);
    } catch (VMDisconnectedException e) {
      finish = new Finish(Status.RUNTIME_ERROR, null);
    }

    HeapReader reader = new HeapReader(registry, mapper, learnerClasses);
    Map<String, HeapObject> heap;
    Map<String, Map<String, Value>> statics;
    try {
      if (finish.status() == Status.TIMEOUT) {
        vm.suspend();
      }
      heap = reader.readHeap();
      statics = reader.readStatics(learnerTypes);
    } catch (RuntimeException e) {
      // The JVM is gone: the objects are known, their final state is not.
      heap = new LinkedHashMap<>();
      for (int i = 0; i < registry.size(); i++) {
        heap.put(registry.id(i), new HeapObject.Instance("Object", Map.of()));
      }
      statics = Map.of();
    }

    boolean endedOnItsOwn =
        finish.status() == Status.COMPLETED
            || (finish.status() == Status.RUNTIME_ERROR && fatalException != null);
    return new RunOutcome(
        finish.status(),
        stdout.toString(),
        steps,
        heap,
        statics,
        fatalException,
        new Limits(steps.size(), !endedOnItsOwn, finish.exceeded()));
  }

  private Finish eventLoop() throws InterruptedException {
    vm.resume();
    while (true) {
      long remainingMs = (deadlineNanos - System.nanoTime()) / 1_000_000;
      if (remainingMs <= 0) {
        return new Finish(Status.TIMEOUT, null);
      }
      EventSet events = vm.eventQueue().remove(Math.min(remainingMs, POLL_MS));
      if (events == null) {
        // The program may be printing in a loop without any other event.
        drainOutput();
        continue;
      }
      for (Event event : events) {
        Finish finish = handle(event);
        if (finish != null) {
          return finish;
        }
      }
      events.resume();
    }
  }

  private Finish handle(Event event) {
    try {
      return switch (event) {
        case ClassPrepareEvent prepared -> onClassPrepared(prepared);
        case MethodEntryEvent entry -> onMethodEntry(entry);
        case MethodExitEvent exit -> onMethodExit(exit);
        case StepEvent step -> onStep(step);
        case ModificationWatchpointEvent write -> onFieldWrite(write);
        case ExceptionEvent thrown -> onException(thrown);
        // The JVM died before main returned: a crash or a kill.
        case VMDeathEvent death -> new Finish(Status.RUNTIME_ERROR, null);
        case VMDisconnectEvent disconnect -> new Finish(Status.RUNTIME_ERROR, null);
        default -> null;
      };
    } catch (com.sun.jdi.IncompatibleThreadStateException | AbsentInformationException e) {
      // The frame could not be inspected; the event is skipped and the program goes on.
      return null;
    }
  }

  // --- Event handlers

  private Finish onClassPrepared(ClassPrepareEvent event) {
    ReferenceType type = event.referenceType();
    if (!learnerClasses.contains(type.name())) {
      return null;
    }
    learnerTypes.add(type);
    if (mainThread == null) {
      mainThread = event.thread();
      startTracing();
    }
    EventRequestManager requests = vm.eventRequestManager();
    for (Field field : type.fields()) {
      if (!field.isSynthetic()) {
        ModificationWatchpointRequest request = requests.createModificationWatchpointRequest(field);
        request.addThreadFilter(mainThread);
        request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        request.enable();
      }
    }
    return null;
  }

  /** Called when the first learner class is prepared: from here on the main thread is traced. */
  private void startTracing() {
    unbufferStandardOutput();
    EventRequestManager requests = vm.eventRequestManager();

    MethodEntryRequest entries = requests.createMethodEntryRequest();
    JDK_CLASSES.forEach(entries::addClassExclusionFilter);
    entries.addThreadFilter(mainThread);
    entries.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    entries.enable();

    MethodExitRequest exits = requests.createMethodExitRequest();
    JDK_CLASSES.forEach(exits::addClassExclusionFilter);
    exits.addThreadFilter(mainThread);
    exits.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    exits.enable();

    StepRequest lines =
        requests.createStepRequest(mainThread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
    JDK_CLASSES.forEach(lines::addClassExclusionFilter);
    lines.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    lines.enable();

    ExceptionRequest exceptions = requests.createExceptionRequest(null, true, true);
    exceptions.addThreadFilter(mainThread);
    exceptions.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    exceptions.enable();
  }

  /**
   * Replaces System.out with a stream that writes straight to the file descriptor. The default one
   * buffers until a line ends, which would tie the text of a {@code print} to a later line.
   */
  private void unbufferStandardOutput() {
    try {
      ClassType descriptorType = classType("java.io.FileDescriptor");
      ClassType fileStreamType = classType("java.io.FileOutputStream");
      ClassType printStreamType = classType("java.io.PrintStream");
      ClassType systemType = classType("java.lang.System");
      int single = ObjectReference.INVOKE_SINGLE_THREADED;

      com.sun.jdi.Value descriptor = descriptorType.getValue(descriptorType.fieldByName("out"));
      ObjectReference fileStream =
          fileStreamType.newInstance(
              mainThread,
              fileStreamType.concreteMethodByName("<init>", "(Ljava/io/FileDescriptor;)V"),
              List.of(descriptor),
              single);
      fileStream.disableCollection();
      ObjectReference printStream =
          printStreamType.newInstance(
              mainThread,
              printStreamType.concreteMethodByName("<init>", "(Ljava/io/OutputStream;Z)V"),
              List.of(fileStream, vm.mirrorOf(true)),
              single);
      printStream.disableCollection();
      systemType.invokeMethod(
          mainThread,
          systemType.concreteMethodByName("setOut", "(Ljava/io/PrintStream;)V"),
          List.of(printStream),
          single);
      fileStream.enableCollection();
      printStream.enableCollection();
    } catch (VMDisconnectedException e) {
      throw e;
    } catch (Exception e) {
      // The default stream stays: output is still captured, only less precisely placed.
    }
  }

  private ClassType classType(String name) {
    return (ClassType) vm.classesByName(name).getFirst();
  }

  private Finish onMethodEntry(MethodEntryEvent event)
      throws com.sun.jdi.IncompatibleThreadStateException, AbsentInformationException {
    Method method = event.method();
    if (!isLearner(method.declaringType())) {
      return null;
    }
    drainOutput();
    ThreadReference thread = event.thread();
    int depth = thread.frameCount();
    List<StackFrame> top = thread.frames(0, Math.min(depth, 2));
    StackFrame frame = top.getFirst();
    Position entry = position(event.location());
    Position callSite = entry;
    if (top.size() > 1 && isLearner(top.getLast().location().declaringType())) {
      callSite = position(top.getLast().location());
    }

    boolean isMain =
        mainDepth < 0
            && method.isStatic()
            && method.name().equals("main")
            && method.signature().equals(MAIN_SIGNATURE)
            && method.declaringType().name().equals("Main");
    if (isMain) {
      mainDepth = depth;
    }
    if (mainDepth > 0 && depth - mainDepth >= limits.maxCallDepth()) {
      // Runaway recursion: stepping gets slower with every frame, so it is cut long before the
      // stack overflows.
      throw new LimitReached(ExceededLimit.CALL_DEPTH);
    }

    ObjectReference self = method.isStatic() ? null : frame.thisObject();
    String target = null;
    if (self != null) {
      boolean created = method.isConstructor() && !registry.knows(self);
      target = idOf(self);
      if (created) {
        String id = target;
        String type = self.referenceType().name();
        Position at = callSite;
        record(
            index ->
                new Step.ObjectCreated(index, at.file(), at.line(), new Step.ObjectRef(id, type)));
      }
    }
    if (isReported(method)) {
      // The String[] of main is not something the learner created: it stays out of the trace.
      List<Value> arguments =
          isMain ? List.of() : frame.getArgumentValues().stream().map(this::toValue).toList();
      String name = name(method);
      String receiver = target;
      Position at = callSite;
      record(index -> new Step.Call(index, at.file(), at.line(), name, receiver, arguments));
    }
    shadowOf(frame, depth, method, entry);
    current = entry;
    return null;
  }

  private Finish onStep(StepEvent event)
      throws com.sun.jdi.IncompatibleThreadStateException, AbsentInformationException {
    Location location = event.location();
    if (!isLearner(location.declaringType())) {
      return null;
    }
    drainOutput();
    ThreadReference thread = event.thread();
    Position here = position(location);
    StackFrame frame = thread.frame(0);
    FrameShadow shadow = frames.peek();
    if (framesMayBeStale || shadow == null || !shadow.method.equals(location.method())) {
      // Asking for the depth costs a round trip: only done when the shadows cannot be trusted.
      shadow = shadowOf(frame, thread.frameCount(), location.method(), here);
      framesMayBeStale = false;
    }
    recordLocalChanges(frame, shadow);
    shadow.position = here;
    current = here;
    return null;
  }

  private Finish onMethodExit(MethodExitEvent event)
      throws com.sun.jdi.IncompatibleThreadStateException, AbsentInformationException {
    Method method = event.method();
    if (!isLearner(method.declaringType())) {
      return null;
    }
    drainOutput();
    ThreadReference thread = event.thread();
    int depth = thread.frameCount();
    Position here = position(event.location());
    FrameShadow shadow = shadowOf(thread.frame(0), depth, method, here);
    recordLocalChanges(thread.frame(0), shadow);
    if (isReported(method)) {
      boolean returnsVoid = method.returnTypeName().equals("void");
      Value value = returnsVoid ? null : toValue(event.returnValue());
      String name = name(method);
      record(index -> new Step.Return(index, here.file(), here.line(), name, value));
    }
    frames.pop();
    current = here;
    return depth == mainDepth ? new Finish(Status.COMPLETED, null) : null;
  }

  private Finish onFieldWrite(ModificationWatchpointEvent event) {
    drainOutput();
    Position here = position(event.location());
    ObjectReference object = event.object();
    String target = object == null ? null : idOf(object);
    String ownerClass = object == null ? event.field().declaringType().name() : null;
    String field = event.field().name();
    Value value = toValue(event.valueToBe());
    record(
        index ->
            new Step.FieldSet(index, here.file(), here.line(), target, ownerClass, field, value));
    return null;
  }

  private Finish onException(ExceptionEvent event)
      throws com.sun.jdi.IncompatibleThreadStateException {
    framesMayBeStale = true;
    Location handler = event.catchLocation();
    boolean uncaught = handler == null;
    if (!uncaught && !isLearner(handler.declaringType())) {
      // Thrown and caught inside the JDK: not the learner's business.
      return null;
    }
    drainOutput();
    ExceptionInfo info = describe(event);
    record(index -> new Step.ExceptionThrown(index, info.file(), info.line(), info, !uncaught));
    if (!uncaught) {
      return null;
    }
    fatalException = info;
    return new Finish(Status.RUNTIME_ERROR, null);
  }

  // --- Exceptions

  private ExceptionInfo describe(ExceptionEvent event)
      throws com.sun.jdi.IncompatibleThreadStateException {
    ObjectReference exception = event.exception();
    ThreadReference thread = event.thread();
    String type = exception.referenceType().name();
    String message = message(exception, thread, type);
    Position where = firstLearnerFrame(thread);
    if (where == null) {
      where = current != null ? current : new Position("Main.java", 1);
    }
    return new ExceptionInfo(type, message, where.file(), where.line());
  }

  private String message(ObjectReference exception, ThreadReference thread, String type) {
    if (type.equals("java.lang.NullPointerException")) {
      // The JVM computes the helpful message ("because "fr" is null") only when asked for it.
      try {
        Method getMessage =
            exception
                .referenceType()
                .methodsByName("getMessage", "()Ljava/lang/String;")
                .getFirst();
        com.sun.jdi.Value computed =
            exception.invokeMethod(
                thread, getMessage, List.of(), ObjectReference.INVOKE_SINGLE_THREADED);
        if (computed instanceof StringReference text) {
          return ValueMapper.cut(text.value());
        }
      } catch (VMDisconnectedException e) {
        throw e;
      } catch (Exception e) {
        // Fall back to the stored message.
      }
    }
    Field stored = exception.referenceType().fieldByName("detailMessage");
    if (stored != null && exception.getValue(stored) instanceof StringReference text) {
      return ValueMapper.cut(text.value());
    }
    return null;
  }

  private Position firstLearnerFrame(ThreadReference thread)
      throws com.sun.jdi.IncompatibleThreadStateException {
    int searched = Math.min(thread.frameCount(), MAX_FRAMES_SEARCHED);
    for (StackFrame frame : thread.frames(0, searched)) {
      if (isLearner(frame.location().declaringType())) {
        return position(frame.location());
      }
    }
    return null;
  }

  // --- Local variables

  /** What is known about a frame of learner code: where it is and the last value of its locals. */
  private static final class FrameShadow {
    final int depth;
    final Method method;
    Position position;
    Map<String, Object> localKeys;

    FrameShadow(int depth, Method method, Position position, Map<String, Object> localKeys) {
      this.depth = depth;
      this.method = method;
      this.position = position;
      this.localKeys = localKeys;
    }
  }

  /**
   * The shadow of the top frame. Frames popped by an exception leave no exit event, so the stack of
   * shadows is brought in line with the real depth first.
   */
  private FrameShadow shadowOf(StackFrame frame, int depth, Method method, Position position)
      throws AbsentInformationException {
    while (!frames.isEmpty() && frames.peek().depth > depth) {
      frames.pop();
    }
    FrameShadow top = frames.peek();
    if (top != null && top.depth == depth && top.method.equals(method)) {
      return top;
    }
    if (top != null && top.depth == depth) {
      frames.pop();
    }
    FrameShadow shadow = new FrameShadow(depth, method, position, changeKeys(readLocals(frame)));
    frames.push(shadow);
    return shadow;
  }

  /** Compares the locals with the last ones seen; the line that just ran made the changes. */
  private void recordLocalChanges(StackFrame frame, FrameShadow shadow)
      throws AbsentInformationException {
    Map<String, com.sun.jdi.Value> locals = readLocals(frame);
    Map<String, Object> keys = changeKeys(locals);
    Position at = shadow.position;
    String method = name(shadow.method);
    for (Map.Entry<String, com.sun.jdi.Value> local : locals.entrySet()) {
      String name = local.getKey();
      Object key = keys.get(name);
      if (!shadow.localKeys.containsKey(name) || !shadow.localKeys.get(name).equals(key)) {
        Value value = toValue(local.getValue());
        record(index -> new Step.LocalSet(index, at.file(), at.line(), method, name, value));
      }
    }
    shadow.localKeys = keys;
  }

  private static Map<String, com.sun.jdi.Value> readLocals(StackFrame frame)
      throws AbsentInformationException {
    List<LocalVariable> variables = frame.visibleVariables();
    Map<LocalVariable, com.sun.jdi.Value> values = frame.getValues(variables);
    Map<String, com.sun.jdi.Value> locals = new LinkedHashMap<>();
    for (LocalVariable variable : variables) {
      locals.put(variable.name(), values.get(variable));
    }
    return locals;
  }

  private Map<String, Object> changeKeys(Map<String, com.sun.jdi.Value> locals) {
    Map<String, Object> keys = new LinkedHashMap<>();
    locals.forEach((name, value) -> keys.put(name, mapper.changeKey(value)));
    return keys;
  }

  // --- Output

  /**
   * Takes what the program printed since the last event. The JVM is suspended at every event, so
   * the text belongs to the line that was running.
   */
  private void drainOutput() {
    byte[] chunk;
    try {
      InputStream stream = child.stdout();
      int available = stream.available();
      if (available <= 0) {
        return;
      }
      chunk = stream.readNBytes(available);
    } catch (IOException e) {
      return;
    }
    int room = limits.maxOutputBytes() - outputBytes;
    boolean overLimit = chunk.length > room;
    if (overLimit) {
      chunk = Arrays.copyOf(chunk, room);
    }
    outputBytes += chunk.length;
    String text = decode(chunk);
    if (!text.isEmpty()) {
      stdout.append(text);
      if (current != null) {
        Position at = current;
        record(index -> new Step.Output(index, at.file(), at.line(), text));
      }
    }
    if (overLimit) {
      throw new LimitReached(ExceededLimit.OUTPUT);
    }
  }

  /** Decodes UTF-8 across chunks: a character split between two reads waits for its second half. */
  private String decode(byte[] chunk) {
    ByteBuffer bytes = ByteBuffer.allocate(undecoded.length + chunk.length);
    bytes.put(undecoded).put(chunk).flip();
    CharBuffer chars = CharBuffer.allocate(bytes.remaining() + 1);
    decoder.decode(bytes, chars, false);
    undecoded = new byte[bytes.remaining()];
    bytes.get(undecoded);
    return chars.flip().toString();
  }

  // --- Recording

  private void record(IntFunction<Step> step) {
    steps.add(step.apply(steps.size()));
    if (steps.size() >= limits.maxSteps()) {
      throw new LimitReached(ExceededLimit.STEPS);
    }
  }

  private Value toValue(com.sun.jdi.Value value) {
    Value mapped = mapper.toValue(value);
    checkObjectLimit();
    return mapped;
  }

  private String idOf(ObjectReference object) {
    String id = registry.idOf(object);
    checkObjectLimit();
    return id;
  }

  private void checkObjectLimit() {
    if (registry.size() > limits.maxObjects()) {
      throw new LimitReached(ExceededLimit.OBJECTS);
    }
  }

  private boolean isLearner(ReferenceType type) {
    return learnerClasses.contains(type.name());
  }

  /** Static initializers and the methods javac makes up (lambdas, bridges) are not reported. */
  private static boolean isReported(Method method) {
    return !method.isSynthetic() && !method.isStaticInitializer();
  }

  private static String name(Method method) {
    return method.declaringType().name() + "." + method.name();
  }

  private record Position(String file, int line) {}

  private static Position position(Location location) {
    String file;
    try {
      file = location.sourceName();
    } catch (AbsentInformationException e) {
      file = location.declaringType().name() + ".java";
    }
    return new Position(file, Math.max(1, location.lineNumber()));
  }
}
