package scripts.tests;

import ru.ytkab0bp.slicebeam.utils.PrinterWebOriginPolicy;

public final class PrinterWebOriginPolicyTest {
    public static void main(String[] args) {
        equals("http://192.168.0.23/", PrinterWebOriginPolicy.dashboardUrl("192.168.0.23:7125"));
        equals("http://printer.local/", PrinterWebOriginPolicy.dashboardUrl("http://printer.local:7125"));
        equals("http://printer.local:10088/", PrinterWebOriginPolicy.dashboardUrl("http://printer.local:10088/"));
        equals("https://printer.local/dashboard", PrinterWebOriginPolicy.dashboardUrl("https://printer.local/dashboard"));
        allows("http://printer.local", "http://printer.local/dashboard");
        allows("http://192.168.1.20", "http://192.168.1.20:80/");
        allows("https://printer.local:443", "https://PRINTER.local/status");
        blocks("http://printer.local", "http://attacker.local/");
        blocks("http://printer.local", "http://printer.local:8080/");
        blocks("http://printer.local", "https://printer.local/");
        blocks("https://printer.local", "http://printer.local/");
        blocks("http://printer.local", "javascript:alert(1)");
        blocks("http://printer.local", "file:///etc/passwd");
        blocks("http://printer.local", "http://user@printer.local/");
        blocks("not a URL", "http://printer.local/");
        System.out.println("PrinterWebOriginPolicyTest passed: dashboard resolution and same-origin HTTP(S) policy.");
    }

    private static void equals(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static void allows(String origin, String target) {
        if (!PrinterWebOriginPolicy.allows(origin, target)) {
            throw new AssertionError("Expected allowed: " + origin + " -> " + target);
        }
    }

    private static void blocks(String origin, String target) {
        if (PrinterWebOriginPolicy.allows(origin, target)) {
            throw new AssertionError("Expected blocked: " + origin + " -> " + target);
        }
    }
}
