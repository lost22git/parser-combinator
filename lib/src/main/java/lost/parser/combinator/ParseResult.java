package lost.parser.combinator;

import java.nio.ByteBuffer;

public record ParseResult<T>(T value, ByteBuffer rest) {}
