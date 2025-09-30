package ch.turic.cli;

import ch.turic.Repl;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Console {

    final private LineReader reader;
    final private History history;
    final Terminal terminal;
    final List<String> loaded = new ArrayList<>();
    final Config configuration;

    public void load(final Path path) throws IOException {
        load(Files.readString(path, StandardCharsets.UTF_8));
    }

    public void load(final String lines) {
        loaded.addAll(Arrays.asList(lines.split("\n")));
    }

    public Console(Repl interpreter, Config configuration) throws IOException {
        this.configuration = configuration;
        terminal = TerminalBuilder.builder().system(true).build();
        Parser parser = new DefaultParser() {
            @Override
            public boolean isEscapeChar(char c) {
                return false;
            }
        };
        history = new DefaultHistory();
        reader = LineReaderBuilder.builder()
                .appName("Turicum")
                .terminal(terminal)
                .completer(new StringsCompleter(interpreter::completions))
                .parser(parser)
                .history(history)
                .build();
        try {
            load(Path.of(".repl.turi"));
            output(".repl.turi was loaded");
        } catch (Exception ignore) {
            error("no .repl.turi");
        }
    }


    public void output(final String output) {
        final var ansi = new AttributedString(output, AttributedStyle.DEFAULT.foreground(
                Integer.parseInt(configuration.get("command_output_color", "" + AttributedStyle.BLACK))
        )).toAnsi();
        terminal.writer().println(ansi);
    }

    private void output_c(String output, int color) {
        final var ansi = new AttributedString(output, AttributedStyle.DEFAULT.foreground(color)).toAnsi();
        terminal.writer().println(ansi);
    }

    public void error(final String output) {
        output_c(output,
                Integer.parseInt(configuration.get("error_output_color", "" + AttributedStyle.RED)));
    }

    public void shell_output(final String output) {
        output_c(output,
                Integer.parseInt(configuration.get("shell_output_color", "" + AttributedStyle.CYAN))
        );
    }

    public String in(String prompt) {
        if (loaded.isEmpty()) {
            return reader.readLine(new AttributedString(prompt, AttributedStyle.DEFAULT.foreground(
                    Integer.parseInt(configuration.get("prompt_color", "" + AttributedStyle.BLUE))
            )).toAnsi());
        } else {
            final var cmd = loaded.removeFirst();
            output("<<< " + cmd);
            return cmd;
        }
    }

    void addToHistory(String line) {
        history.add(line);
    }

}
