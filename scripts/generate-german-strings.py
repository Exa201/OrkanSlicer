from pathlib import Path
import re
import time
import xml.etree.ElementTree as ET

from deep_translator import GoogleTranslator


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/values/strings.xml"
TARGET = ROOT / "app/src/main/res/values-de/strings.xml"
TOKEN = re.compile(r"%(?:\d+\$)?[a-zA-Z]|\\n|&amp;|&lt;|&gt;")


def protect(text):
    tokens = []

    def replace(match):
        tokens.append(match.group(0))
        return f"__ORCA_TOKEN_{len(tokens) - 1}__"

    return TOKEN.sub(replace, text), tokens


def restore(text, tokens):
    for index, value in enumerate(tokens):
        text = text.replace(f"__ORCA_TOKEN_{index}__", value)
    return text


source = ET.parse(SOURCE).getroot()
existing = {}
if TARGET.exists():
    for item in ET.parse(TARGET).getroot().findall("string"):
        existing[item.attrib["name"]] = item.text or ""

translator = GoogleTranslator(source="en", target="de")
result = ET.Element("resources")
pending = []

for item in source.findall("string"):
    clone = ET.SubElement(result, "string", item.attrib)
    name = item.attrib["name"]
    text = item.text or ""
    if item.attrib.get("translatable") == "false" or name in existing:
        clone.text = existing.get(name, text)
    elif list(item) or not text.strip():
        clone.text = text
    else:
        protected, tokens = protect(text)
        pending.append((clone, protected, tokens))

for clone, protected, tokens in pending:
    for attempt in range(6):
        try:
            clone.text = restore(translator.translate(protected), tokens)
            break
        except Exception:
            if attempt == 5:
                raise
            time.sleep(5 * (attempt + 1))
    time.sleep(0.45)

ET.indent(result, space="    ")
TARGET.parent.mkdir(parents=True, exist_ok=True)
ET.ElementTree(result).write(TARGET, encoding="utf-8", xml_declaration=True)
