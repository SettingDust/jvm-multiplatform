import net.msrandom.multiplatform.java11.Java11PlatformHelper;

final class VerifyFeature {
    public static void main(String[] args) {
        if (!"11".equals(System.getProperty("java.specification.version"))) {
            throw new AssertionError("Expected Java 11 launcher");
        }
        if (new Java11PlatformHelper().getElementRemover() == null) {
            throw new AssertionError("Java 11 feature is unusable");
        }
        System.out.println("CI_JAVA11_EXECUTED helper=" + helper().getName()
            + " home=" + System.getProperty("java.home"));
    }

    static Class<?> helper() {
        return Java11PlatformHelper.class;
    }
}
