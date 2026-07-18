# typed: strict
# frozen_string_literal: true

# The Turicum command-line interpreter: released jars from Maven Central,
# started on Homebrew's openjdk by a small `turi` wrapper script.
class Turicum < Formula
  desc "Programming language: interpreter, compiler, and REPL"
  homepage "https://github.com/verhas/turicum"
  url "https://search.maven.org/remotecontent?filepath=ch/turic/turicum-cli/1.4.2/turicum-cli-1.4.2-distribution.zip"
  sha256 "c766df5cc426fd8511f6e36c5b5ec3d59fe0690587864b3dac50e665401a9c3c"
  license "Apache-2.0"

  livecheck do
    url "https://search.maven.org/remotecontent?filepath=ch/turic/turicum-cli/maven-metadata.xml"
    regex(%r{<release>v?(\d+(?:\.\d+)+)</release>}i)
  end

  depends_on "openjdk"

  def install
    libexec.install Dir["*.jar"]
    # the cli jar has no Main-Class manifest entry, so start it with an explicit
    # classpath over every jar of the distribution (cli, core, jline)
    (bin/"turi").write <<~SCRIPT
      #!/bin/bash
      exec "#{formula_opt_bin("openjdk")}/java" -cp "#{libexec}/*" ch.turic.cli.Main "$@"
    SCRIPT
  end

  def caveats
    <<~EOS
      The interpreter is installed as `turi`:
        turi program.turi     run a program
        turi -REPL            start the interactive REPL
        turi -help            list all options
    EOS
  end

  test do
    (testpath/"hello.turi").write <<~TURI
      println "hello, " + "brew"
    TURI
    assert_equal "hello, brew", shell_output("#{bin}/turi #{testpath}/hello.turi").strip
    assert_match version.to_s, shell_output("#{bin}/turi -version")
  end
end
