"""Version the retained Codex plugin bundle without rebuilding compiled code."""

import argparse
from copy import copy
from io import BytesIO
from pathlib import Path
import re
import xml.etree.ElementTree as ET
from zipfile import BadZipFile, ZipFile


PLUGIN_ID = "com.janmilota.codex-terminal-context"
PLUGIN_NAME = "codex-terminal-context"
PLUGIN_XML = "META-INF/plugin.xml"
MANIFEST = "META-INF/MANIFEST.MF"


def release_version(tag: str) -> str:
    if not re.fullmatch(r"v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)", tag):
        raise ValueError("Use a numeric vMAJOR.MINOR.PATCH release tag.")
    return tag[1:]


def versioned_jar(content: bytes, version: str) -> bytes:
    output = BytesIO()
    with ZipFile(BytesIO(content)) as source, ZipFile(output, "w") as target:
        descriptor = ET.fromstring(source.read(PLUGIN_XML))
        version_element = descriptor.find("version")
        if descriptor.findtext("id") != PLUGIN_ID or version_element is None:
            raise ValueError("The retained JAR must contain the Codex Terminal Context descriptor and version.")
        version_element.text = version
        manifest, replacements = re.subn(
            rb"(?m)^Version: [^\r\n]*",
            b"Version: " + version.encode("ascii"),
            source.read(MANIFEST),
        )
        if replacements != 1:
            raise ValueError("The retained JAR must contain exactly one manifest Version field.")

        # Preserve each entry's metadata and bytes except the two release-version metadata files.
        for entry in source.infolist():
            data = source.read(entry)
            if entry.filename == PLUGIN_XML:
                data = ET.tostring(descriptor, encoding="utf-8")
            elif entry.filename == MANIFEST:
                data = manifest
            target.writestr(entry, data)
    return output.getvalue()


def promote(archive: Path, tag: str, output_dir: Path) -> Path:
    version = release_version(tag)
    destination = output_dir / f"{PLUGIN_NAME}-{version}.zip"
    if destination.exists():
        raise ValueError(f"The destination already exists: {destination}")

    with ZipFile(archive) as source:
        plugin_jars = [
            entry for entry in source.infolist()
            if entry.filename.startswith(f"{PLUGIN_NAME}/lib/{PLUGIN_NAME}-") and entry.filename.endswith(".jar")
        ]
        if len(plugin_jars) != 1:
            raise ValueError("The retained bundle must contain exactly one Codex Terminal Context JAR.")
        plugin_jar = plugin_jars[0]
        promoted_jar = versioned_jar(source.read(plugin_jar), version)
        output_dir.mkdir(parents=True, exist_ok=True)
        with ZipFile(destination, "w") as target:
            for entry in source.infolist():
                if entry.filename == plugin_jar.filename:
                    renamed = copy(entry)
                    renamed.filename = f"{PLUGIN_NAME}/lib/{PLUGIN_NAME}-{version}.jar"
                    target.writestr(renamed, promoted_jar)
                else:
                    target.writestr(entry, source.read(entry))
    return destination


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path)
    parser.add_argument("tag")
    parser.add_argument("--output-dir", type=Path, default=Path("artifacts/release"))
    args = parser.parse_args()
    try:
        print(promote(args.archive, args.tag, args.output_dir))
    except (ValueError, KeyError, BadZipFile, ET.ParseError, OSError) as error:
        parser.error(str(error))


if __name__ == "__main__":
    main()
