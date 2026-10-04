package dev.l4z.viaproxyweb.exception;

public class NoAccountSelectedException extends RuntimeException {
    public NoAccountSelectedException() {
        super("No account selected");
    }
}