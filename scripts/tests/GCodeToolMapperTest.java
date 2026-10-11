package scripts.tests;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import ru.ytkab0bp.slicebeam.utils.GCodeToolMapper;

public final class GCodeToolMapperTest {
    public static void main(String[] args) throws IOException {
        int[] tools = {0, 1, 0, 1, 0};
        check("T0", GCodeToolMapper.remapToolLine("T0", tools));
        check("T1 ; tool change", GCodeToolMapper.remapToolLine("T1 ; tool change", tools));
        check("T0", GCodeToolMapper.remapToolLine("T2", tools));
        check("T1", GCodeToolMapper.remapToolLine("T3", tools));
        check("T0", GCodeToolMapper.remapToolLine("T4", tools));
        check("G1 X10", GCodeToolMapper.remapToolLine("G1 X10", tools));
        check(0, GCodeToolMapper.parseToolCommand("T0"));
        check(12, GCodeToolMapper.parseToolCommand("T12 ; tool change"));
        check(-1, GCodeToolMapper.parseToolCommand("G1 X12 T0"));
        check(-1, GCodeToolMapper.parseToolCommand("T123"));
        if (!GCodeToolMapper.isFilamentMapLine("; filament: 1,2"))
            throw new AssertionError("Filament map comment was not recognized");
        if (!GCodeToolMapper.isFilamentMapLine(" ;  filament : 1,2"))
            throw new AssertionError("Whitespace in filament map comment was not recognized");
        if (GCodeToolMapper.isFilamentMapLine("; filament_type = PLA"))
            throw new AssertionError("Filament type header was mistaken for the filament map");
        check("1,2,1,2,1", GCodeToolMapper.formatGateList(new int[]{0, 1, 0, 1, 0}));
        boolean rejected = false;
        try { GCodeToolMapper.remapToolLine("T5", tools); }
        catch (IllegalArgumentException expected) { rejected = true; }
        if (!rejected) throw new AssertionError("Unassigned tool must be rejected");
        if (args.length > 0) testFile(args[0], tools);
        System.out.println("GCodeToolMapperTest passed: five slicer tools route only to ACE tools T0/T1.");
    }

    private static void testFile(String path, int[] tools) throws IOException {
        int commands = 0;
        int highest = -1;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String mapped = GCodeToolMapper.remapToolLine(line, tools);
                if (!mapped.equals(line) || line.matches("T[0-9]{1,2}(?:\\s*;.*)?")) {
                    int tool = Integer.parseInt(mapped.substring(1).split("[ ;]", 2)[0]);
                    highest = Math.max(highest, tool);
                    commands++;
                }
            }
        }
        if (commands == 0 || highest > 1)
            throw new AssertionError("Expected the active G-code's tools to map only to T0/T1; commands="
                    + commands + ", highest=" + highest);
        System.out.println("Active printer G-code check passed: " + commands
                + " tool commands remap to T0/T1; no empty gate 3 is requested.");
    }

    private static void check(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static void check(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
