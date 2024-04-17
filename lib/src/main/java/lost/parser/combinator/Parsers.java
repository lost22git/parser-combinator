package lost.parser.combinator;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class Parsers {
    private Parsers() {
        throw new Error("Can not instantiate me !!!");
    }

    /**
     * end parser
     */
    public static Parser<Void> end() {
        return Parsers::parser_end;
    }

    private static ParseResult<Void> parser_end(ByteBuffer input) {
        if (input.hasRemaining()) throw new ParseError("parse end error");
        return new ParseResult<>(null, input);
    }

    /**
     * skip whitespace parser
     */
    public static Parser<Void> skipWhitespaces() {
        return Parsers::parse_skip_ws;
    }

    private static ParseResult<Void> parse_skip_ws(ByteBuffer input) {

        while (true) {
            if (!input.hasRemaining()) break;
            try {
                if (!Character.isWhitespace(input.getChar())) {
                    input.position(input.position() - 2);
                    break;
                }
            } catch (BufferUnderflowException ignore) {
                break;
            }
        }
        return new ParseResult<>(null, input);
    }

    /**
     * until byte parser
     *
     * @param tester  test fn
     * @param include include until
     * @return
     */
    public static Parser<ByteBuffer> untilByte(Predicate<Byte> tester, boolean include) {
        return input -> Parsers.parse_until_byte(input, tester, include);
    }

    private static ParseResult<ByteBuffer> parse_until_byte(ByteBuffer input, Predicate<Byte> tester, boolean include) {
        int begin = input.position();

        while (true) {
            if (!input.hasRemaining()) break;
            if (tester.test(input.get())) {
                if (!include) input.position(input.position() - 1);
                break;
            }
        }

        var value = input.slice(begin, input.position() - begin);
        return new ParseResult<>(value, input);
    }

    /**
     * until char parser
     *
     * @param tester  test fn
     * @param include include until
     * @return
     */
    public static Parser<ByteBuffer> untilChar(Predicate<Character> tester, boolean include) {
        return input -> Parsers.parse_until_char(input, tester, include);
    }

    private static ParseResult<ByteBuffer> parse_until_char(
            ByteBuffer input, Predicate<Character> tester, boolean include) {
        int begin = input.position();

        while (true) {
            if (!input.hasRemaining()) break;
            try {
                if (tester.test(input.getChar())) {
                    if (!include) input.position(input.position() - 2);
                    break;
                }
            } catch (BufferUnderflowException e) {
                input.position(begin);
                throw new ParseError("parse until char error", e);
            }
        }

        var value = input.slice(begin, input.position() - begin);
        return new ParseResult<>(value, input);
    }

    /**
     * a byte parser
     */
    public static Parser<Byte> abyte(byte expect) {
        return input -> parse_a_byte(input, expect);
    }

    private static ParseResult<Byte> parse_a_byte(ByteBuffer input, byte expect) {
        int begin = input.position();

        if (!input.hasRemaining()) throw new ParseError("parse a byte error: end");
        var actual = input.get();
        if (expect != actual) {
            input.position(begin);
            throw new ParseError("parse a byte error: expect " + expect + " but got " + actual);
        }
        return new ParseResult<>(expect, input);
    }

    /**
     * a char parser
     */
    public static Parser<Character> achar(char expect) {
        return input -> parse_a_char(input, expect);
    }

    private static ParseResult<Character> parse_a_char(ByteBuffer input, char expect) {
        int begin = input.position();

        if (!input.hasRemaining()) throw new ParseError("parse a char error: end");
        char actual;
        try {
            actual = input.getChar();
        } catch (BufferUnderflowException e) {
            throw new ParseError("parse a char error", e);
        }
        if (expect != actual) {
            input.position(begin);
            throw new ParseError("parse a char error: expect " + expect + " but got " + actual);
        }
        return new ParseResult<>(expect, input);
    }

    /**
     * opt parser
     * <p>
     * like regex `?`
     */
    public static <T> Parser<Optional<T>> opt(Parser<T> parser) {
        return input -> parse_opt(parser, input);
    }

    private static <T> ParseResult<Optional<T>> parse_opt(Parser<T> parser, ByteBuffer input) {
        try {
            var r = parser.parse(input);
            return new ParseResult<>(Optional.of(r.value()), r.rest());
        } catch (ParseError ignore) {
            return new ParseResult<>(Optional.empty(), input);
        }
    }

    /**
     * repeat parser by times
     * <p>
     * like regex `{min.max}`
     *
     * @param min min times
     * @param max max times
     */
    public static <T> Parser<List<T>> repeat(Parser<T> parser, int min, int max) {
        if (!(min > 0)) throw new IllegalArgumentException("`times` requires > 0");
        if (!(max >= min)) throw new IllegalArgumentException("requires `max` >= `min`");

        return input -> parse_repeat(parser, min, max, input);
    }

    /**
     * repeat parser by times
     * <p>
     * like regex `{min,}`
     *
     * @param min min times
     */
    public static <T> Parser<List<T>> repeat(Parser<T> parser, int min) {
        return repeat(parser, min, Integer.MAX_VALUE);
    }

    /**
     * repeat parser by fixed times
     * <p>
     * like regex `{times}`
     *
     * @param times fixed times
     */
    public static <T> Parser<List<T>> repeat_fixed(Parser<T> parser, int times) {
        return repeat(parser, times, times);
    }

    private static <T> ParseResult<List<T>> parse_repeat(Parser<T> parser, int min, int max, ByteBuffer input) {
        int begin = input.position();

        var values = new ArrayList<T>(min);
        var rest = input;

        try {
            for (int i = 0; i < max; i++) {
                var result = parser.parse(rest);
                rest = result.rest();
                values.add(result.value());
            }
        } catch (ParseError e) {
            if (values.size() < min) {
                input.position(begin);
                throw new ParseError("parse repeat error", e);
            }
        }

        return new ParseResult<>(values, rest);
    }

    /**
     * one or many parser
     * <p>
     * like regex `+`
     */
    public static <T> Parser<List<T>> oneOrMany(Parser<T> parser) {
        return repeat(parser, 1);
    }

    /**
     * zero or many parser
     * <p>
     * like regex `*`
     */
    public static <T> Parser<List<T>> zeroOrMany(Parser<T> parser) {
        return input -> parse_zero_or_many(parser, input);
    }

    private static <T> ParseResult<List<T>> parse_zero_or_many(Parser<T> parser, ByteBuffer input) {
        int begin = input.position();
        try {
            var result = opt(parser).parse(input);
            if (result.value().isEmpty()) return new ParseResult<>(List.of(), result.rest());
            input.position(begin);
            return repeat(parser, 1).parse(input);
        } catch (ParseError e) {
            input.position(begin);
            throw new ParseError("parse zeroOrMany error", e);
        }
    }

    /**
     * or parser
     * <p>
     * like regex `|`
     */
    public static <T> Parser<T> or(Parser<T>... parsers) {
        return input -> parse_or(input, parsers);
    }

    private static <T> ParseResult<T> parse_or(ByteBuffer input, Parser<T>... parsers) {
        int begin = input.position();

        for (Parser<T> parser : parsers) {
            try {
                return parser.parse(input);
            } catch (ParseError ignore) {
                input.position(begin);
            }
        }

        input.position(begin);
        throw new ParseError("parse or error");
    }

    /**
     * and parser
     */
    public static <A, B, C> Parser<C> and(Parser<A> a, Parser<B> b, BiFunction<A, B, C> merge) {
        return input -> parse_and(a, b, merge, input);
    }

    private static <A, B, C> ParseResult<C> parse_and(
            Parser<A> a, Parser<B> b, BiFunction<A, B, C> merge, ByteBuffer input) {
        int begin = input.position();

        try {
            var a_result = a.parse(input);
            var b_result = b.parse(a_result.rest());
            var c_result = merge.apply(a_result.value(), b_result.value());
            return new ParseResult<>(c_result, b_result.rest());
        } catch (ParseError e) {
            input.position(begin);
            throw new ParseError("parse and error", e);
        }
    }
}
