const os = require("os");
const path = require("path");
const vscode = require("vscode");

function repoRoot(context) {
  return path.resolve(context.extensionPath, "..", "..");
}

function defaultAdapterArgs(root) {
  const version = "1.4.2";
  const jline = path.join(os.homedir(), ".m2", "repository", "org", "jline", "jline", "3.30.4", "jline-3.30.4.jar");
  const classpath = [
    path.join(root, "cli", "target", `turicum-cli-${version}.jar`),
    path.join(root, "core", "target", `turicum-${version}.jar`),
    jline
  ].join(path.delimiter);

  return [
    "-cp",
    classpath,
    "ch.turic.cli.Main",
    path.join(root, "clifx", "src", "test", "resources", "debugger_dap.turi")
  ];
}

function activate(context) {
  const factory = {
    createDebugAdapterDescriptor(session) {
      const root = repoRoot(context);
      const command = session.configuration.adapterCommand || "java";
      const args = session.configuration.adapterArgs || defaultAdapterArgs(root);
      const cwd = session.workspaceFolder ? session.workspaceFolder.uri.fsPath : root;
      return new vscode.DebugAdapterExecutable(command, args, { cwd });
    }
  };

  context.subscriptions.push(vscode.debug.registerDebugAdapterDescriptorFactory("turi", factory));
}

function deactivate() {}

module.exports = { activate, deactivate };
