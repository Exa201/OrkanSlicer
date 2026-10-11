#!/usr/bin/env python3
"""Static regression checks for Android cut connector plumbing.

This intentionally checks the Java/JNI/native bridge points that make Feature 5 usable:
- Java Native declarations and Model wrapper methods exist.
- Native C++ uses the correct ModelRef pointer wrapper.
- Connector add/remove/clear JNI functions create unprocessed cut connector volumes.
- Cut execution enables CreateDowels so dowel connectors survive the cut.
- Renderer exposes cut-plane connector placement and preview state.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

def require(text: str, needle: str, label: str) -> None:
    if needle not in text:
        raise AssertionError(f"missing {label}: {needle}")

native_kotlin = read("app/src/main/java/ru/ytkab0bp/slicebeam/slic3r/Native.kt")
model_kotlin = read("app/src/main/java/ru/ytkab0bp/slicebeam/slic3r/Model.kt")
renderer_java = read("app/src/main/java/ru/ytkab0bp/slicebeam/render/GLRenderer.kt")
transform_cut = read("app/src/main/java/ru/ytkab0bp/slicebeam/components/bed_menu/TransformCutMenu.kt")
beam_native = read("app/src/main/jni/slicebeam/beam_native.cpp")

for needle, label in [
    ("external fun model_add_connector(ptr: Long, objIdx: Int, x: Double, y: Double, z: Double, radius: Float, height: Float, type: Int): Unit", "specified add connector JNI declaration"),
    ("external fun model_remove_connector(ptr: Long, objIdx: Int, connIdx: Int): Unit", "remove connector JNI declaration"),
    ("external fun model_clear_connectors(ptr: Long, objIdx: Int): Unit", "clear connector JNI declaration"),
    ("external fun model_add_connector_on_plane", "rotated-plane connector JNI declaration"),
]:
    require(native_kotlin, needle, label)

for needle, label in [
    ("open fun addConnector(", "Model.addConnector wrapper"),
    ("open fun removeConnector(objectIndex", "Model.removeConnector wrapper"),
    ("open fun clearConnectors(objectIndex", "Model.clearConnectors wrapper"),
]:
    require(model_kotlin, needle, label)

for needle, label in [
    ("Java_ru_ytkab0bp_slicebeam_slic3r_Native_model_1add_1connector", "native add connector implementation"),
    ("Java_ru_ytkab0bp_slicebeam_slic3r_Native_model_1remove_1connector", "native remove connector implementation"),
    ("Java_ru_ytkab0bp_slicebeam_slic3r_Native_model_1clear_1connectors", "native clear connector implementation"),
    ("ModelRef* model = (ModelRef *) (intptr_t) ptr;", "native functions use ModelRef wrapper"),
    ("ModelVolume::CutInfo(connector_type", "connector volume cut info"),
    ("ModelObjectCutAttribute::CreateDowels", "cut enables dowel creation"),
    ("!volume->cut_info.is_processed", "remove/clear only unprocessed connector volumes"),
]:
    require(beam_native, needle, label)

for needle, label in [
    ("setCutConnectorPlacement", "renderer placement toggle"),
    ("tryAddCutConnector", "renderer click-to-place implementation"),
    ("drawCutConnectors", "renderer connector preview drawing"),
    ("cutConnectorsDirty", "renderer preview VBO invalidation"),
]:
    require(renderer_java, needle, label)

for needle, label in [
    ("R.string.CutAddConnector", "CutMenu add connector toggle"),
    ("R.string.CutConnectorRadius", "CutMenu radius control"),
    ("R.string.CutConnectorHeight", "CutMenu height control"),
    ("R.string.CutClearConnectors", "CutMenu clear connector action"),
    ("setCutConnectorPlacement(connectorMode)", "CutMenu connector placement toggle"),
    ("renderer.clearAllCutConnectorPreviews()", "CutMenu connector preview cleanup"),
]:
    require(transform_cut, needle, label)

print("cut connector bridge checks passed")
