package chiralsoftware.surgixml;

import picocli.CommandLine;

import static java.lang.System.exit;

public final class Main {

    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new Surgixml()).execute(args);
        exit(exitCode);
    }

}