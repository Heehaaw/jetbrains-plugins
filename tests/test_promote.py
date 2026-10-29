from io import BytesIO
from pathlib import Path
import sys
from tempfile import TemporaryDirectory
import unittest
import xml.etree.ElementTree as ET
from zipfile import ZipFile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from promote import MANIFEST, PLUGIN_ID, PLUGIN_XML, promote, release_version


class PromotionTests(unittest.TestCase):
    def setUp(self):
        self.temporary = TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.source = self.root / "development.zip"
        self.jar_name = "codex-terminal-context/lib/codex-terminal-context-0.0.0-dev.jar"
        self.jar_entries = {
            PLUGIN_XML: f"<idea-plugin><id>{PLUGIN_ID}</id><version>0.0.0-dev</version><name>Codex Terminal Context</name></idea-plugin>".encode(),
            MANIFEST: b"Manifest-Version: 1.0\r\nVersion: 0.0.0-dev\r\nCreated-By: Gradle\r\n\r\n",
            "codexcontext/Terminal.class": bytes(range(256)),
            "META-INF/pluginIcon.svg": b"<svg/>",
        }
        self.write_bundle()

    def write_bundle(self, duplicate_jar=False):
        jar_bytes = BytesIO()
        with ZipFile(jar_bytes, "w") as jar:
            for name, content in self.jar_entries.items():
                jar.writestr(name, content)
        with ZipFile(self.source, "w") as bundle:
            bundle.writestr(self.jar_name, jar_bytes.getvalue())
            bundle.writestr("codex-terminal-context/lib/dependency.jar", b"retained dependency")
            if duplicate_jar:
                bundle.writestr("codex-terminal-context/lib/codex-terminal-context-other.jar", jar_bytes.getvalue())

    def test_should_promote_metadata_and_preserve_compiled_code_and_dependencies(self):
        original = self.source.read_bytes()
        result = promote(self.source, "v1.2.3", self.root / "release")

        self.assertEqual("codex-terminal-context-1.2.3.zip", result.name)
        self.assertEqual(original, self.source.read_bytes())
        with ZipFile(result) as bundle:
            self.assertEqual(b"retained dependency", bundle.read("codex-terminal-context/lib/dependency.jar"))
            with ZipFile(BytesIO(bundle.read("codex-terminal-context/lib/codex-terminal-context-1.2.3.jar"))) as jar:
                self.assertEqual(set(self.jar_entries), set(jar.namelist()))
                self.assertEqual("1.2.3", ET.fromstring(jar.read(PLUGIN_XML)).findtext("version"))
                self.assertEqual(self.jar_entries[MANIFEST].replace(b"0.0.0-dev", b"1.2.3"), jar.read(MANIFEST))
                for name in self.jar_entries.keys() - {PLUGIN_XML, MANIFEST}:
                    self.assertEqual(self.jar_entries[name], jar.read(name))

    def test_should_reject_invalid_release_tags(self):
        for tag in ("1.2.3", "v1.2", "v01.2.3", "v1.2.3-beta", "v1.2.3/path", "v1.2.3\n"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                release_version(tag)

    def test_should_reject_another_plugins_descriptor(self):
        self.jar_entries[PLUGIN_XML] = b"<idea-plugin><id>another.plugin</id><version>0.0.0-dev</version></idea-plugin>"
        self.write_bundle()
        with self.assertRaises(ValueError):
            promote(self.source, "v1.0.0", self.root / "release")
        self.assertFalse((self.root / "release").exists())

    def test_should_reject_ambiguous_plugin_jars(self):
        self.write_bundle(duplicate_jar=True)
        with self.assertRaises(ValueError):
            promote(self.source, "v1.0.0", self.root / "release")

    def test_should_reject_missing_version_metadata(self):
        del self.jar_entries[MANIFEST]
        self.write_bundle()
        with self.assertRaises(KeyError):
            promote(self.source, "v1.0.0", self.root / "release")

    def test_should_preserve_existing_release_artifacts(self):
        result = promote(self.source, "v1.0.0", self.root / "release")
        original = result.read_bytes()
        with self.assertRaises(ValueError):
            promote(self.source, "v1.0.0", self.root / "release")
        self.assertEqual(original, result.read_bytes())


if __name__ == "__main__":
    unittest.main()
