import zipfile
import re
import shutil
import subprocess
import sys
import platform
from pathlib import Path
import time

ROOT = Path(__file__).resolve().parent
VERSION_FILE = ROOT.parent / "core" / "src" / "main" / "resources" / "turi" / "version.turi"
JARS_DIR = ROOT / "target" / "JARS"
OUTPUT_DIR = ROOT / "output"
RESOURCE_DIR = ROOT / "src" / "packaging-resources"
CLI_TARGET = ROOT / "target"

# --- Extract version ---
with open(VERSION_FILE, encoding="utf-8") as f:
    text = f.read()

match = re.search(r'let `turicum.version`\s*=\s*"([^"]+)"\s*;', text)
if not match:
    print("ERROR: Could not extract VERSION from " + VERSION_FILE, file=sys.stderr)
    sys.exit(1)

ts = int(time.time())
if ts > 65535 :
    ts = 32000 + (ts % 32000)

version = match.group(1)
tversion = re.sub(r"\.\d+-SNAPSHOT$", "."+str(ts), version)

print(f"Version={version}")
print(f"Translated Version={tversion}")

# --- Prepare target directory ---
if JARS_DIR.exists():
    shutil.rmtree(JARS_DIR)
JARS_DIR.mkdir(parents=True)

# --- Unzip distribution zip ---
zip_file = CLI_TARGET / f"turicum-cli-{version}-distribution.zip"
if not zip_file.exists():
    print(f"ERROR: ZIP file not found: {zip_file}", file=sys.stderr)
    sys.exit(1)

with zipfile.ZipFile(zip_file, 'r') as zip_ref:
    zip_ref.extractall(JARS_DIR)

system = platform.system().lower()
if system == "windows":
    packaging_types = ["exe", "msi"]
elif system == "darwin":
    packaging_types = ["pkg"]
elif system == "linux":
    packaging_types = ["deb"]
else:
    print(f"Unsupported platform: {system}", file=sys.stderr)
    sys.exit(1)

print(f"Running on: {platform.system()} ({sys.platform})")

# --- Run jpackage ---
for packaging_type in packaging_types:
    print(f"Creating installer type: {packaging_type}")
    args = [
               "jpackage",
               "--input", str(JARS_DIR),
               "--vendor", "Peter Verhas",
               "--name", "turicum",
               "--app-version", tversion,
               "--main-jar", f"turicum-cli-{version}.jar",
               "--main-class", "ch.turic.cli.Main",
               "--type", packaging_type,
               "--dest", str(OUTPUT_DIR),
               "--java-options", "-Xmx2048m",
               "--resource-dir", str(RESOURCE_DIR),
           ] + (["--win-console"] if system == "windows" else [])
    print(args)
    subprocess.run(args, check=True)
