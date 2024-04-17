package lost.parser.combinator;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public interface Parser<T> {

    ParseResult<T> parse(ByteBuffer input);

    /**
     * opt parser
     * <p>
     * like regex `?`
     */
    default Parser<Optional<T>> opt() {
        return Parsers.opt(Parser.this);
    }

    /**
     * repeat parser by times
     * <p>
     * like regex `{min.max}`
     *
     * @param min min times
     * @param max max times
     */
    default Parser<List<T>> repeat(Parser<T> parser, int min, int max) {
        return Parsers.repeat(Parser.this, min, max);
    }

    /**
     * repeat parser by times
     * <p>
     * like regex `{min,}`
     *
     * @param min min times
     */
    default Parser<List<T>> repeat(int min) {
        return Parsers.repeat(Parser.this, min);
    }

    /**
     * repeat parser by fixed times
     * <p>
     * like regex `{times}`
     *
     * @param times fixed times
     */
    default Parser<List<T>> repeat_fixed(int times) {
        return Parsers.repeat_fixed(Parser.this, times);
    }

    /**
     * one or many parser
     * <p>
     * like regex `+`
     */
    default Parser<List<T>> oneOrMany() {
        return Parsers.oneOrMany(Parser.this);
    }

    /**
     * zero or many parser
     * <p>
     * like regex `*`
     */
    default Parser<List<T>> zeroOrMany() {
        return Parsers.zeroOrMany(Parser.this);
    }

    /**
     * or parser
     * <p>
     * like regex `|`
     */
    default Parser<T> or(Parser<T> other) {
        //noinspection unchecked
        return Parsers.or(Parser.this, other);
    }

    /**
     * and parser
     */
    default <B, C> Parser<C> and(Parser<B> other, BiFunction<T, B, C> merge) {
        return Parsers.and(Parser.this, other, merge);
    }
}
