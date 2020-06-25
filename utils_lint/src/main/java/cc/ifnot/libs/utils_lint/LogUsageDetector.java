package cc.ifnot.libs.utils_lint;

import com.android.annotations.NonNull;
import com.android.tools.lint.checks.StringFormatDetector;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import static com.android.SdkConstants.GET_STRING_METHOD;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_BOOLEAN;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_BYTE;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_CHAR;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_DOUBLE;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_FLOAT;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_INT;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_LONG;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_NULL;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_OBJECT;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_SHORT;
import static com.android.tools.lint.client.api.JavaEvaluatorKt.TYPE_STRING;
import static com.android.tools.lint.detector.api.ConstantEvaluator.evaluateString;
import static com.android.tools.lint.detector.api.Lint.isString;

public class LogUsageDetector extends Detector implements Detector.UastScanner {

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("v", "d", "i", "w", "e", "o", "tag");
    }

    private static boolean isSubclassOf(JavaContext context, UExpression expression, Class<?> cls) {
        PsiType expressionType = expression.getExpressionType();
        if (expressionType instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) expressionType;
            PsiClass resolvedClass = classType.resolve();
            return context.getEvaluator().extendsClass(resolvedClass, cls.getName(), false);
        }
        return false;
    }

    private static Class<?> getType(UExpression expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression call = (PsiMethodCallExpression) expression;
            PsiMethod method = call.resolveMethod();
            if (method == null) {
                return null;
            }
            String methodName = method.getName();
            if (methodName.equals(GET_STRING_METHOD)) {
                return String.class;
            }
        } else if (expression instanceof PsiLiteralExpression) {
            PsiLiteralExpression literalExpression = (PsiLiteralExpression) expression;
            PsiType expressionType = literalExpression.getType();
            if (isString(expressionType)) {
                return String.class;
            } else if (expressionType == PsiType.INT) {
                return Integer.TYPE;
            } else if (expressionType == PsiType.FLOAT) {
                return Float.TYPE;
            } else if (expressionType == PsiType.CHAR) {
                return Character.TYPE;
            } else if (expressionType == PsiType.BOOLEAN) {
                return Boolean.TYPE;
            } else if (expressionType == PsiType.NULL) {
                return Object.class;
            }
        }

        PsiType type = expression.getExpressionType();
        if (type != null) {
            Class<?> typeClass = getTypeClass(type);
            return typeClass != null ? typeClass : Object.class;
        }

        return null;
    }

    private static Class<?> getTypeClass(@Nullable PsiType type) {
        if (type != null) {
            return getTypeClass(type.getCanonicalText());
        }
        return null;
    }

    private static Class<?> getTypeClass(@Nullable String typeClassName) {
        if (typeClassName == null) {
            return null;
        } else if (typeClassName.equals(TYPE_STRING) || "String".equals(typeClassName)) {
            return String.class;
        } else if (typeClassName.equals(TYPE_INT)) {
            return Integer.TYPE;
        } else if (typeClassName.equals(TYPE_BOOLEAN)) {
            return Boolean.TYPE;
        } else if (typeClassName.equals(TYPE_NULL)) {
            return Object.class;
        } else if (typeClassName.equals(TYPE_LONG)) {
            return Long.TYPE;
        } else if (typeClassName.equals(TYPE_FLOAT)) {
            return Float.TYPE;
        } else if (typeClassName.equals(TYPE_DOUBLE)) {
            return Double.TYPE;
        } else if (typeClassName.equals(TYPE_CHAR)) {
            return Character.TYPE;
        } else if ("BigDecimal".equals(typeClassName) || "java.math.BigDecimal".equals(typeClassName)) {
            return Float.TYPE;
        } else if ("BigInteger".equals(typeClassName) || "java.math.BigInteger".equals(typeClassName)) {
            return Integer.TYPE;
        } else if (typeClassName.equals(TYPE_OBJECT)) {
            return null;
        } else if (typeClassName.startsWith("java.lang.")) {
            if ("java.lang.Integer".equals(typeClassName)
                    || "java.lang.Short".equals(typeClassName)
                    || "java.lang.Byte".equals(typeClassName)
                    || "java.lang.Long".equals(typeClassName)) {
                return Integer.TYPE;
            } else if ("java.lang.Float".equals(typeClassName) || "java.lang.Double".equals(
                    typeClassName)) {
                return Float.TYPE;
            } else if ("java.lang.Boolean".equals(typeClassName)) {
                return Boolean.TYPE;
            } else {
                return null;
            }
        } else if (typeClassName.equals(TYPE_BYTE)) {
            return Byte.TYPE;
        } else if (typeClassName.equals(TYPE_SHORT)) {
            return Short.TYPE;
        } else if ("Date".equals(typeClassName) || "java.util.Date".equals(typeClassName)) {
            return Date.class;
        } else if ("Calendar".equals(typeClassName) || "java.util.Calendar".equals(typeClassName)) {
            return Calendar.class;
        } else {
            return null;
        }
    }

    private static List<String> getStringArgumentTypes(String formatString) {
        List<String> types = new ArrayList<>();
        Matcher matcher = StringFormatDetector.FORMAT.matcher(formatString);
        int index = 0;
        int prevIndex = 0;
        while (true) {
            if (matcher.find(index)) {
                int matchStart = matcher.start();
                while (prevIndex < matchStart) {
                    char c = formatString.charAt(prevIndex);
                    if (c == '\\') {
                        prevIndex++;
                    }
                    prevIndex++;
                }
                if (prevIndex > matchStart) {
                    index = prevIndex;
                    continue;
                }

                index = matcher.end();
                String str = formatString.substring(matchStart, matcher.end());
                if ("%%".equals(str) || "%n".equals(str)) {
                    continue;
                }
                String time = matcher.group(5);
                if ("t".equalsIgnoreCase(time)) {
                    types.add(time + matcher.group(6));
                } else {
                    types.add(matcher.group(6));
                }
            } else {
                break;
            }
        }
        return types;
    }

    private LintFix quickFixIssueLog(UCallExpression logCall) {
        List<UExpression> arguments = logCall.getValueArguments();
        String methodName = logCall.getMethodName();
        UExpression tag = arguments.get(0);

        // 1st suggestion respects author's tag preference.
        // 2nd suggestion drops it (Lg defaults to calling class name).
        String fixSource1 = "Lg.tag(" + tag.asSourceString() + ");\nLg.";
        String fixSource2 = "Lg.";

        int numArguments = arguments.size();
        if (numArguments == 2) {
            UExpression msgOrThrowable = arguments.get(1);
            fixSource1 += methodName + "(" + msgOrThrowable.asSourceString() + ")";
            fixSource2 += methodName + "(" + msgOrThrowable.asSourceString() + ")";
        } else if (numArguments == 3) {
            UExpression msg = arguments.get(1);
            UExpression throwable = arguments.get(2);
            fixSource1 +=
                    methodName + "(" + throwable.asSourceString() + ", " + msg.asSourceString() + ")";
            fixSource2 +=
                    methodName + "(" + throwable.asSourceString() + ", " + msg.asSourceString() + ")";
        } else {
            throw new IllegalStateException("android.util.Log overloads should have 2 or 3 arguments");
        }

        String logCallSource = logCall.asSourceString();
        System.out.println("lint: " + logCall.asSourceString() + "-- " + logCall.asLogString());
        LintFix.GroupBuilder fixGrouper = fix().group();
        fixGrouper.add(
                fix().replace().text(logCallSource).shortenNames().reformat(true).with(fixSource1).build());
        fixGrouper.add(
                fix().replace().text(logCallSource).shortenNames().reformat(true).with(fixSource2).build());
        return fixGrouper.build();
    }


    @Override
    public void visitMethodCall(@NotNull JavaContext context, @NotNull UCallExpression call, @NotNull PsiMethod method) {
        String methodName = call.getMethodName();
        JavaEvaluator evaluator = context.getEvaluator();

        if (evaluator.isMemberInClass(method, "android.util.Log")) {
            LintFix fix = quickFixIssueLog(call);
            context.report(ISSUE_LOG, call, context.getLocation(call), "Using 'Log' instead of 'Lg'",
                    fix);
            return;
        }

        if (!evaluator.isMemberInClass(method, "cc.ifnot.libs.utils.Lg")) {
            return;
        }

        List<UExpression> arguments = call.getValueArguments();
        int numArguments = arguments.size();
        if (numArguments == 0) {
            return;
        }

        int startIndexOfArguments = 1;

        for (UExpression u : arguments) {
            if (isSubclassOf(context, u, Throwable.class)) {
                if (numArguments == 1) {
                    return;
                }
                startIndexOfArguments++;
            }
        }

        String formatString = evaluateString(context, arguments.get(0), true);
        // We passed for example a method call
        if (formatString == null) {
            return;
        }

        int formatArgumentCount = getFormatArgumentCount(formatString);
        int passedArgCount = numArguments - startIndexOfArguments;
        if (formatArgumentCount < passedArgCount) {
            context.report(ISSUE_ARG_COUNT, call, context.getLocation(call), String.format(
                    "Wrong argument count, format string `%1$s` requires "
                            + "`%2$d` but format call supplies `%3$d`", formatString, formatArgumentCount,
                    passedArgCount));
            return;
        }

        if (formatArgumentCount == 0) {
            return;
        }

        List<String> types = getStringArgumentTypes(formatString);
        UExpression argument = null;
        int argumentIndex = startIndexOfArguments;
        boolean valid;
        for (int i = 0; i < types.size(); i++) {
            String formatType = types.get(i);
            if (argumentIndex != numArguments) {
                argument = arguments.get(argumentIndex++);
            } else {
                context.report(ISSUE_ARG_COUNT, call, context.getLocation(call), String.format(
                        "Wrong argument count, format string `%1$s` requires "
                                + "`%2$d` but format call supplies `%3$d`", formatString, formatArgumentCount,
                        passedArgCount));
            }

            Class type = getType(argument);
            if (type == null) {
                continue;
            }

            char last = formatType.charAt(formatType.length() - 1);
            if (formatType.length() >= 2
                    && Character.toLowerCase(formatType.charAt(formatType.length() - 2)) == 't') {
                // Date time conversion.
                switch (last) {
                    // time
                    case 'H':
                    case 'I':
                    case 'k':
                    case 'l':
                    case 'M':
                    case 'S':
                    case 'L':
                    case 'N':
                    case 'p':
                    case 'z':
                    case 'Z':
                    case 's':
                    case 'Q':
                        // date
                    case 'B':
                    case 'b':
                    case 'h':
                    case 'A':
                    case 'a':
                    case 'C':
                    case 'Y':
                    case 'y':
                    case 'j':
                    case 'm':
                    case 'd':
                    case 'e':
                        // date/time
                    case 'R':
                    case 'T':
                    case 'r':
                    case 'D':
                    case 'F':
                    case 'c':
                        valid = type == Integer.TYPE || type == Calendar.class || type == Date.class
                                || type == Long.TYPE;
                        if (!valid) {
                            String message = String.format(
                                    "Wrong argument type for date formatting argument '#%1$d' "
                                            + "in `%2$s`: conversion is '`%3$s`', received `%4$s` "
                                            + "(argument #%5$d in method call)", i + 1, formatString, formatType,
                                    type.getSimpleName(), startIndexOfArguments + i + 1);
                            context.report(ISSUE_ARG_TYPES, call, context.getLocation(argument), message);
                        }
                        break;
                    default:
                        String message = String.format("Wrong suffix for date format '#%1$d' "
                                        + "in `%2$s`: conversion is '`%3$s`', received `%4$s` "
                                        + "(argument #%5$d in method call)", i + 1, formatString, formatType,
                                type.getSimpleName(), startIndexOfArguments + i + 1);
                        context.report(ISSUE_FORMAT, call, context.getLocation(argument), message);
                }
                continue;
            }
            switch (last) {
                case 'b':
                case 'B':
                    valid = type == Boolean.TYPE;
                    break;
                case 'x':
                case 'X':
                case 'd':
                case 'o':
                case 'e':
                case 'E':
                case 'f':
                case 'g':
                case 'G':
                case 'a':
                case 'A':
                    valid = type == Integer.TYPE
                            || type == Float.TYPE
                            || type == Double.TYPE
                            || type == Long.TYPE
                            || type == Byte.TYPE
                            || type == Short.TYPE;
                    break;
                case 'c':
                case 'C':
                    valid = type == Character.TYPE;
                    break;
                case 'h':
                case 'H':
                    valid = type != Boolean.TYPE && !Number.class.isAssignableFrom(type);
                    break;
                case 's':
                case 'S':
                default:
                    valid = true;
            }
            if (!valid) {
                String message = String.format("Wrong argument type for formatting argument '#%1$d' "
                                + "in `%2$s`: conversion is '`%3$s`', received `%4$s` "
                                + "(argument #%5$d in method call)", i + 1, formatString, formatType,
                        type.getSimpleName(), startIndexOfArguments + i + 1);
                context.report(ISSUE_ARG_TYPES, call, context.getLocation(argument), message);
            }
        }
    }

    private static int getFormatArgumentCount(@NonNull String s) {
        Matcher matcher = StringFormatDetector.FORMAT.matcher(s);
        int index = 0;
        int prevIndex = 0;
        int nextNumber = 1;
        int max = 0;
        while (true) {
            if (matcher.find(index)) {
                String value = matcher.group(6);
                if ("%".equals(value) || "n".equals(value)) {
                    index = matcher.end();
                    continue;
                }
                int matchStart = matcher.start();
                for (; prevIndex < matchStart; prevIndex++) {
                    char c = s.charAt(prevIndex);
                    if (c == '\\') {
                        prevIndex++;
                    }
                }
                if (prevIndex > matchStart) {
                    index = prevIndex;
                    continue;
                }

                int number;
                String numberString = matcher.group(1);
                if (numberString != null) {
                    // Strip off trailing $
                    numberString = numberString.substring(0, numberString.length() - 1);
                    number = Integer.parseInt(numberString);
                    nextNumber = number + 1;
                } else {
                    number = nextNumber++;
                }
                if (number > max) {
                    max = number;
                }
                index = matcher.end();
            } else {
                break;
            }
        }

        return max;
    }

    static Issue[] getIssues() {
        return new Issue[]{
                ISSUE_LOG, ISSUE_FORMAT, ISSUE_ARG_COUNT, ISSUE_ARG_TYPES,
        };
    }

    public static final Issue ISSUE_LOG =
            Issue.create("LogNotLg", "Logging call to Log instead of Lg",
                    "Since Lg is included in the project, it is likely that calls to Log should instead"
                            + " be going to Lg.", Category.MESSAGES, 5, Severity.WARNING,
                    new Implementation(LogUsageDetector.class, Scope.JAVA_FILE_SCOPE));

    public static final Issue ISSUE_ARG_TYPES =
            Issue.create("LgArgTypes", "Formatting string doesn't match passed arguments",
                    "The argument types that you specified in your formatting string does not match the types"
                            + " of the arguments that you passed to your formatting call.", Category.MESSAGES, 9,
                    Severity.ERROR,
                    new Implementation(LogUsageDetector.class, Scope.JAVA_FILE_SCOPE));

    public static final Issue ISSUE_ARG_COUNT =
            Issue.create("LgArgCount", "Formatting argument types incomplete or inconsistent",
                    "When a formatted string takes arguments, you need to pass at least that amount of"
                            + " arguments to the formatting call.", Category.MESSAGES, 9, Severity.ERROR,
                    new Implementation(LogUsageDetector.class, Scope.JAVA_FILE_SCOPE));

    public static final Issue ISSUE_FORMAT =
            Issue.create("StringFormatInLg", "Logging call with Lg contains String#format()",
                    "Since Lg handles String.format automatically, you may not use String#format().",
                    Category.MESSAGES, 5, Severity.WARNING,
                    new Implementation(LogUsageDetector.class, Scope.JAVA_FILE_SCOPE));

}
