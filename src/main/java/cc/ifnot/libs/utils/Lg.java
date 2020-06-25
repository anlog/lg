package cc.ifnot.libs.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class Lg {
    public static final int MORE = 0;

    public static final int LESS = 1;

    public static final int VERBOSE = 2;

    public static final int DEBUG = 3;

    public static final int INFO = 4;

    public static final int WARN = 5;

    public static final int ERROR = 6;

    public static final int SILENT = Integer.MAX_VALUE;

    private static final int BASE = 29;
    private static final String EMPTY_LINE = "";
    private static String TAG = "Lg";
    private static boolean init = false;
    private static boolean isAndroid;
    private static boolean more = false;
    private static int level = VERBOSE;
    private static Method v;
    private static Method vt;
    private static Method d;
    private static Method dt;
    private static Method i;
    private static Method it;
    private static Method w;
    private static Method wt;
    private static Method e;
    private static Method et;

    static {
        try {
            Class.forName("android.view.View");
            isAndroid = true;

            Class<?> clz = Class.forName("android.util.Log");
            v = clz.getDeclaredMethod("v", String.class, String.class);
            vt = clz.getDeclaredMethod("v", String.class, String.class, Throwable.class);
            d = clz.getDeclaredMethod("d", String.class, String.class);
            dt = clz.getDeclaredMethod("d", String.class, String.class, Throwable.class);
            i = clz.getDeclaredMethod("i", String.class, String.class);
            it = clz.getDeclaredMethod("i", String.class, String.class, Throwable.class);
            w = clz.getDeclaredMethod("w", String.class, String.class);
            wt = clz.getDeclaredMethod("w", String.class, String.class, Throwable.class);
            e = clz.getDeclaredMethod("e", String.class, String.class);
            et = clz.getDeclaredMethod("e", String.class, String.class, Throwable.class);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            if (more) f(ignored, ERROR + BASE);
            isAndroid = false;
        }
    }

    private static void print(Object msg, int level) {
        if (msg instanceof Throwable) {
            if (isAndroid) {
                try {
                    switch (level) {
                        case VERBOSE:
                            vt.invoke(null, TAG, "", msg);
                            break;
                        case DEBUG:
                            dt.invoke(null, TAG, "", msg);
                            break;
                        case INFO:
                            it.invoke(null, TAG, "", msg);
                            break;
                        default:
                        case WARN:
                            wt.invoke(null, TAG, "", msg);
                            break;
                        case ERROR:
                            et.invoke(null, TAG, "", msg);
                            break;
                    }
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    if (more) f(ignored, WARN + BASE);
                    f(msg, WARN + BASE);
                }
            } else {
                f(msg, level + BASE);
            }
        } else {
            if (isAndroid) {
                try {
                    switch (level) {
                        case VERBOSE:
                            v.invoke(null, TAG, msg);
                            break;
                        case DEBUG:
                            d.invoke(null, TAG, msg);
                            break;
                        case INFO:
                            i.invoke(null, TAG, msg);
                            break;
                        default:
                        case WARN:
                            w.invoke(null, TAG, msg);
                            break;
                        case ERROR:
                            e.invoke(null, TAG, msg);
                            break;
                    }
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    if (more) f(ignored, WARN + BASE);
                    f(msg, level);
                }
            } else {
                f(msg, level);
            }
        }
    }


    private static void o(Object msg, int level) {
        if (Lg.level <= level) {
            if (msg instanceof Throwable) {
                print(msg, level);
            } else {
                if (Lg.level > LESS) {
                    print(msg, level);
                } else {
                    Thread thread = Thread.currentThread();
                    StackTraceElement[] stacks = thread.getStackTrace();
                    StackTraceElement stack = stacks[1];
                    for (StackTraceElement s : stacks) {
                        if (!Thread.class.getName().equals(s.getClassName())
                                && !"dalvik.system.VMStack".equals(s.getClassName())
                                && !Lg.class.getName().equals(s.getClassName())) {
                            stack = s;
                            break;
                        }
                    }

                    msg = Lg.level == MORE ?
                            String.format(Locale.getDefault(), "%s[%s:%d@%s]: %s",
                                    stack.getFileName() == null ? "Anonymous" :
                                            stack.getFileName().replace(".java", "")
                                                    .replace(".kt", ""),
                                    stack.getMethodName(), stack.getLineNumber(), thread.getName(), msg) :
                            String.format(Locale.getDefault(), "%s: %s",
                                    stack.getFileName() == null ? "Anonymous" :
                                            stack.getFileName().replace(".java", "")
                                                    .replace(".kt", ""), msg);
                    print(msg, level);
                }
            }
        }
    }

    public static void f(Object msg, int level) {
        if (msg instanceof Throwable) {
            // todo: impl stack print
            ((Throwable) msg).printStackTrace();
        } else {
            level = level + BASE;
            String console_prefix = level > 0 ?
                    String.format(Locale.getDefault(), "\033[%d;3m", level) : "";
            String console_suffix = level > 0 ?
                    "\033[0m" : "";
            System.out.println(String.format(Locale.getDefault(),
                    "%s%s%s", console_prefix, msg, console_suffix));
        }
    }

    public synchronized static void tag(String tag) {
        if (init) {
            o("tag is already inited", WARN);
            return;
        }
        TAG = tag;
        init = true;
    }

    public static void level(int level) {
        Lg.level = level;
    }

    private static void wrap(int level, String format, Object... msg) {
        List<Object> logs = new ArrayList<>();
        Collections.addAll(logs, msg);
        List<Throwable> throwables = new ArrayList<>();
        for (Object o : msg) {
            if (o instanceof Throwable) {
                throwables.add((Throwable) o);
                logs.remove(o);
            }
        }
        if (logs.size() > 0) {
            o(new Formatter().format(format, logs.toArray()).toString(), level);
            for (Throwable t : throwables) {
                o(t, level);
            }
        } else if (Lg.level <= level) {
            o(new Formatter().format(format, msg).toString(), level);
        }
    }

    public static void i(String format, Object... msg) {
        wrap(INFO, format, msg);
    }

    public static void i(Object msg) {
        o(msg, INFO);
    }

    public static void i() {
        o(EMPTY_LINE, INFO);
    }

    public static void v(String format, Object... msg) {
        wrap(VERBOSE, format, msg);
    }

    public static void v(Object msg) {
        o(msg, VERBOSE);
    }

    public static void v() {
        o(EMPTY_LINE, VERBOSE);
    }

    public static void d(String format, Object... msg) {
        wrap(DEBUG, format, msg);
    }

    public static void d(Object msg) {
        o(msg, DEBUG);
    }

    public static void d() {
        o(EMPTY_LINE, DEBUG);
    }

    public static void w(String format, Object... msg) {
        wrap(WARN, format, msg);
    }

    public static void w(Object msg) {
        o(msg, WARN);
    }

    public static void w() {
        o(EMPTY_LINE, WARN);
    }

    public static void e(String format, Object... msg) {
        wrap(ERROR, format, msg);
    }

    public static void e(Object msg) {
        o(msg, ERROR);
    }

    public static void e() {
        o(EMPTY_LINE, ERROR);
    }
}
