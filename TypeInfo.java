import java.util.Objects;

public class TypeInfo {
    public enum Kind {
        INT,
        BOOLEAN,
        VOID,
        CLASS,
        ERROR
    }

    private final Kind kind;
    private final String className;

    public static final TypeInfo INT = new TypeInfo(Kind.INT, null);
    public static final TypeInfo BOOLEAN = new TypeInfo(Kind.BOOLEAN, null);
    public static final TypeInfo VOID = new TypeInfo(Kind.VOID, null);
    public static final TypeInfo ERROR = new TypeInfo(Kind.ERROR, null);

    private TypeInfo(Kind kind, String className) {
        this.kind = kind;
        this.className = className;
    }

    public static TypeInfo classType(String className) {
        return new TypeInfo(Kind.CLASS, className);
    }

    public Kind getKind() {
        return kind;
    }

    public String getClassName() {
        return className;
    }

    public boolean isInt() {
        return kind == Kind.INT;
    }

    public boolean isBoolean() {
        return kind == Kind.BOOLEAN;
    }

    public boolean isVoid() {
        return kind == Kind.VOID;
    }

    public boolean isClass() {
        return kind == Kind.CLASS;
    }

    public boolean isError() {
        return kind == Kind.ERROR;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TypeInfo)) {
            return false;
        }
        TypeInfo other = (TypeInfo) obj;
        return kind == other.kind && Objects.equals(className, other.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, className);
    }

    @Override
    public String toString() {
        switch (kind) {
            case INT:
                return "int";
            case BOOLEAN:
                return "boolean";
            case VOID:
                return "void";
            case CLASS:
                return className;
            default:
                return "<erro>";
        }
    }
}
