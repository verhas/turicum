package ch.turic.memory;

import ch.turic.Command;

/**
 * A HasCommands interface is implemented by objects that contain a list of commands.
 */
public interface HasCommands {
    Command[] commands();
}
